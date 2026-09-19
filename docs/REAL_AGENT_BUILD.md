# REAL Agent Build — No Simulation Doctrine
> How to run REAL tiny-model agents. If the model didn't produce the token,
> the agent didn't speak. If javac didn't compile, the code didn't compile.

## The Architecture
```
4 x tiny model (15M) ──► house GGUF server :5000 ──► real tokens
        │                                                  │
        ▼                                                  ▼
   memory/agent_<name>.jsonl (disk)          compile gate (REAL javac)
        │                                                  │
        └──────────► Markov chain (SQLite) ──► LoRA = the agent
```

## Step 1 — Native llama.cpp (NEVER the prebuilt Android-bionic binary)
```bash
# Prebuilt llama-bin/llama-cli is Android-bionic — needs /system/bin/linker,
# will NOT run in proot/Termux-cross shells. Always build native.
git clone --depth 1 https://github.com/ggml-org/llama.cpp /tmp/llama.cpp
cd /tmp/llama.cpp
cmake -B build -DLLAMA_CUBLAS=OFF -DLLAMA_METAL=OFF -DLLAMA_CURL=OFF \
      -DBUILD_SHARED_LIBS=OFF -DCMAKE_BUILD_TYPE=Release
cmake --build build -j$(nproc) --target llama-cli llama-quantize
```

## Step 2 — Models (tiny first, 135M fallback)
| Model | URL | Size |
|-------|-----|------|
| stories15M Q8 (primary) | `ggml-org/tinyllamas/resolve/main/tinyllamas/stories15M-be.Q8_0.gguf` | 26MB |
| **SmolLM-135M Q4_K_M (fallback — VERIFIED on device)** | `MaziyarPanahi/SmolLM-135M-Instruct-GGUF/resolve/main/SmolLM-135M-Instruct.Q4_K_M.gguf` | 105MB |
| danube3-500m Q4_K_M (stronger) | `h2oai/h2o-danube3-500m-chat-GGUF/resolve/main/h2o-danube3-500m-chat-Q4_K_M.gguf` | 303MB |

On-device inventory (2026-07-25) lives at `/storage/emulated/0/MatrixVault/GGUF/`:
SmolLM-135M-Instruct-v1.Q4_K_M.gguf (105MB), h2o-danube3-500m-chat-q4_k_m.gguf (303MB),
alpha-triton-grpo-1.7b.Q4_K_M.gguf (1.03GB), qwen2.5-0.5b-instruct-q4_k_m.gguf (468MB),
SmolLM-360M-Instruct-v1.Q4_K_M.gguf (258MB). The device files are ALREADY quantized —
no llama-quantize needed. Full machine-readable manifest: `house-inference/model_manifest.json`.

## Step 3 — Start the house server (strict, real)
```bash
# from MatrixWinCE (has gguf_server_v2.py):
python3 gguf_server_v2.py 5000 /tmp/stories15M-be.Q8_0.gguf --no-placeholder
curl -sf localhost:5000/healthz   # must return 200
```

## Step 4 — Run the REAL crew (4 agents)
```bash
cd SIMS1337
GGUF_BASE=http://localhost:5000 python3 scripts/agents/crew_loop.py --rounds 2
# memory lands in SIMS1337/memory/agent_<name>.jsonl — inspect it, it's real
```

## Step 5 — LoRA = the agent (after a few tocs of real transcripts)
```bash
python3 scripts/train_lora.py --hessian --data memory/agent_<name>.jsonl
# adapter trained on REAL agent behavior becomes the agent identity
```

## Troubleshooting
| Symptom | Fix |
|---------|-----|
| cmake: "No CMAKE_CXX_COMPILER" | g++ missing — apk add g++; if rename fails, free disk (df -h) then retry |
| disk 99% full, apk rename fails | rm /var/cache/apk/*; rm big temp files; retry |
| prebuilt llama-cli: "linker not found" | it's bionic — build native (Step 1) |
| model outputs garbage | switch to 135M fallback (Step 2) |
| server returns fake tokens | restart with --no-placeholder |
| connection refused :5000 | port 5000 (NOT 11434 — Ollama owns that) |
| crew aborts "GGUF server DOWN" | that's CORRECT behavior — start the server, no mocks |
