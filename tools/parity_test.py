#!/usr/bin/env python3
"""
Parity test suite verifying numerical consistency between Python FaceAPI reference
and on-device Android pipeline specifications.
"""
import json
import math
import sys
from pathlib import Path
import numpy as np

def run_parity_tests():
    vector_file = Path(__file__).parent / "test_vectors.json"
    if not vector_file.exists():
        print(f"Error: {vector_file} does not exist. Run generate_sample_embeddings.py first.")
        sys.exit(1)

    with open(vector_file, "r") as f:
        data = json.load(f)

    print("==================================================")
    print("Running Face Pipeline Mathematical Parity Tests")
    print("==================================================")

    # 1. Test Cosine Similarity Parity
    emb1 = np.array(data["embedding1"], dtype=np.float32)
    emb_same = np.array(data["embedding_same"], dtype=np.float32)
    emb_diff = np.array(data["embedding_diff"], dtype=np.float32)

    sim_same_expected = data["similarity_same"]
    sim_diff_expected = data["similarity_diff"]

    sim_same_calc = float(np.dot(emb1, emb_same) / (np.linalg.norm(emb1) * np.linalg.norm(emb_same)))
    sim_diff_calc = float(np.dot(emb1, emb_diff) / (np.linalg.norm(emb1) * np.linalg.norm(emb_diff)))

    assert abs(sim_same_calc - sim_same_expected) < 1e-6, f"Cosine similarity mismatch: {sim_same_calc} vs {sim_same_expected}"
    assert abs(sim_diff_calc - sim_diff_expected) < 1e-6, f"Cosine similarity mismatch: {sim_diff_calc} vs {sim_diff_expected}"
    print("[PASS] Cosine Similarity calculation parity verified (tol < 1e-6)")

    # 2. Test Umeyama Similarity Transform Matrix Parity
    sample_kps = np.array(data["sample_kps"], dtype=np.float32)
    expected_m = np.array(data["umeyama_affine_matrix"], dtype=np.float32)

    arcface_dst = np.array(data["arcface_dst"], dtype=np.float32)
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
    calc_m = np.hstack([scale * R, t.reshape(2, 1)])

    max_diff_m = np.max(np.abs(calc_m - expected_m))
    assert max_diff_m < 1e-5, f"Umeyama transform matrix diff too large: {max_diff_m}"
    print(f"[PASS] Umeyama 2D Alignment Transform parity verified (max diff: {max_diff_m:.2e})")

    # 3. Test Yaw Proxy & EAR Parity
    frames_pts = [np.array(pts, dtype=np.float64) for pts in data["frames_landmarks"]]
    for i, pts in enumerate(frames_pts):
        width = abs(pts[16, 0] - pts[0, 0]) + 1e-6
        yaw = float((pts[30, 0] - (pts[0, 0] + pts[16, 0]) / 2.0) / width)
        expected_yaw = data["yaws"][i]
        assert abs(yaw - expected_yaw) < 1e-6, f"Yaw mismatch frame {i}: {yaw} vs {expected_yaw}"

        def ear(eye):
            d1 = np.linalg.norm(eye[1] - eye[5])
            d2 = np.linalg.norm(eye[2] - eye[4])
            d0 = np.linalg.norm(eye[0] - eye[3])
            return float((d1 + d2) / (2.0 * d0 + 1e-6))

        frame_ear = (ear(pts[36:42]) + ear(pts[42:48])) / 2.0
        expected_ear = data["ears"][i]
        assert abs(frame_ear - expected_ear) < 1e-6, f"EAR mismatch frame {i}: {frame_ear} vs {expected_ear}"

    print("[PASS] Yaw Proxy and Eye Aspect Ratio (EAR) formulas verified")

    # 4. Test Least Squares Nonrigid Deformation Residual Parity
    P = np.stack(frames_pts)
    ref_i = data["ref_frame_index"]
    R = P[ref_i]
    iod = float(np.linalg.norm(R[36:42].mean(0) - R[42:48].mean(0))) + 1e-6
    assert abs(iod - data["iod"]) < 1e-6

    ones = np.ones((P.shape[1], 1))
    calc_residuals = []
    for t in range(P.shape[0]):
        if t == ref_i:
            continue
        X = np.hstack([P[t], ones])
        M, *_ = np.linalg.lstsq(X, R, rcond=None)
        res = R - X @ M
        resid = math.sqrt(float((res ** 2).sum(1).mean())) / iod
        calc_residuals.append(resid)

    for i, resid in enumerate(calc_residuals):
        exp_resid = data["affine_residuals"][i]
        assert abs(resid - exp_resid) < 1e-6, f"Affine residual {i} mismatch: {resid} vs {exp_resid}"

    nonrigid_calc = float(np.percentile(calc_residuals, 75))
    assert abs(nonrigid_calc - data["nonrigid_score"]) < 1e-6
    print("[PASS] Procrustes/Affine least-squares nonrigid deformation residual verified")

    # 5. Test Liveness Verdict Formula
    motion = data["motion_score"]
    nonrigid = data["nonrigid_score"]
    blinks = data["blinks"]

    nonrigid_part = min(nonrigid / (2.0 * 0.010), 1.0)
    motion_part = min(motion / (2.0 * 0.015), 1.0) * min(nonrigid / 0.010, 1.0)
    score_calc = float(0.5 * nonrigid_part + 0.3 * (1.0 if blinks >= 1 else 0.0) + 0.2 * motion_part)

    assert abs(score_calc - data["liveness_score"]) < 1e-5
    print(f"[PASS] Weighted Liveness Score formula verified (Calculated: {score_calc:.4f})")

    print("\n==================================================")
    print("All Parity Tests Passed Successfully!")
    print("==================================================")

if __name__ == "__main__":
    run_parity_tests()
