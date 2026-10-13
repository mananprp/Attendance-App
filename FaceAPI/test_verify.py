"""
Quick tester for the face verify API.

Usage (webcam, capture both photos live):
  python test_verify.py --api-key your-secret

Usage (use existing image files instead of the webcam):
  python test_verify.py --api-key your-secret --profile me1.jpg --live me2.jpg

Webcam controls:  SPACE = capture,  Q or ESC = quit
Needs:  pip install requests opencv-python   (opencv-python, NOT the headless build, for the preview window)
"""
import argparse
import json
import sys

import cv2
import requests


def capture(window_title: str, camera_index: int):
    """Show webcam preview, return JPEG bytes when SPACE is pressed."""
    cap = cv2.VideoCapture(camera_index, cv2.CAP_DSHOW)  # CAP_DSHOW is faster on Windows
    if not cap.isOpened():
        cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print("Could not open the camera. Try --camera 1, or use --profile/--live with image files.")
        sys.exit(1)

    jpeg = None
    while True:
        ok, frame = cap.read()
        if not ok:
            print("Failed to read from camera")
            break
        preview = frame.copy()
        cv2.putText(preview, window_title, (20, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)
        cv2.putText(preview, "SPACE = capture | Q = quit", (20, 70),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        cv2.imshow("Face test", preview)

        key = cv2.waitKey(1) & 0xFF
        if key == 32:  # SPACE
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


def read_file(path: str) -> bytes:
    with open(path, "rb") as f:
        return f.read()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--url", default="http://127.0.0.1:8000", help="API base URL")
    p.add_argument("--api-key", default="change-me")
    p.add_argument("--employee-id", default="EMP001")
    p.add_argument("--camera", type=int, default=0)
    p.add_argument("--profile", help="path to profile image (skips webcam capture)")
    p.add_argument("--live", help="path to live image (skips webcam capture)")
    p.add_argument("--no-nonce", action="store_true", help="skip challenge call (use if REQUIRE_NONCE=false)")
    args = p.parse_args()

    headers = {"X-API-Key": args.api_key}

    # 1. get both images
    if args.profile:
        profile_bytes = read_file(args.profile)
    else:
        print("Step 1: capture the PROFILE photo (the registered one)")
        profile_bytes = capture("PROFILE photo", args.camera)
        with open("captured_profile.jpg", "wb") as f:
            f.write(profile_bytes)

    if args.live:
        live_bytes = read_file(args.live)
    else:
        print("Step 2: capture the LIVE photo (the punch-in selfie)")
        live_bytes = capture("LIVE photo", args.camera)
        with open("captured_live.jpg", "wb") as f:
            f.write(live_bytes)

    # 2. request a nonce (single use, expires in ~60s so do it right before verify)
    data = {"employee_id": args.employee_id, "punch_type": "in"}
    if not args.no_nonce:
        r = requests.post(f"{args.url}/v1/challenge",
                          data={"employee_id": args.employee_id}, headers=headers, timeout=15)
        if r.status_code != 200:
            print("Challenge failed:", r.status_code, r.text)
            sys.exit(1)
        data["nonce"] = r.json()["nonce"]

    # 3. verify
    files = {
        "profile_photo": ("profile.jpg", profile_bytes, "image/jpeg"),
        "live_photo": ("live.jpg", live_bytes, "image/jpeg"),
    }
    r = requests.post(f"{args.url}/v1/verify", data=data, files=files, headers=headers, timeout=60)

    print("\nHTTP", r.status_code)
    try:
        print(json.dumps(r.json(), indent=2))
    except Exception:
        print(r.text)


if __name__ == "__main__":
    main()
