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
| stories110M f32 (fallback) | `klosax/tinyllamas-stories-gguf/resolve/main/tinyllamas-stories-110m-f32.gguf` | 440MB |
| stories110M Q8 (fallback, quantized) | run `llama-quantize` on f32 → Q8_0 | ~110MB |

```bash
# quantize the fallback once (one-time):
./build/bin/llama-quantize stories110M-f32.gguf stories110M-Q8_0.gguf Q8_0
```

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
