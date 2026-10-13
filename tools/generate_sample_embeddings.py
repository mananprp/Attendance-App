#!/usr/bin/env python3
"""
Generates reference mathematical test vectors and sample embeddings
for verifying numerical parity between Python FaceAPI and Kotlin Android Face Pipeline.
"""
import json
import math
import os
from pathlib import Path
import numpy as np

def generate_test_vectors():
    np.random.seed(42)
    output_dir = Path(__file__).parent
    output_file = output_dir / "test_vectors.json"

    # 1. Standard 5 landmark template
    arcface_dst = np.array([
        [38.2946, 51.6963],
        [73.5318, 51.5014],
        [56.0252, 71.7366],
        [41.5493, 92.3655],
        [70.7299, 92.2041]
    ], dtype=np.float32)

    # 2. Sample 5 landmarks (synthetic face)
    sample_kps = np.array([
        [120.5, 140.2],
        [210.8, 142.1],
        [165.0, 195.4],
        [135.2, 245.8],
        [198.6, 246.3]
    ], dtype=np.float32)

    # Umeyama similarity transform calculation in Python
    src_mean = sample_kps.mean(axis=0)
    dst_mean = arcface_dst.mean(axis=0)

    src_c = sample_kps - src_mean
    dst_c = arcface_dst - dst_mean

    src_var = float(np.mean(np.sum(src_c**2, axis=1)))
    C = (src_c.T @ dst_c) / 5.0

    U, S, Vt = np.linalg.svd(C)
    d = np.linalg.det(U) * np.linalg.det(Vt.T)
    S_diag = np.diag([1.0, 1.0 if d >= 0 else -1.0])
    R = Vt.T @ S_diag @ U.T
    scale = float(np.trace(np.diag(S) @ S_diag) / src_var)
    t = dst_mean - scale * (R @ src_mean)

    affine_matrix = np.hstack([scale * R, t.reshape(2, 1)])

    # 3. Sample 68 3D landmarks for 4 frames
    num_frames = 5
    base_68 = np.random.uniform(100.0, 300.0, (68, 2))
    # Assign specific points for eyes and nose for consistent testing
    base_68[0] = [100.0, 200.0]  # left cheek
    base_68[16] = [300.0, 200.0] # right cheek
    base_68[30] = [200.0, 220.0] # nose tip

    # Eye landmarks: 36..41 (left eye), 42..47 (right eye)
    # Left eye: 36: outer, 39: inner, 37,38: top, 40,41: bottom
    base_68[36] = [130.0, 180.0]
    base_68[37] = [140.0, 175.0]
    base_68[38] = [150.0, 175.0]
    base_68[39] = [160.0, 180.0]
    base_68[40] = [150.0, 185.0]
    base_68[41] = [140.0, 185.0]

    # Right eye: 42: inner, 45: outer, 43,44: top, 46,47: bottom
    base_68[42] = [240.0, 180.0]
    base_68[43] = [250.0, 175.0]
    base_68[44] = [260.0, 175.0]
    base_68[45] = [270.0, 180.0]
    base_68[46] = [260.0, 185.0]
    base_68[47] = [250.0, 185.0]

    frames_landmarks = []
    yaws = []
    ears = []

    def calc_ear(eye):
        d1 = np.linalg.norm(eye[1] - eye[5])
        d2 = np.linalg.norm(eye[2] - eye[4])
        d0 = np.linalg.norm(eye[0] - eye[3])
        return float((d1 + d2) / (2.0 * d0 + 1e-6))

    for f in range(num_frames):
        pts = base_68.copy()
        # Add slight 3D deformation / expression & head motion
        pts += np.random.normal(0, 1.5, pts.shape)
        # Simulate blink on frame 2
        if f == 2:
            pts[37, 1] = pts[41, 1] = 180.0
            pts[38, 1] = pts[40, 1] = 180.0
            pts[43, 1] = pts[47, 1] = 180.0
            pts[44, 1] = pts[46, 1] = 180.0

        width = abs(pts[16, 0] - pts[0, 0]) + 1e-6
        yaw = float((pts[30, 0] - (pts[0, 0] + pts[16, 0]) / 2.0) / width)
        yaws.append(yaw)

        left_ear = calc_ear(pts[36:42])
        right_ear = calc_ear(pts[42:48])
        ears.append(float((left_ear + right_ear) / 2.0))
        frames_landmarks.append(pts.tolist())

    # Calculate Python reference motion signals
    P = np.array(frames_landmarks)
    ref_i = int(np.argmin([abs(y) for y in yaws]))
    R = P[ref_i]
    iod = float(np.linalg.norm(R[36:42].mean(0) - R[42:48].mean(0))) + 1e-6

    ones = np.ones((P.shape[1], 1))
    resid = []
    for t in range(P.shape[0]):
        if t == ref_i:
            continue
        X = np.hstack([P[t], ones])
        M, *_ = np.linalg.lstsq(X, R, rcond=None)
        res = R - X @ M
        resid.append(math.sqrt(float((res ** 2).sum(1).mean())) / iod)
    nonrigid = float(np.percentile(resid, 75)) if resid else 0.0

    centers = P.mean(axis=1)
    motion = float(np.linalg.norm(centers.std(axis=0)) / iod)

    med_ear = float(np.median(ears))
    blinks = 0
    if med_ear > 0.12:
        closed = np.array(ears) < 0.75 * med_ear
        blinks = int(np.sum(closed[1:] & ~closed[:-1]) + (1 if closed[0] else 0))

    nonrigid_part = min(nonrigid / (2.0 * 0.010), 1.0)
    motion_part = min(motion / (2.0 * 0.015), 1.0) * min(nonrigid / 0.010, 1.0)
    liveness_score = float(0.5 * nonrigid_part + 0.3 * (1.0 if blinks >= 1 else 0.0) + 0.2 * motion_part)

    # 4. Sample 512-d embeddings
    emb1 = np.random.randn(512).astype(np.float32)
    emb1 = emb1 / np.linalg.norm(emb1)

    emb2_same = emb1 + np.random.normal(0, 0.1, 512).astype(np.float32)
    emb2_same = emb2_same / np.linalg.norm(emb2_same)

    emb3_diff = np.random.randn(512).astype(np.float32)
    emb3_diff = emb3_diff / np.linalg.norm(emb3_diff)

    sim_same = float(np.dot(emb1, emb2_same))
    sim_diff = float(np.dot(emb1, emb3_diff))

    test_data = {
        "arcface_dst": arcface_dst.tolist(),
        "sample_kps": sample_kps.tolist(),
        "umeyama_affine_matrix": affine_matrix.tolist(),
        "frames_landmarks": frames_landmarks,
        "yaws": yaws,
        "ears": ears,
        "ref_frame_index": ref_i,
        "iod": iod,
        "affine_residuals": resid,
        "nonrigid_score": nonrigid,
        "motion_score": motion,
        "ear_median": med_ear,
        "blinks": blinks,
        "liveness_score": liveness_score,
        "embedding1": emb1.tolist(),
        "embedding_same": emb2_same.tolist(),
        "embedding_diff": emb3_diff.tolist(),
        "similarity_same": sim_same,
        "similarity_diff": sim_diff
    }

    with open(output_file, "w") as f:
        json.dump(test_data, f, indent=2)

    print(f"Test vectors successfully written to {output_file}")
    print(f"Sample similarity same: {sim_same:.4f}, diff: {sim_diff:.4f}")
    print(f"Sample liveness score: {liveness_score:.4f}, nonrigid: {nonrigid:.5f}, blinks: {blinks}")

if __name__ == "__main__":
    generate_test_vectors()
