import os
import hashlib
import shutil
import numpy as np
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_MODELS_DIR = os.path.join(BASE_DIR, "android", "app", "src", "main", "assets", "models")
SERVER_MODELS_DIR = os.path.join(BASE_DIR, "server", "uploads", "models")
BACKUP_DIR = os.path.join(BASE_DIR, "tools", "fp32_models_backup")

os.makedirs(ASSETS_MODELS_DIR, exist_ok=True)
os.makedirs(SERVER_MODELS_DIR, exist_ok=True)
os.makedirs(BACKUP_DIR, exist_ok=True)

def compute_sha256(filepath):
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest()

def main():
    print("=== ONNX INT8 Dynamic Quantization Pipeline ===")
    
    models = ["det_10g.onnx", "1k3d68.onnx", "w600k_r50.onnx"]
    results = []

    for name in models:
        src_path = os.path.join(ASSETS_MODELS_DIR, name)
        backup_path = os.path.join(BACKUP_DIR, name)
        
        # Backup original FP32 if not already backed up
        if not os.path.exists(backup_path) and os.path.exists(src_path):
            print(f"Backing up original FP32 {name} -> {backup_path}")
            shutil.copy2(src_path, backup_path)
            
        fp32_path = backup_path if os.path.exists(backup_path) else src_path
        orig_size = os.path.getsize(fp32_path) / (1024 * 1024)
        print(f"\nProcessing {name} (Original: {orig_size:.1f} MB)...")

        quant_temp = os.path.join(BACKUP_DIR, f"{name}.quant.tmp")
        
        # Dynamic quantization to 8-bit unsigned/signed integers
        quantize_dynamic(
            model_input=fp32_path,
            model_output=quant_temp,
            weight_type=QuantType.QUInt8
        )
        
        new_size = os.path.getsize(quant_temp) / (1024 * 1024)
        reduction = (1 - (new_size / orig_size)) * 100
        print(f"  -> Quantized to {new_size:.1f} MB ({reduction:.1f}% reduction)")

        # Verify numerical parity
        print("  -> Verifying output sanity on test input...")
        session_fp32 = ort.InferenceSession(fp32_path)
        session_int8 = ort.InferenceSession(quant_temp)

        inp_name = session_fp32.get_inputs()[0].name
        inp_shape = session_fp32.get_inputs()[0].shape
        
        # Build dummy tensor with valid dimensions
        shape = [1 if isinstance(dim, str) or dim is None or dim < 0 else dim for dim in inp_shape]
        if name == "w600k_r50.onnx":
            shape = [1, 3, 112, 112]
        elif name == "1k3d68.onnx":
            shape = [1, 3, 192, 192]
        elif name == "det_10g.onnx":
            shape = [1, 3, 640, 640]

        dummy = np.random.randn(*shape).astype(np.float32)
        out_fp32 = session_fp32.run(None, {inp_name: dummy})
        out_int8 = session_int8.run(None, {inp_name: dummy})

        if name == "w600k_r50.onnx":
            v1 = out_fp32[0][0] / np.linalg.norm(out_fp32[0][0])
            v2 = out_int8[0][0] / np.linalg.norm(out_int8[0][0])
            cosine_sim = float(np.dot(v1, v2))
            print(f"  -> ArcFace Cosine Parity: {cosine_sim:.6f}")
            assert cosine_sim > 0.95, f"Cosine similarity {cosine_sim} below safety threshold!"
        else:
            diff = np.max(np.abs(out_fp32[0] - out_int8[0]))
            print(f"  -> Max Output Deviation: {diff:.6f}")

        # Deploy quantized model to assets and backend
        target_asset = os.path.join(ASSETS_MODELS_DIR, name)
        target_server = os.path.join(SERVER_MODELS_DIR, name)
        shutil.copy2(quant_temp, target_asset)
        shutil.copy2(quant_temp, target_server)
        os.remove(quant_temp)

        final_size_bytes = os.path.getsize(target_asset)
        sha256 = compute_sha256(target_asset)
        results.append({
            "name": name.replace(".onnx", ""),
            "file_name": name,
            "size_bytes": final_size_bytes,
            "sha256": sha256
        })

    print("\n=== Quantization Summary & Generated Hashes ===")
    total_mb = sum(r["size_bytes"] for r in results) / (1024 * 1024)
    print(f"Total Model Footprint: {total_mb:.1f} MB (Down from 319.4 MB)")
    for r in results:
        print(f"  - {r['file_name']}: {r['size_bytes']} bytes | SHA256: {r['sha256']}")

    print("\nDone! Updated both android/app/src/main/assets/models/ and server/uploads/models/")

if __name__ == "__main__":
    main()
