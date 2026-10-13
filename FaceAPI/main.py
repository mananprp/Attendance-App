"""
Face verification API for attendance punch in/out.

Endpoints
  POST /v1/challenge     -> single-use nonce (call right before the camera opens)
  POST /v1/verify        -> profile photo + live PHOTO  (original flow)
  POST /v1/verify-video  -> profile photo + live VIDEO (3-5 s) : liveness + face match
  GET  /health

Run:
  pip install -r requirements.txt
  python -m uvicorn main:app --host 0.0.0.0 --port 8000 --workers 1
"""
import hashlib
import json
import logging
import math
import os
import secrets
import tempfile
import threading
import time
import uuid
from collections import OrderedDict
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import cv2
import numpy as np
from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from insightface.app import FaceAnalysis
from insightface.app.common import Face

# --------------------------------------------------------------------------
# Config (override with environment variables)
# --------------------------------------------------------------------------
API_KEY = os.getenv("API_KEY", "change-me")
MODEL_NAME = os.getenv("MODEL_NAME", "buffalo_l")

# face matching (cosine similarity, 0..1)
MATCH_THRESHOLD = float(os.getenv("MATCH_THRESHOLD", "0.55"))    # >= accept, below = reject

# request handling
REQUIRE_NONCE = os.getenv("REQUIRE_NONCE", "true").lower() == "true"
NONCE_TTL_SECONDS = int(os.getenv("NONCE_TTL_SECONDS", "60"))
MAX_IMAGE_BYTES = int(os.getenv("MAX_IMAGE_BYTES", str(8 * 1024 * 1024)))
MAX_IMAGE_SIDE = int(os.getenv("MAX_IMAGE_SIDE", "1280"))
MAX_VIDEO_BYTES = int(os.getenv("MAX_VIDEO_BYTES", str(30 * 1024 * 1024)))

# minimal face requirements (no brightness / blur rejection: the match score decides)
MIN_DET_SCORE = float(os.getenv("MIN_DET_SCORE", "0.50"))
MIN_FACE_SIDE = int(os.getenv("MIN_FACE_SIDE", "100"))

# photo endpoint liveness (kept from v1; needs an anti-spoof model plugged in)
REQUIRE_LIVENESS = os.getenv("REQUIRE_LIVENESS", "false").lower() == "true"

# video liveness
LIVENESS_MODE = os.getenv("LIVENESS_MODE", "enforce")            # enforce | shadow
INCONCLUSIVE_ACTION = os.getenv("INCONCLUSIVE_ACTION", "reject") # reject | accept (weak liveness signals)
ANALYSIS_FPS = float(os.getenv("ANALYSIS_FPS", "8"))             # frames/sec analysed
MAX_ANALYSIS_FRAMES = int(os.getenv("MAX_ANALYSIS_FRAMES", "40"))
FRAME_SIDE = int(os.getenv("FRAME_SIDE", "720"))
MIN_VIDEO_SECONDS = float(os.getenv("MIN_VIDEO_SECONDS", "2.0"))
MAX_VIDEO_SECONDS = float(os.getenv("MAX_VIDEO_SECONDS", "10.0"))
MIN_GOOD_FRAMES = int(os.getenv("MIN_GOOD_FRAMES", "8"))
NONRIGID_MIN = float(os.getenv("NONRIGID_MIN", "0.010"))         # landmark residual / eye distance
MIN_MOTION = float(os.getenv("MIN_MOTION", "0.015"))             # head movement / eye distance
BLINK_DROP = float(os.getenv("BLINK_DROP", "0.75"))              # EAR below 75% of median = closed
LIVENESS_PASS = float(os.getenv("LIVENESS_PASS", "0.60"))
LIVENESS_FAIL = float(os.getenv("LIVENESS_FAIL", "0.25"))
SELF_CONSISTENCY_MIN = float(os.getenv("SELF_CONSISTENCY_MIN", "0.45"))
TOP_FRAMES_FOR_MATCH = int(os.getenv("TOP_FRAMES_FOR_MATCH", "3"))

AUDIT_DIR = Path(os.getenv("AUDIT_DIR", "./audit"))
EMBED_CACHE_SIZE = int(os.getenv("EMBED_CACHE_SIZE", "5000"))

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("face_verify")

face_engine: Optional[FaceAnalysis] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global face_engine
    AUDIT_DIR.mkdir(parents=True, exist_ok=True)
    face_engine = FaceAnalysis(name=MODEL_NAME, providers=["CPUExecutionProvider"])
    face_engine.prepare(ctx_id=-1, det_size=(640, 640))
    for needed in ("landmark_3d_68", "recognition"):
        if needed not in face_engine.models:
            log.error("Model pack %s has no %s module: video liveness will not work", MODEL_NAME, needed)
    log.info("Model %s loaded | liveness mode: %s", MODEL_NAME, LIVENESS_MODE)
    yield


app = FastAPI(title="Face Verify API", version="2.0", lifespan=lifespan)


# --------------------------------------------------------------------------
# Nonce store (single use, short TTL). Use Redis if you run multiple workers.
# --------------------------------------------------------------------------
class NonceStore:
    def __init__(self):
        self._data = {}
        self._lock = threading.Lock()

    def issue(self, employee_id: str) -> str:
        nonce = secrets.token_urlsafe(24)
        with self._lock:
            self._purge()
            self._data[nonce] = (employee_id, time.time() + NONCE_TTL_SECONDS)
        return nonce

    def consume(self, nonce: str, employee_id: str) -> bool:
        with self._lock:
            self._purge()
            item = self._data.pop(nonce, None)
        return bool(item and item[0] == employee_id and item[1] >= time.time())

    def _purge(self):
        now = time.time()
        for k in [k for k, v in self._data.items() if v[1] < now]:
            self._data.pop(k, None)


nonces = NonceStore()


class EmbeddingCache:
    def __init__(self, size: int):
        self.size = size
        self._d: "OrderedDict[str, np.ndarray]" = OrderedDict()
        self._lock = threading.Lock()

    def get(self, key):
        with self._lock:
            if key in self._d:
                self._d.move_to_end(key)
                return self._d[key]
        return None

    def put(self, key, value):
        with self._lock:
            self._d[key] = value
            self._d.move_to_end(key)
            while len(self._d) > self.size:
                self._d.popitem(last=False)


embed_cache = EmbeddingCache(EMBED_CACHE_SIZE)


# --------------------------------------------------------------------------
# Optional per-frame anti-spoof model hook (screens / prints).
# Plug e.g. Silent-Face-Anti-Spoofing (ONNX) in here.
# score() should return probability the face is REAL (0..1), or None if no model.
# --------------------------------------------------------------------------
class AntiSpoofModel:
    def score(self, img: np.ndarray, face) -> Optional[float]:
        return None


anti_spoof = AntiSpoofModel()
ANTISPOOF_MIN = float(os.getenv("ANTISPOOF_MIN", "0.50"))


# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------
class VerifyError(Exception):
    def __init__(self, code: str, message: str):
        self.code = code
        self.message = message


def area(f) -> float:
    return float((f.bbox[2] - f.bbox[0]) * (f.bbox[3] - f.bbox[1]))


def downscale(img: np.ndarray, max_side: int) -> np.ndarray:
    h, w = img.shape[:2]
    scale = max_side / max(h, w)
    if scale < 1:
        img = cv2.resize(img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
    return img


def decode_image(data: bytes, label: str) -> np.ndarray:
    if len(data) > MAX_IMAGE_BYTES:
        raise VerifyError("image_too_large", f"{label} exceeds size limit")
    img = cv2.imdecode(np.frombuffer(data, dtype=np.uint8), cv2.IMREAD_COLOR)
    if img is None:
        raise VerifyError("invalid_image", f"{label} is not a valid image")
    return downscale(img, MAX_IMAGE_SIDE)


def face_crop_stats(img: np.ndarray, face):
    """(min_side_px, sharpness, brightness) of the face crop, or None."""
    h, w = img.shape[:2]
    x1, y1, x2, y2 = [int(v) for v in face.bbox]
    x1, y1, x2, y2 = max(x1, 0), max(y1, 0), min(x2, w), min(y2, h)
    if x2 <= x1 or y2 <= y1:
        return None
    gray = cv2.cvtColor(img[y1:y2, x1:x2], cv2.COLOR_BGR2GRAY)
    return min(x2 - x1, y2 - y1), float(cv2.Laplacian(gray, cv2.CV_64F).var()), float(gray.mean())


def get_single_face(img: np.ndarray, label: str):
    """Photo path: full detection + quality gate. Raises VerifyError on problems."""
    faces = face_engine.get(img)
    if not faces:
        raise VerifyError("no_face", f"No face found in {label}")
    faces.sort(key=area, reverse=True)
    main = faces[0]
    if len(faces) > 1 and area(faces[1]) > 0.3 * area(main):
        raise VerifyError("multiple_faces", f"More than one face in {label}")
    if float(main.det_score) < MIN_DET_SCORE:
        raise VerifyError("low_confidence_face", f"Face in {label} is unclear")
    stats = face_crop_stats(img, main)
    if stats is None:
        raise VerifyError("no_face", f"No face found in {label}")
    if stats[0] < MIN_FACE_SIDE:
        raise VerifyError("face_too_small", f"Face in {label} is too small/far away")
    return main


def get_profile_embedding(profile_bytes: bytes) -> np.ndarray:
    key = hashlib.sha256(profile_bytes).hexdigest()
    emb = embed_cache.get(key)
    if emb is None:
        img = decode_image(profile_bytes, "profile photo")
        emb = get_single_face(img, "profile photo").normed_embedding
        embed_cache.put(key, emb)
    return emb


def decide_by_score(sim: float):
    """-> (verified, decision, reason, message). Only accept or reject."""
    if sim >= MATCH_THRESHOLD:
        return True, "accept", "ok", "Face verified"
    return False, "reject", "face_mismatch", "Face did not match"


def audit(record: dict, media_bytes: Optional[bytes], ext: str = "jpg"):
    """JSONL audit record + keep the live capture for disputes.
    Swap for S3 (private bucket) + a DB table in production."""
    try:
        day = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        folder = AUDIT_DIR / day
        folder.mkdir(parents=True, exist_ok=True)
        if media_bytes:
            (folder / f"{record['request_id']}.{ext}").write_bytes(media_bytes)
        with open(folder / "audit.jsonl", "a") as f:
            f.write(json.dumps(record, default=float) + "\n")
    except Exception:
        log.exception("audit write failed")


def check_key(x_api_key: Optional[str]):
    if not x_api_key or not secrets.compare_digest(x_api_key, API_KEY):
        raise HTTPException(status_code=401, detail="invalid api key")


# --------------------------------------------------------------------------
# Video: frame extraction, per-frame landmarks, liveness signals
# --------------------------------------------------------------------------
def read_video_frames(path: str):
    """Sequentially read the clip, keeping ~ANALYSIS_FPS frames per second."""
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        raise VerifyError("invalid_video", "Video could not be read")
    fps = cap.get(cv2.CAP_PROP_FPS)
    if not fps or math.isnan(fps) or fps < 1 or fps > 120:
        fps = 30.0
    stride = max(1, int(round(fps / ANALYSIS_FPS)))
    max_raw = int(MAX_VIDEO_SECONDS * fps) + 1

    frames, total = [], 0
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        total += 1
        if total > max_raw:
            cap.release()
            raise VerifyError("video_too_long", "Video is too long, record 3-5 seconds")
        if (total - 1) % stride == 0:
            frames.append(downscale(frame, FRAME_SIDE))
    cap.release()

    duration = total / fps
    if total == 0 or duration < MIN_VIDEO_SECONDS:
        raise VerifyError("video_too_short", "Video is too short, record 3-5 seconds")
    if len(frames) > MAX_ANALYSIS_FRAMES:
        idx = np.linspace(0, len(frames) - 1, MAX_ANALYSIS_FRAMES).astype(int)
        frames = [frames[i] for i in idx]
    return frames, fps, duration


def detect_with_landmarks(img: np.ndarray):
    """Light per-frame pass: detector + 68-pt landmarks only (no recognition)."""
    det = getattr(face_engine, "det_model", None) or face_engine.models["detection"]
    bboxes, kpss = det.detect(img, max_num=0, metric="default")
    faces = []
    if bboxes is None or bboxes.shape[0] == 0:
        return faces
    lm_model = face_engine.models.get("landmark_3d_68")
    for i in range(bboxes.shape[0]):
        f = Face(bbox=bboxes[i, :4], kps=kpss[i] if kpss is not None else None,
                 det_score=bboxes[i, 4])
        if lm_model is not None:
            lm_model.get(img, f)
        faces.append(f)
    return faces


def embed_face(img: np.ndarray, face) -> np.ndarray:
    emb = face_engine.models["recognition"].get(img, face)
    emb = np.asarray(emb, dtype=np.float32).flatten()
    return emb / (np.linalg.norm(emb) + 1e-9)


def yaw_proxy(pts: np.ndarray) -> float:
    """Dimensionless head-turn estimate from 68 landmarks (0 = frontal)."""
    width = abs(pts[16, 0] - pts[0, 0]) + 1e-6
    return float((pts[30, 0] - (pts[0, 0] + pts[16, 0]) / 2) / width)


def eye_aspect_ratio(eye: np.ndarray) -> float:
    return float((np.linalg.norm(eye[1] - eye[5]) + np.linalg.norm(eye[2] - eye[4]))
                 / (2 * np.linalg.norm(eye[0] - eye[3]) + 1e-6))


def collect_frame_records(frames):
    """Detect + landmarks on every analysed frame, keep the usable ones."""
    records, multi = [], 0
    for img in frames:
        faces = detect_with_landmarks(img)
        if not faces:
            continue
        faces.sort(key=area, reverse=True)
        main = faces[0]
        if len(faces) > 1 and area(faces[1]) > 0.3 * area(main):
            multi += 1
            continue
        pts3d = main.get("landmark_3d_68")
        if pts3d is None or float(main.det_score) < MIN_DET_SCORE:
            continue
        stats = face_crop_stats(img, main)
        if stats is None:
            continue
        side, sharp, _bright = stats
        if side < MIN_FACE_SIDE:
            continue
        pts = np.asarray(pts3d)[:, :2].astype(np.float64)
        records.append({"img": img, "face": main, "pts": pts, "sharp": sharp,
                        "yaw": yaw_proxy(pts)})
    if multi > 0.3 * len(frames):
        raise VerifyError("multiple_faces", "More than one face in the video")
    if len(records) < MIN_GOOD_FRAMES:
        raise VerifyError("not_enough_good_frames",
                          "Face not clearly visible in the video, face the camera")
    return records


def motion_signals(records) -> dict:
    """
    nonrigid: how much the facial landmarks deviate from a single flat (affine) transform
              of a reference frame. A flat photo/screen moving in the hand ~ 0.
              A real 3D face (parallax, expressions, blinks, mouth) > 0.
    motion  : overall head movement (translation) relative to eye distance.
    blinks  : number of eye-closing events.
    """
    P = np.stack([r["pts"] for r in records])  # T x 68 x 2
    ref_i = int(np.argmin([abs(r["yaw"]) for r in records]))
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

    ears = np.array([(eye_aspect_ratio(p[36:42]) + eye_aspect_ratio(p[42:48])) / 2 for p in P])
    med = float(np.median(ears))
    blinks = 0
    if med > 0.12:
        closed = ears < BLINK_DROP * med
        blinks = int(np.sum(closed[1:] & ~closed[:-1]) + (1 if closed[0] else 0))

    return {"nonrigid": round(nonrigid, 5), "motion": round(motion, 5),
            "blinks": blinks, "ear_median": round(med, 4)}


def liveness_verdict(sig: dict, spoof_prob: Optional[float], consistent: bool) -> dict:
    """Combine signals into pass / inconclusive / fail with a 0..1 score."""
    nonrigid_part = min(sig["nonrigid"] / (2 * NONRIGID_MIN), 1.0)
    # head movement only counts when the landmarks also moved like a 3D face,
    # otherwise a shaking photo would earn credit for "movement"
    motion_part = min(sig["motion"] / (2 * MIN_MOTION), 1.0) * min(sig["nonrigid"] / NONRIGID_MIN, 1.0)
    score = (0.5 * nonrigid_part
             + 0.3 * (1.0 if sig["blinks"] >= 1 else 0.0)
             + 0.2 * motion_part)
    reason = None
    if not consistent:
        verdict, reason = "fail", "face_changed_during_clip"
    elif spoof_prob is not None and spoof_prob < ANTISPOOF_MIN:
        verdict, reason = "fail", "anti_spoof_model"
    elif score >= LIVENESS_PASS:
        verdict = "pass"
    elif score < LIVENESS_FAIL:
        verdict, reason = "fail", "flat_or_static_face"
    else:
        verdict, reason = "inconclusive", "weak_liveness_signals"
    return {"verdict": verdict, "score": round(score, 3), "reason": reason,
            "signals": sig, "anti_spoof": spoof_prob}


# --------------------------------------------------------------------------
# Endpoints
# --------------------------------------------------------------------------
@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME, "liveness_mode": LIVENESS_MODE}


@app.post("/v1/challenge")
def challenge(employee_id: str = Form(...), x_api_key: Optional[str] = Header(None)):
    check_key(x_api_key)
    return {"nonce": nonces.issue(employee_id), "expires_in": NONCE_TTL_SECONDS}


@app.post("/v1/verify")
def verify(
    employee_id: str = Form(...),
    profile_photo: UploadFile = File(...),
    live_photo: UploadFile = File(...),
    nonce: Optional[str] = Form(None),
    punch_type: Optional[str] = Form(None),
    latitude: Optional[float] = Form(None),
    longitude: Optional[float] = Form(None),
    device_id: Optional[str] = Form(None),
    x_api_key: Optional[str] = Header(None),
):
    """Original photo-only flow (no motion-based liveness)."""
    check_key(x_api_key)
    request_id = str(uuid.uuid4())
    started = time.time()
    profile_bytes = profile_photo.file.read()
    live_bytes = live_photo.file.read()

    result = {"request_id": request_id, "employee_id": employee_id, "verified": False,
              "decision": "reject", "match_score": None,
              "threshold": round(MATCH_THRESHOLD * 100, 2), "reason": None, "message": None}
    try:
        if REQUIRE_NONCE and not (nonce and nonces.consume(nonce, employee_id)):
            raise VerifyError("invalid_nonce", "Session expired, please try again")

        live_img = decode_image(live_bytes, "live photo")
        live_face = get_single_face(live_img, "live photo")

        spoof_prob = anti_spoof.score(live_img, live_face)
        result["anti_spoof"] = spoof_prob
        if spoof_prob is not None and spoof_prob < ANTISPOOF_MIN:
            raise VerifyError("spoof_suspected", "Live check failed")
        if REQUIRE_LIVENESS and spoof_prob is None:
            raise VerifyError("liveness_unavailable", "Live check could not be completed")

        sim = float(np.dot(get_profile_embedding(profile_bytes), live_face.normed_embedding))
        result["match_score"] = round(max(sim, 0.0) * 100, 2)
        verified, decision, reason, message = decide_by_score(sim)
        result.update(verified=verified, decision=decision, reason=reason, message=message)
    except VerifyError as e:
        result.update(reason=e.code, message=e.message)
    except Exception:
        log.exception("verify failed request_id=%s", request_id)
        result.update(reason="server_error", message="Verification failed, try again")

    result["latency_ms"] = int((time.time() - started) * 1000)
    audit({**result, "punch_type": punch_type, "latitude": latitude, "longitude": longitude,
           "device_id": device_id, "ts": datetime.now(timezone.utc).isoformat()}, live_bytes)
    return result


@app.post("/v1/verify-video")
def verify_video(
    employee_id: str = Form(...),
    profile_photo: UploadFile = File(...),
    live_video: UploadFile = File(...),
    nonce: Optional[str] = Form(None),
    punch_type: Optional[str] = Form(None),
    latitude: Optional[float] = Form(None),
    longitude: Optional[float] = Form(None),
    device_id: Optional[str] = Form(None),
    x_api_key: Optional[str] = Header(None),
):
    """Profile photo + 3-5 s live clip -> liveness check + face match in one call."""
    check_key(x_api_key)
    request_id = str(uuid.uuid4())
    started = time.time()
    profile_bytes = profile_photo.file.read()
    video_bytes = live_video.file.read()
    ext = (Path(live_video.filename or "clip.mp4").suffix or ".mp4").lstrip(".").lower()

    result = {"request_id": request_id, "employee_id": employee_id, "verified": False,
              "decision": "reject", "match_score": None,
              "threshold": round(MATCH_THRESHOLD * 100, 2), "reason": None, "message": None,
              "liveness": None, "liveness_mode": LIVENESS_MODE}

    tmp_path = None
    try:
        if REQUIRE_NONCE and not (nonce and nonces.consume(nonce, employee_id)):
            raise VerifyError("invalid_nonce", "Session expired, please try again")
        if len(video_bytes) > MAX_VIDEO_BYTES:
            raise VerifyError("video_too_large", "Video file is too large")

        # 1. write clip to a temp file (OpenCV reads from a path) and pull frames
        with tempfile.NamedTemporaryFile(delete=False, suffix="." + ext) as tmp:
            tmp.write(video_bytes)
            tmp_path = tmp.name
        frames, fps, duration = read_video_frames(tmp_path)

        # 2. per-frame detection + landmarks, quality filter
        records = collect_frame_records(frames)

        # 3. identity consistency across the clip (same person start -> end)
        first, last = records[0], records[-1]
        e_first = embed_face(first["img"], first["face"])
        e_last = embed_face(last["img"], last["face"])
        self_sim = float(np.dot(e_first, e_last))
        consistent = self_sim >= SELF_CONSISTENCY_MIN

        # 4. liveness: motion signals (+ optional anti-spoof model on a few frames)
        sig = motion_signals(records)
        probs = []
        for r in records[:: max(1, len(records) // 4)]:
            p = anti_spoof.score(r["img"], r["face"])
            if p is not None:
                probs.append(p)
        spoof_prob = min(probs) if probs else None
        live = liveness_verdict(sig, spoof_prob, consistent)
        live.update(frames_analysed=len(frames), frames_used=len(records),
                    clip_seconds=round(duration, 2), self_similarity=round(self_sim, 3))
        result["liveness"] = live

        # 5. face match on the best (sharp + frontal) frames, averaged
        frontal = [r for r in records if abs(r["yaw"]) <= 0.25] or records
        best = sorted(frontal, key=lambda r: r["sharp"], reverse=True)[:TOP_FRAMES_FOR_MATCH]
        profile_emb = get_profile_embedding(profile_bytes)
        sims = [float(np.dot(profile_emb, embed_face(r["img"], r["face"]))) for r in best]
        sim = float(np.mean(sims))
        result["match_score"] = round(max(sim, 0.0) * 100, 2)
        result["frame_scores"] = [round(max(s, 0.0) * 100, 2) for s in sims]

        # 6. final decision = face match score, then liveness on top
        verified, decision, reason, message = decide_by_score(sim)
        result["match_decision"] = decision
        if LIVENESS_MODE == "enforce":
            if live["verdict"] == "fail":
                verified, decision = False, "reject"
                reason, message = "spoof_suspected", "Live check failed"
            elif (live["verdict"] == "inconclusive" and decision == "accept"
                  and INCONCLUSIVE_ACTION == "reject"):
                verified, decision = False, "reject"
                reason, message = "liveness_inconclusive", "Live check unclear, please try again"
        result.update(verified=verified, decision=decision, reason=reason, message=message)

    except VerifyError as e:
        result.update(reason=e.code, message=e.message)
    except Exception:
        log.exception("verify-video failed request_id=%s", request_id)
        result.update(reason="server_error", message="Verification failed, try again")
    finally:
        if tmp_path:
            try:
                os.remove(tmp_path)
            except OSError:
                pass

    result["latency_ms"] = int((time.time() - started) * 1000)
    audit({**result, "punch_type": punch_type, "latitude": latitude, "longitude": longitude,
           "device_id": device_id, "ts": datetime.now(timezone.utc).isoformat()},
          video_bytes, ext)
    return result