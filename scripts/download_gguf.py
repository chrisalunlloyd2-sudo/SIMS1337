#!/usr/bin/env python3
"""
PHASE 13: Q4_K_M GGUF Downloader from HuggingFace
Downloads quantized GGUF models for Ollama — Q4_K_M is the sweet spot:
~4-bit quantization, 4-5 tok/s on CPU, 4-6GB RAM for 7B models.

Usage: python download_gguf.py [model_name]
  model_name: one of the supported models below, or 'all'

Supported models:
  - llama3.2:1b-q4_K_M    (~700MB)
  - gemma2:2b-q4_K_M      (~1.2GB)
  - phi3:mini-q4_K_M      (~2.0GB)
  - qwen2.5:0.5b-q4_K_M   (~350MB)
  - tinyllama:1.1b-q4_K_M (~650MB)
  - deepseek-r1:1.5b-q4_K_M (~900MB)
  - codellama:7b-q4_K_M   (~4.0GB)
  - mistral:7b-q4_K_M     (~4.1GB)
  - phi:latest-q4_K_M     (~1.5GB)
"""

import subprocess, sys, os, json, urllib.request, hashlib, time

OLLAMA_HOST = "http://localhost:11434"

# HuggingFace GGUF repos and filenames
GGUF_MODELS = {
    "llama3.2:1b-q4_K_M": {
        "repo": "bartowski/Llama-3.2-1B-Instruct-GGUF",
        "file": "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        "size_gb": 0.7,
    },
    "gemma2:2b-q4_K_M": {
        "repo": "bartowski/gemma-2-2b-it-GGUF",
        "file": "gemma-2-2b-it-Q4_K_M.gguf",
        "size_gb": 1.2,
    },
    "phi3:mini-q4_K_M": {
        "repo": "bartowski/Phi-3-mini-4k-instruct-GGUF",
        "file": "Phi-3-mini-4k-instruct-Q4_K_M.gguf",
        "size_gb": 2.0,
    },
    "qwen2.5:0.5b-q4_K_M": {
        "repo": "bartowski/Qwen2.5-0.5B-Instruct-GGUF",
        "file": "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
        "size_gb": 0.35,
    },
    "tinyllama:1.1b-q4_K_M": {
        "repo": "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
        "file": "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        "size_gb": 0.65,
    },
    "deepseek-r1:1.5b-q4_K_M": {
        "repo": "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
        "file": "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
        "size_gb": 0.9,
    },
    "codellama:7b-q4_K_M": {
        "repo": "TheBloke/CodeLlama-7B-Instruct-GGUF",
        "file": "codellama-7b-instruct.Q4_K_M.gguf",
        "size_gb": 4.0,
    },
    "mistral:7b-q4_K_M": {
        "repo": "TheBloke/Mistral-7B-Instruct-v0.2-GGUF",
        "file": "mistral-7b-instruct-v0.2.Q4_K_M.gguf",
        "size_gb": 4.1,
    },
    "phi:latest-q4_K_M": {
        "repo": "bartowski/Phi-3.1-mini-4k-instruct-GGUF",
        "file": "Phi-3.1-mini-4k-instruct-Q4_K_M.gguf",
        "size_gb": 1.5,
    },
}

def check_ollama():
    try:
        req = urllib.request.Request(f"{OLLAMA_HOST}/api/tags")
        resp = urllib.request.urlopen(req, timeout=5)
        return resp.status == 200
    except:
        return False

def download_gguf(model_key):
    info = GGUF_MODELS[model_key]
    url = f"https://huggingface.co/{info['repo']}/resolve/main/{info['file']}"
    dest = os.path.expanduser(f"~/.ollama/models/{info['file']}")
    
    print(f"  Downloading: {info['file']} ({info['size_gb']:.1f} GB)")
    print(f"  From: {url}")
    print(f"  To: {dest}")
    
    # Use curl for reliable downloads with progress
    result = subprocess.run([
        "curl", "-L", "--progress-bar", "-o", dest, url
    ], capture_output=True, text=True, timeout=3600)
    
    if result.returncode != 0:
        print(f"  FAILED: {result.stderr[:200]}")
        return False
    
    # Verify file size
    actual_size = os.path.getsize(dest) / (1024**3)
    print(f"  Downloaded: {actual_size:.2f} GB")
    
    # Create Modelfile for Ollama
    model_name = model_key.replace("-q4_K_M", "")
    modelfile_path = os.path.expanduser(f"~/.ollama/models/Modelfile.{model_name}")
    modelfile = f"""FROM {dest}
PARAMETER temperature 0.7
PARAMETER num_ctx 8192
PARAMETER num_predict 256
"""
    with open(modelfile_path, 'w') as f:
        f.write(modelfile)
    
    # Create in Ollama
    result = subprocess.run(
        ["ollama", "create", model_name, "-f", modelfile_path],
        capture_output=True, text=True, timeout=120
    )
    print(f"  Ollama create: {result.stdout.strip() or result.stderr.strip()[:100]}")
    return result.returncode == 0

def main():
    if not check_ollama():
        print("ERROR: Ollama not running at localhost:11434")
        sys.exit(1)
    
    target = sys.argv[1] if len(sys.argv) > 1 else "all"
    
    if target == "all":
        print(f"=== Downloading ALL {len(GGUF_MODELS)} Q4_K_M models ===\n")
        total_gb = sum(m['size_gb'] for m in GGUF_MODELS.values())
        print(f"Total: ~{total_gb:.1f} GB\n")
        
        for key in GGUF_MODELS:
            print(f"[{key}]")
            if download_gguf(key):
                print(f"  OK\n")
            else:
                print(f"  SKIPPED\n")
            time.sleep(2)  # Rate limit
    elif target in GGUF_MODELS:
        print(f"[{target}]")
        download_gguf(target)
    else:
        print(f"Unknown model: {target}")
        print(f"Available: {', '.join(GGUF_MODELS.keys())}")
        sys.exit(1)
    
    print("\n=== Done ===")
    subprocess.run(["ollama", "list"], timeout=10)

if __name__ == "__main__":
    main()
