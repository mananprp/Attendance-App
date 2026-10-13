# Face Verify API

In-house face verification service for **attendance punch in / punch out**. It replaces the external face-match API.

Given the employee's registered **profile photo** and a **live capture** taken at punch time, it returns `accept` or `reject`.

Two capture modes:

| Mode | Endpoint | Live input | Liveness (anti-spoof) |
|---|---|---|---|
| **Video (recommended)** | `POST /v1/verify-video` | 3-5 second clip | Motion-based liveness + face match |
| Photo | `POST /v1/verify` | single selfie | None built in (optional model hook) |

Stack: Python 3.12, FastAPI, InsightFace (ArcFace, `buffalo_l`), ONNX Runtime (CPU), OpenCV.

> **Status:** working prototype. Liveness thresholds are starting values and **must be calibrated on real employee footage** before relying on them (see [Tuning](#tuning-and-calibration) and [Known limitations](#known-limitations)).

---

## 1. How it works

**Video flow (`/v1/verify-video`)**

1. Validate the single-use nonce.
2. Read the clip and sample ~8 frames/second.
3. Per frame: face detection + 68-point landmarks. Frames with no face, several faces, or a face under 100 px are dropped.
4. Check that the **same person** appears at the start and end of the clip.
5. **Liveness score** from three signals:
   - **Non-rigid motion**: a real 3D face (parallax, expressions) moves differently from a flat photo or screen, whose landmarks move together as one flat sheet.
   - **Blinks**: eye-aspect-ratio dips.
   - **Head movement**: only counts when the non-rigid signal also supports it.
6. **Face match**: the 3 sharpest, most frontal frames are compared with the profile photo; the scores are averaged.
7. Final decision: match score first, then liveness on top (see [Decision logic](#4-decision-logic)).

Face match = **cosine similarity** of ArcFace embeddings, reported as a percentage (`similarity x 100`, minimum 0).

**Photo flow (`/v1/verify`)**: the same match step on a single selfie, with only an optional anti-spoof model hook.

---

## 2. Setup

```
python -m venv venv
venv\Scripts\activate          # Windows   (Linux/macOS: source venv/bin/activate)
pip install -r requirements.txt
```

Run (Windows PowerShell):

```
$env:API_KEY="your-secret"
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --workers 1
```

Run (Linux/macOS):

```
API_KEY=your-secret python -m uvicorn main:app --host 0.0.0.0 --port 8000 --workers 1
```

Wait for the `Model buffalo_l loaded` log line. Check `http://<host>:8000/health`.

Notes:
- **First start downloads the InsightFace model** (~300 MB) to `~/.insightface/models/`. On a server without internet, download it beforehand and copy it there.
- **Windows:** if `onnxruntime` fails with `DLL load failed`, install the Microsoft Visual C++ Redistributable (x64), keep only `onnxruntime` (not `onnxruntime-gpu`) installed, and use `numpy<2` if needed.
- **Use `--workers 1`.** Nonces are stored in memory (see [Known limitations](#known-limitations)).
- The server uses `opencv-python-headless`. The test script `test_verify_video.py` needs `opencv-python` (with the preview window). Do not install both in the same environment; use a separate venv for testing.

### Files

| File | Purpose |
|---|---|
| `main.py` | The API service |
| `requirements.txt` | Server dependencies |
| `test_verify_video.py` | Webcam tester for the video flow (capture profile photo, record a clip, print the result) |
| `test_verify.py` | Webcam tester for the photo flow |

---

## 3. API

All endpoints except `/health` require the header `X-API-Key: <API_KEY>` (otherwise HTTP 401).
Requests are `multipart/form-data`. Verification outcomes, including rejections, return **HTTP 200**; read the `decision` field. Missing form fields return HTTP 422.

### `GET /health`
```json
{ "status": "ok", "model": "buffalo_l", "liveness_mode": "enforce" }
```

### `POST /v1/challenge`
Get a single-use nonce. Call it right before opening the camera. It expires after `NONCE_TTL_SECONDS` (default 60 s) and is bound to the `employee_id`.

| Field | Type | Required |
|---|---|---|
| `employee_id` | string | yes |

```json
{ "nonce": "Zk3...", "expires_in": 60 }
```

### `POST /v1/verify-video`  (recommended)

| Field | Type | Required | Notes |
|---|---|---|---|
| `employee_id` | string | yes | |
| `profile_photo` | file | yes | Registered photo (JPEG/PNG) |
| `live_video` | file | yes | 3-5 s clip, MP4 recommended |
| `nonce` | string | yes if `REQUIRE_NONCE=true` | From `/v1/challenge` |
| `punch_type` | string | no | `in` / `out`, stored in the audit log |
| `latitude`, `longitude` | float | no | Stored in the audit log (geofence check stays in your platform) |
| `device_id` | string | no | Stored in the audit log |

Example:
```
curl -X POST http://HOST:8000/v1/verify-video \
  -H "X-API-Key: your-secret" \
  -F employee_id=EMP001 -F nonce=NONCE_HERE -F punch_type=in \
  -F profile_photo=@profile.jpg -F live_video=@clip.mp4
```

Response:
```json
{
  "request_id": "6f1c...",
  "employee_id": "EMP001",
  "verified": true,
  "decision": "accept",
  "match_decision": "accept",
  "match_score": 71.4,
  "frame_scores": [72.1, 71.0, 70.9],
  "threshold": 55.0,
  "reason": "ok",
  "message": "Face verified",
  "liveness_mode": "enforce",
  "liveness": {
    "verdict": "pass",
    "score": 0.86,
    "reason": null,
    "signals": { "nonrigid": 0.031, "motion": 0.024, "blinks": 1, "ear_median": 0.28 },
    "anti_spoof": null,
    "frames_analysed": 32,
    "frames_used": 29,
    "clip_seconds": 4.0,
    "self_similarity": 0.93
  },
  "latency_ms": 2400
}
```

Key fields:
- `decision`: **`accept` or `reject`**. Use this (or the `verified` boolean) to mark the punch.
- `match_decision`: what the face score alone would have decided (before liveness).
- `match_score`: percent. `threshold`: percent needed to accept.
- `reason` / `message`: machine code and user-friendly text (see the table below).
- `liveness.verdict`: `pass`, `inconclusive` or `fail`; `liveness.reason` explains a non-pass.
- `liveness` is `null` if the request failed before liveness ran.

### `POST /v1/verify` (photo flow)

Same fields, with `live_photo` (file) instead of `live_video`. Response has the same top-level shape (no `liveness` block; `anti_spoof` is `null` unless a model is plugged in).

### Reason codes

| `reason` | Meaning | Suggested app message |
|---|---|---|
| `ok` | Accepted | Punch recorded |
| `face_mismatch` | Score below threshold | Face did not match |
| `spoof_suspected` | Liveness failed (see `liveness.reason`) | Live check failed |
| `liveness_inconclusive` | Weak liveness signals (rejected by default) | Please try again, face the camera and blink naturally |
| `invalid_nonce` | Nonce missing, reused, expired or for another employee | Session expired, retry |
| `no_face` | No face found | Face the camera |
| `multiple_faces` | More than one face in frame | Only one person in frame |
| `face_too_small` | Face under 100 px | Move closer |
| `low_confidence_face` | Detector unsure (photo flow) | Retake |
| `not_enough_good_frames` | Face visible in fewer than 8 analysed frames | Face the camera for the whole clip |
| `invalid_video` / `invalid_image` | File could not be decoded | Retry |
| `video_too_short` / `video_too_long` | Outside 2-10 s | Record 3-5 seconds |
| `video_too_large` / `image_too_large` | Over size limit | Lower the resolution/bitrate |
| `server_error` | Unexpected error (check server logs with `request_id`) | Try again |

`liveness.reason` values: `flat_or_static_face`, `face_changed_during_clip`, `anti_spoof_model`, `weak_liveness_signals`.

---

## 4. Decision logic

1. Face match: `similarity >= MATCH_THRESHOLD` means accept, otherwise reject.
2. If `LIVENESS_MODE=enforce` (default), liveness is applied on top:
   - `fail` means **reject** (`spoof_suspected`).
   - `inconclusive` and the face matched means **reject** (`liveness_inconclusive`), unless `INCONCLUSIVE_ACTION=accept`.
   - `pass` means the match decision stands.
3. If `LIVENESS_MODE=shadow`, liveness is computed and returned but **does not change the decision**. Use this while collecting calibration data.

There is no manager-review state: results are only `accept` or `reject`.

No brightness or blur rejection is applied. The match score decides. Note that very poor lighting adds noise to the landmarks and can weaken the liveness score.

---

## 5. Integration guide (mobile app / platform)

**Punch flow**

1. App checks the geofence (done in your platform, not here).
2. App calls `POST /v1/challenge` with the `employee_id` and gets a `nonce`.
3. App opens the **front camera** and records a **3-5 second** clip.
4. App (or your backend) sends the clip, the employee's **stored profile photo**, the `employee_id`, and the `nonce` to `/v1/verify-video`.
5. Mark the punch only if `decision == "accept"`; otherwise show the `message` and let the user retry.

**Capture guidelines (important for accuracy)**

- Duration 3-5 s (server accepts 2-10 s). Front camera, face centred and filling a good part of the frame.
- Ask the user to **look at the camera and behave naturally**: blink normally, small head movements are fine. A completely frozen face can score as inconclusive.
- Exactly **one face** in view.
- Format: MP4 (H.264) recommended. Roughly 720p at 15-30 fps is plenty; max upload 30 MB.
- Camera-only capture. Never allow gallery/pre-recorded uploads in the app.
- Request the nonce right before recording; use it within 60 seconds. Each nonce works once.
- The **profile photo must be a clear, frontal, well-lit photo** captured at enrollment. Match quality is limited by the reference image.
- The service does **not store profile photos**; send the profile photo with every request. It caches the profile embedding in memory by photo hash, so repeat punches are faster.

**Backend-to-service calls:** the API key is a single shared secret. Prefer calling this service from your backend, not directly from the app, so the key is not embedded in the app. Serve it over HTTPS (reverse proxy such as nginx or an ALB).

---

## 6. Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `API_KEY` | `change-me` | Shared secret for `X-API-Key`. **Change it.** |
| `MODEL_NAME` | `buffalo_l` | InsightFace model pack (`buffalo_s` is lighter/faster). |
| `MATCH_THRESHOLD` | `0.55` | Cosine similarity needed to accept (0.55 = 55%). |
| `REQUIRE_NONCE` | `true` | Enforce the single-use nonce. |
| `NONCE_TTL_SECONDS` | `60` | Nonce lifetime. |
| `LIVENESS_MODE` | `enforce` | `enforce` or `shadow` (log only). |
| `INCONCLUSIVE_ACTION` | `reject` | `reject` or `accept` when liveness is inconclusive and the face matched. |
| `LIVENESS_PASS` | `0.60` | Liveness score at or above this passes. |
| `LIVENESS_FAIL` | `0.25` | Liveness score below this fails. Between the two = inconclusive. |
| `NONRIGID_MIN` | `0.010` | Non-rigid motion level counted as "3D face". |
| `MIN_MOTION` | `0.015` | Head-movement level counted as movement. |
| `BLINK_DROP` | `0.75` | Eye openness below this fraction of the median counts as closed. |
| `SELF_CONSISTENCY_MIN` | `0.45` | Min similarity between first and last frame (same person in the clip). |
| `ANTISPOOF_MIN` | `0.50` | Min "real" probability from the anti-spoof model, if one is plugged in. |
| `REQUIRE_LIVENESS` | `false` | Photo flow only: reject if no anti-spoof model result is available. |
| `MIN_DET_SCORE` | `0.50` | Minimum face-detector confidence. |
| `MIN_FACE_SIDE` | `100` | Minimum face size in pixels. |
| `MIN_GOOD_FRAMES` | `8` | Minimum usable frames in the clip. |
| `ANALYSIS_FPS` | `8` | Frames per second analysed from the clip. |
| `MAX_ANALYSIS_FRAMES` | `40` | Cap on analysed frames (controls CPU time). |
| `FRAME_SIDE` | `720` | Frames are downscaled to this max side. |
| `TOP_FRAMES_FOR_MATCH` | `3` | Number of best frames averaged for the match. |
| `MIN_VIDEO_SECONDS` / `MAX_VIDEO_SECONDS` | `2.0` / `10.0` | Accepted clip duration. |
| `MAX_VIDEO_BYTES` / `MAX_IMAGE_BYTES` | 30 MB / 8 MB | Upload limits. |
| `MAX_IMAGE_SIDE` | `1280` | Photos are downscaled to this. |
| `EMBED_CACHE_SIZE` | `5000` | Cached profile embeddings. |
| `AUDIT_DIR` | `./audit` | Where audit records and captures are written. |

---

## 7. Audit log

Every request writes to `AUDIT_DIR/<YYYY-MM-DD>/`:
- `audit.jsonl`: one JSON line per request (request id, employee id, decision, score, reason, liveness signals, punch type, GPS, device id, timestamp, latency).
- `<request_id>.mp4` (or `.jpg`): the live capture, kept for dispute resolution.

---

## 8. Tuning and calibration

The defaults are untested on real phone footage. Before enforcing:

1. Run with `LIVENESS_MODE=shadow` for 2-3 weeks alongside your current vendor API.
2. Compare `accept` / `reject` against the vendor result and collect the logged `match_score` and `liveness.signals` values.
3. Set `MATCH_THRESHOLD` from real genuine vs impostor score distributions (employee A against employee B). Cosine similarity on ArcFace is **not** the same scale as the vendor's percentage, so do not copy the vendor's number.
4. Deliberately try to beat the system: a printed photo, a photo on another phone's screen, a video replay, a paper cutout. Record how many pass.
5. Set `NONRIGID_MIN`, `MIN_MOTION`, `LIVENESS_PASS` and `LIVENESS_FAIL` so attacks are rejected while genuine employees are not (target well under a few percent false rejects).
6. Switch to `LIVENESS_MODE=enforce`.

Watch `latency_ms`. CPU inference on a typical server should take a few seconds per clip; lower `MAX_ANALYSIS_FRAMES` if needed.

---

## 9. Known limitations

- **Virtual camera / injection attacks** (software feeding a pre-made clip into the app): not detected by this service. Mitigate app-side with Play Integrity / device attestation, device binding (one employee = one registered device) and camera-only capture.
- **Very still real faces or poor light** may get weak liveness scores and be rejected (`liveness_inconclusive`).
- **Nonce storage is in-memory:** run a single worker, or move `NonceStore` to Redis before scaling out.
- **Single shared API key, no rate limiting.** Put the service behind your gateway or add limits.
- No GPU support configured (CPU provider only).

---

## 10. Testing

With the server running:

```
pip install requests opencv-python
python test_verify_video.py --api-key your-secret
```

1. Press SPACE to capture the profile photo.
2. Press SPACE to record ~4 seconds. Look at the camera, blink and move naturally.
3. The result and a short summary are printed.

Options: `--profile me.jpg --video clip.mp4` to use files instead of the webcam, `--url` for a remote server, `--employee-id`, `--camera 1`, `--seconds`, `--no-nonce` (when `REQUIRE_NONCE=false`).

Suggested test cases: same person (should `accept`), a different person (should `reject`), a photo held up to the camera, a photo or video on a phone screen, dim light, two faces in frame.
