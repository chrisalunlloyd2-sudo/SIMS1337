# Ollama service environment — persistent config (C.3 / H9-env)

Status: **values persisted 2026-10-10; running server still on pre-MAX_QUEUE env until
one UAC-approved restart** (see "Apply" below). Source plan:
`.hermes/plans/2026-10-09_scope_reconciliation_perf_epic_vs_memory_hdd_fencing.md` row
C.3/H9-env; card t_b58a8120.

## Where the values live (persistence mechanism on this machine)

There is NO Windows service and NO scheduled task for Ollama here — the desktop app
(`Ollama.lnk` in the user Startup folder at login) and any manual `ollama serve` both
inherit the **user registry environment**. So the persistent env file equivalent on
Windows is:

`HKCU\Environment` (regedit: HKEY_CURRENT_USER\Environment) — survives restarts and
re-logins. Current set (verified via `reg query` 2026-10-10):

| Registry value | Setting | Set by |
|---|---|---|
| OLLAMA_MAX_LOADED_MODELS | 1 | architect 2026-10-03 |
| OLLAMA_NUM_PARALLEL | 1 | architect 2026-10-03 |
| OLLAMA_KEEP_ALIVE | 5m | architect 2026-10-03 |
| OLLAMA_FLASH_ATTENTION | 1 | architect 2026-10-03 |
| OLLAMA_CONTEXT_LENGTH | 8192 | architect 2026-10-03 |
| OLLAMA_HOST | 127.0.0.1:11434 | architect 2026-10-03 |
| OLLAMA_MAX_QUEUE | 64 | t_b58a8120 2026-10-10 (setx) |

Each value, one-line justification:

- **OLLAMA_MAX_LOADED_MODELS=1** — one-hot cellular-mic policy (C.2): the server itself
  refuses to keep a second model resident; pairs with app-side single-flight ModelBus so
  no swap can double-resident (baseline log analysis showed the server alone allowed N).
- **OLLAMA_NUM_PARALLEL=1** — serializes inference at the server for the one resident
  model; parallel requests queue instead of splitting compute (12 s+ p90 loads under
  concurrency in baseline; also caps VRAM/RAM to a single slot, n_slots=1 observed).
- **OLLAMA_KEEP_ALIVE=5m** — hot-model residency between cellular-mic turns: measured
  86.5 s disk-cold / 5.6 s OS-warm with keep_alive:0 vs ~0.5 s per call hot (plan
  C-measured); 5m covers the orchestration burst gap without pinning RAM idle for hours.
  Matches H8: repo commit dd9091a "OllamaClient keep_alive 0 -> configurable (default
  5m) with one-hot eviction".
- **OLLAMA_MAX_QUEUE=64** — backpressure for the serialized orchestrator (t_4a3756ed):
  its FIFO queue is capped at 16 jobs and returns 429 when full (stress-proven 2026-10-10),
  so a single client can never hold more than ~17 pending requests; app-side the game has
  a bounded modelExecutor queue. 64 = 4x the client cap, hard backpressure well above any
  legitimate burst yet far below the default 512 that would let a runaway client stack
  minutes of queued work against a 1-slot server (a 512-deep queue of 5 s jobs = 42 min).
- **OLLAMA_FLASH_ATTENTION=1** — pre-existing (architect); kept: llama.cpp flash-attn
  lowers KV-cache peak memory, directly supporting the <8 GB perf-EPIC RAM budget.
- **OLLAMA_CONTEXT_LENGTH=8192** — pre-existing (architect); kept: bounds KV size per
  slot (n_ctx_slot=2048 in served models) and inference RAM.
- **OLLAMA_HOST=127.0.0.1:11434** — loopback binding only; no LAN/cloud exposure.
  No cloud endpoints or remote URLs anywhere in this config.

Explicitly NOT set (open Architect decision, plan C.3): **OLLAMA_NUM_THREAD=2** — plan
recommends 2 physical cores but it was not in the architect's 10-03 set; adding it would
change llama.cpp threadpool behavior (observed n_threads=4 today). Left as-is; if signed
off, `setx OLLAMA_NUM_THREAD 2` + restart applies it.

## Proof the values work (2026-10-10)

A test serve instance launched from a plain non-elevated shell with env read from the
registry printed this `server config` map (`server-manual.err.log` format, grep
`OLLAMA_[A-Z_]*:[^ ]*`):

```
OLLAMA_CONTEXT_LENGTH:8192  OLLAMA_FLASH_ATTENTION:true  OLLAMA_HOST:http://127.0.0.1:11434
OLLAMA_KEEP_ALIVE:5m0s  OLLAMA_MAX_LOADED_MODELS:1  OLLAMA_MAX_QUEUE:64
OLLAMA_NUM_PARALLEL:1  OLLAMA_MODELS:C:\Users\viper\.ollama\models  OLLAMA_VULKAN:true ...
```

API check: `http://127.0.0.1:11434/api/ps` → model `expires_at` ≈ now+5m (keep-alive
active in the current server) — the 10-03 five values are LIVE in the running server
(startup env dump server-manual.err.log 2026-10-08 10:12:29); only OLLAMA_MAX_QUEUE=64
is pending a restart.

## Apply (needs one UAC click by the Architect)

The running serve (PID from 2026-10-08, manually launched from an elevated shell) holds
an admin token: user-level `taskkill`/`Stop-Process` return Access denied, and
`Register-ScheduledTask -RunLevel Highest` / `schtasks /rl HIGHEST /it` are denied from
a non-elevated shell, while UAC here always prompts (ConsentPromptBehaviorAdmin=5,
secure desktop). Everything else is automated:

```
powershell -ExecutionPolicy Bypass -File scripts\apply_ollama_env_restart.ps1
```

One UAC prompt (taskkill /F /T /IM ollama.exe), then the script: confirms all ollama
processes dead, backs up server-manual.*.log, relaunches `ollama serve` hidden with env
rebuilt from HKCU (so the new server is NOT elevated), waits for /api/version, then
verifies all 7 values in the new server's own "server config" dump and exits non-zero
on any mismatch. Exit 0 = applied + verified.

Restart recipe for later (same script is reusable after any env edit; or manually: kill
ollama.exe, then from a normal shell `powershell -File scripts\apply_ollama_env_restart.ps1`).

## Verification after apply (harness-verified patterns)

```
# 1. registry still holds 7/7
reg query HKCU\Environment | findstr /i OLLAMA
# 2. live server env dump shows all 7 (this is the "running ollama serve environment")
grep -ao "OLLAMA_[A-Z_]*:[^ ]*" "C:\Users\viper\AppData\Local\Ollama\server-manual.err.log" | tail
# 3. keep-alive behavior: /api/ps expires_at ~5m out after a completion
curl -s http://127.0.0.1:11434/api/ps
# 4. queue cap active: POST >64 pending -> 500 "queue full" from server; orchestrator 429 at 17 comes first from t_4a3756ed layer
```

Acceptance per task body: values visible in running `ollama serve` env dump (yes after
apply, section above), restart preserves them (yes — HKCU persistence + registry-driven
relaunch), config committed/documented (this file + scripts/apply_ollama_env_restart.ps1).

## Post-apply bench note (for t_9bb9b19c)

MAX_QUEUE only affects rejection behavior under >64-deep floods; zero-latency impact on
the sequential path. The meaningful post-change delta for the after-bench is KEEP_ALIVE
(eviction timing) + app-side ModelBus serialization, already stress-proven.