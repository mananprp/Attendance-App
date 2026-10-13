"""
Tester for POST /v1/verify-video  (profile photo + short live video).

Webcam flow:
  python test_verify_video.py --api-key your-secret
    1. Capture PROFILE photo  (SPACE)
    2. Press SPACE to start recording, look at the camera and behave naturally
       (blink normally, small head movements are fine). Records ~4 seconds.
    3. Result is printed.

Use existing files instead of the webcam:
  python test_verify_video.py --api-key your-secret --profile me.jpg --video clip.mp4

Needs: pip install requests opencv-python   (standalone, no other file needed)
"""
import argparse
import json
import sys
import time

import cv2
import requests



def read_file(path: str) -> bytes:
    with open(path, "rb") as f:
        return f.read()


def capture(window_title: str, camera_index: int) -> bytes:
    """Show webcam preview, return JPEG bytes when SPACE is pressed."""
    cap = cv2.VideoCapture(camera_index, cv2.CAP_DSHOW)
    if not cap.isOpened():
        cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print("Could not open the camera. Try --camera 1, or use --profile with an image file.")
        sys.exit(1)
    jpeg = None
    while True:
        ok, frame = cap.read()
        if not ok:
            print("Failed to read from camera")
            break
        view = frame.copy()
        cv2.putText(view, window_title, (20, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
        cv2.putText(view, "SPACE = capture | Q = quit", (20, 70),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        cv2.imshow("Face test", view)
        key = cv2.waitKey(1) & 0xFF
        if key == 32:
            ok, buf = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 92])
            if ok:
                jpeg = buf.tobytes()
                break
        elif key in (ord("q"), 27):
            break
    cap.release()
    cv2.destroyAllWindows()
    if jpeg is None:
        print("Capture cancelled")
        sys.exit(0)
    return jpeg


def record_clip(camera_index: int, seconds: float, out_path: str) -> bytes:
    cap = cv2.VideoCapture(camera_index, cv2.CAP_DSHOW)
    if not cap.isOpened():
        cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print("Could not open the camera. Try --camera 1, or use --video with a file.")
        sys.exit(1)

    # wait for SPACE to start
    while True:
        ok, frame = cap.read()
        if not ok:
            print("Failed to read from camera")
            sys.exit(1)
        view = frame.copy()
        cv2.putText(view, "LIVE VIDEO: SPACE = start recording | Q = quit", (20, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.imshow("Face test", view)
        key = cv2.waitKey(1) & 0xFF
        if key == 32:
            break
        if key in (ord("q"), 27):
            cap.release()
            cv2.destroyAllWindows()
            sys.exit(0)

    # record for N seconds
    frames = []
    t0 = time.time()
    while time.time() - t0 < seconds:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(frame)
        view = frame.copy()
        left = max(0.0, seconds - (time.time() - t0))
        cv2.putText(view, f"RECORDING... {left:0.1f}s", (20, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 255), 2)
        cv2.imshow("Face test", view)
        cv2.waitKey(1)
    elapsed = time.time() - t0
    cap.release()
    cv2.destroyAllWindows()

    if len(frames) < 10:
        print("Recorded too few frames, check the camera")
        sys.exit(1)

    # write with the REAL measured fps so the clip duration is correct
    fps = max(1.0, len(frames) / elapsed)
    h, w = frames[0].shape[:2]
    writer = cv2.VideoWriter(out_path, cv2.VideoWriter_fourcc(*"mp4v"), fps, (w, h))
    for f in frames:
        writer.write(f)
    writer.release()
    print(f"Recorded {len(frames)} frames, {elapsed:0.1f}s, ~{fps:0.1f} fps -> {out_path}")
    return read_file(out_path)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--url", default="http://127.0.0.1:8000")
    p.add_argument("--api-key", default="change-me")
    p.add_argument("--employee-id", default="EMP001")
    p.add_argument("--camera", type=int, default=0)
    p.add_argument("--seconds", type=float, default=4.0)
    p.add_argument("--profile", help="profile image path (skips webcam photo)")
    p.add_argument("--video", help="live video path (skips webcam recording)")
    p.add_argument("--no-nonce", action="store_true")
    args = p.parse_args()

    headers = {"X-API-Key": args.api_key}

    if args.profile:
        profile_bytes = read_file(args.profile)
    else:
        print("Step 1: capture the PROFILE photo (the registered one)")
        profile_bytes = capture("PROFILE photo", args.camera)
        with open("captured_profile.jpg", "wb") as f:
            f.write(profile_bytes)

    if args.video:
        video_bytes = read_file(args.video)
        video_name = args.video
    else:
        print("Step 2: record the LIVE video. Look at the camera, blink and move naturally.")
        video_bytes = record_clip(args.camera, args.seconds, "captured_live.mp4")
        video_name = "captured_live.mp4"

    data = {"employee_id": args.employee_id, "punch_type": "in"}
    if not args.no_nonce:
        r = requests.post(f"{args.url}/v1/challenge",
                          data={"employee_id": args.employee_id}, headers=headers, timeout=15)
        if r.status_code != 200:
            print("Challenge failed:", r.status_code, r.text)
            sys.exit(1)
        data["nonce"] = r.json()["nonce"]

    files = {
        "profile_photo": ("profile.jpg", profile_bytes, "image/jpeg"),
        "live_video": (video_name.split("\\")[-1].split("/")[-1], video_bytes, "video/mp4"),
    }
    print("Uploading and verifying...")
    r = requests.post(f"{args.url}/v1/verify-video", data=data, files=files,
                      headers=headers, timeout=120)

    print("\nHTTP", r.status_code)
    try:
        res = r.json()
    except Exception:
        print(r.text)
        return
    print(json.dumps(res, indent=2))

    # short human summary
    live = res.get("liveness") or {}
    print("\n=== SUMMARY ===")
    print("Decision :", res.get("decision"), "| verified:", res.get("verified"))
    print("Match    :", res.get("match_score"), "% (auto-approve at", res.get("threshold"), "%)")
    print("Liveness :", live.get("verdict"), "| score:", live.get("score"),
          "| blinks:", (live.get("signals") or {}).get("blinks"))
    print("Reason   :", res.get("reason"), "-", res.get("message"))


if __name__ == "__main__":
    main()
