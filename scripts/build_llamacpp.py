#!/usr/bin/env python3
"""
PHASE 22: Native llama.cpp build + SmolLM-135M fallback
Builds llama.cpp from source on Windows (no prebuilt bionic binary).
Downloads SmolLM-135M Q4_K_M GGUF as fallback model.
"""
import os, sys, subprocess, urllib.request, shutil

LLAMA_DIR = "C:/Users/viper/AIGEN_SYS/repos/sims-java-neo-fx/llama.cpp"
SMOLLM_URL = "https://huggingface.co/HuggingFaceTB/SmolLM-135M/resolve/main/ggml-model-q4_k_m.gguf"
SMOLLM_PATH = "C:/Users/viper/AIGEN_SYS/repos/sims-java-neo-fx/models/smollm-135m-q4_k_m.gguf"

def run(cmd, cwd=None):
    print(f"  $ {cmd}")
    r = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"  FAIL: {r.stderr[:200]}")
    return r.returncode == 0

def build_llamacpp():
    """Build llama.cpp from source using cmake + MSVC or MinGW."""
    if not os.path.exists(LLAMA_DIR):
        print("Cloning llama.cpp...")
        run(f"git clone --depth 1 https://github.com/ggerganov/llama.cpp.git {LLAMA_DIR}")

    build_dir = f"{LLAMA_DIR}/build"
    os.makedirs(build_dir, exist_ok=True)

    print("Building llama.cpp (this may take a few minutes)...")
    # Try cmake with MinGW first, fall back to MSVC
    if run("cmake .. -DLLAMA_CURL=OFF -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DCMAKE_BUILD_TYPE=Release", cwd=build_dir):
        run("cmake --build . --config Release -j 4", cwd=build_dir)
    else:
        print("cmake failed — trying nmake fallback...")
        run("cmake .. -G \"NMake Makefiles\" -DLLAMA_CURL=OFF -DLLAMA_BUILD_TESTS=OFF -DCMAKE_BUILD_TYPE=Release", cwd=build_dir)
        run("nmake", cwd=build_dir)

    # Verify server binary exists
    server = f"{build_dir}/bin/Release/llama-server.exe"
    if not os.path.exists(server):
        server = f"{build_dir}/bin/llama-server.exe"
    if os.path.exists(server):
        print(f"✅ llama-server built: {server}")
        return server
    print("⚠️ llama-server not found — check build output")
    return None

def download_smollm():
    """Download SmolLM-135M Q4_K_M GGUF (~105MB)."""
    os.makedirs(os.path.dirname(SMOLLM_PATH), exist_ok=True)
    if os.path.exists(SMOLLM_PATH):
        print(f"✅ SmolLM already downloaded: {SMOLLM_PATH}")
        return True
    print(f"Downloading SmolLM-135M Q4_K_M (~105MB)...")
    try:
        urllib.request.urlretrieve(SMOLLM_URL, SMOLLM_PATH)
        size_mb = os.path.getsize(SMOLLM_PATH) / (1024*1024)
        print(f"✅ Downloaded: {size_mb:.0f}MB")
        return True
    except Exception as e:
        print(f"⚠️ Download failed: {e}")
        return False

if __name__ == "__main__":
    print("=== PHASE 22: Native llama.cpp + SmolLM-135M ===")
    server = build_llamacpp()
    smollm_ok = download_smollm()
    if server and smollm_ok:
        print(f"\n✅ Ready. Start server with:")
        print(f"   {server} -m {SMOLLM_PATH} --port 5000 --no-placeholder")
    else:
        print("\n⚠️ Partial success — check output above")
