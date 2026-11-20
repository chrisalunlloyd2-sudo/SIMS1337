# Ollama service environment — persistent config (C.3 / H9-env)

Status: **LIVE AND VERIFIED 2026-11-20** — all values present in the running server's own
startup banner, loopback-only bind confirmed, restart-preservation proven by the real
2026-11-19 task-driven restart. Source plan:
`.hermes/plans/2026-10-09_scope_reconciliation_perf_epic_vs_memory_hdd_fencing.md` row
C.3/H9-env; card t_b58a8120. Machine law (Architect 2026-10-11, 2026-11-07) wins over any
repo doc that asks for more (DEV_SETUP.md / check_ollama_capacity.py hub profile MAX=4 /
KA=-1 / PAR=2 is the 16GB+ profile: do NOT apply it on this 12GB box).

## Architecture on this machine (single start path — the hard rule)

```
logon task "Aria-Ollama-Serve"  (InteractiveToken, viper, at-logon trigger)
  └─ pythonw.exe C:\Users\viper\.aria\engine\watchdog.py start-ollama
       └─ ollama_env(): reads EVERY OLLAMA_* value fresh from HKCU\Environment
          (overrides any stale parent env; OLLAMA_HOST/NO_CLOUD defaulted loopback/no-cloud)
       └─ subprocess: Programs\Ollama\ollama.exe serve   (DETACHED, logs → server-task.err.log)
       └─ idempotent: if the API already answers, it leaves the running serve alone
```

- **No program may start its own ollama serve.** The 2026-11 GodHand outage root cause:
  the GodHand JVM started its own serve with a stale environment. The watchdog/task path
  above is the single writer of the serve process (lesson from the 2026-10-10 19:29
  architect review, encoded in scripts/apply_ollama_env_restart.ps1).
- The Ollama tray app is NOT in the HKCU Run key (verified 2026-11-20: only OneDrive /
  Edge / Talon / OpenClawTray autostart) and holds no port; its
  expose-to-network setting from 2026-10-08 cannot re-open the bind while the tray is
  not autostarted. ollama-app processes found 2026-11-20: none.
- Second ollama instance `C:\MPBox\ollama\ollama.exe serve` (cmd.exe wrapper) listens on
  **127.0.0.1:11436** — a different, loopback-only instance with an empty model list.
  It is NOT part of this config and must survive restarts (the apply script is
  PID/path-scoped to the production exe and port 11434 only, never `taskkill /IM`).

## Persistence mechanism: HKCU\Environment (verified via reg query 2026-11-20)

Windows equivalent of the systemd unit override: user registry environment. The
watchdog reads it fresh at every cold start, so values survive restarts AND re-logins,
and a stale parent process env can never win.

| Registry value | Setting | Justification (one line each) |
|---|---|---|
| OLLAMA_HOST | 127.0.0.1:11434 | Loopback-only bind; local-only is a hard rule, no LAN/cloud exposure. |
| OLLAMA_MAX_LOADED_MODELS | 2 | Machine law 2026-11-07: at most 2 models resident on this 12GB/ HDD box (one running); was 1 under the 10-10 single-hot law, allowed 2 by the 10-10 19:29 law update. |
| OLLAMA_NUM_PARALLEL | 1 | Serializes inference server-side; parallel requests queue instead of splitting compute (12s+ p90 loads under concurrency in the 10-10 baseline), caps per-request RAM. |
| OLLAMA_KEEP_ALIVE | 10m | Hot model survives gaps between orchestrator/agent turns without pinning RAM idle for hours (was 5m; 10m per machine law 2026-11-07). |
| OLLAMA_FLASH_ATTENTION | 1 | llama.cpp flash-attn lowers KV-cache peak memory, supporting the <8GB RAM budget. |
| OLLAMA_CONTEXT_LENGTH | 8192 | Bounds KV size per slot and inference RAM (observed ctx_length 8192 on loaded models). |
| OLLAMA_MAX_QUEUE | 64 | Backpressure for the serialized orchestrator: 4x its 16-job FIFO cap, hard-stops runaway clients far below the default 512 (512-deep x 5s jobs = 42 min of queued work on a 1-parallel server). |
| OLLAMA_NO_CLOUD | 1 | Hard local-only rule: no cloud endpoints, ever (server confirms "Ollama cloud disabled: true"). |
| OLLAMA_LLM_LIBRARY | cpu | Root-cause fix 2026-11: GPU discovery hung ~2 min per start on this box; cpu skips it (API up in seconds, see "inference compute id=cpu" in the banner). |
| OLLAMA_VULKAN | 0 | Ditto: no Vulkan device discovery/driver probing on the CPU-only path. |
| OLLAMA_KV_CACHE_TYPE | q8_0 | Quantized KV cache halves KV RAM for the resident model (machine law 2026-11-07). |

Explicitly NOT set (open Architect option, plan C.3): OLLAMA_NUM_THREAD=2 — never in the
law set; llama.cpp currently picks n_threads=4. Adding it is a deliberate env edit via the
procedure below, not a casual setx.

## Proof (2026-11-20, live production server PID 13208, started 2026-11-19 08:25:36 by the task)

`server-task.err.log` startup banner (line `msg="server config"`, offset 283575895):

```
OLLAMA_CONTEXT_LENGTH:8192  OLLAMA_FLASH_ATTENTION:true  OLLAMA_HOST:http://127.0.0.1:11434
OLLAMA_KEEP_ALIVE:10m0s  OLLAMA_KV_CACHE_TYPE:q8_0  OLLAMA_LLM_LIBRARY:cpu
OLLAMA_MAX_LOADED_MODELS:2  OLLAMA_MAX_QUEUE:64  OLLAMA_NO_CLOUD:true
OLLAMA_NUM_PARALLEL:1  OLLAMA_VULKAN:false
```

Companion lines from the same start:

```
routes.go:2435   msg="Listening on 127.0.0.1:11434 (version 0.40.3)"
routes.go:2375   msg="Ollama cloud disabled: true"
types.go:50      msg="inference compute" id=cpu library=cpu  (no GPU devices listed)
```

- `netstat -ano` on 2026-11-20: the ONLY :11434 listener is `127.0.0.1:11434` (PID 13208);
  no 0.0.0.0 / `[::]` listener on that port. MPBox instance is separate on 127.0.0.1:11436.
- Restart preservation: the values above were written to HKCU on 2026-11-07 (law update)
  and appeared in the banner of the real 2026-11-19 restart (task → watchdog → serve) —
  persistence across an actual production restart, not a lab test.
- /api/version → 0.40.3; two models resident (qwen2.5:0.5b, deepseek-r1:1.5b) with
  client-pinned keep_alive (their expires_at is client-side, not the server default).

## Reapply after any env edit (the procedure; replaces the 2026-10-10 UAC flow — obsolete)

1. Edit the value(s) in HKCU\Environment (setx or regedit). DO NOT start serve yourself.
2. Run the restart script (starts via the task; PID-scoped stop of the production exe
   only; verifies the new server's live banner, bind, and markers; exits non-zero on any
   mismatch; refuses to report success from a stale pre-restart banner):

```
powershell -ExecutionPolicy Bypass -File scripts\apply_ollama_env_restart.ps1          # real restart
powershell -ExecutionPolicy Bypass -File scripts\apply_ollama_env_restart.ps1 -DryRun  # verify current state only
```

Exit codes: 0 = verified, 2 = serve did not die / :11434 owned by a non-production
process (it will not touch MPBox or anything unexpected), 3 = live verify failed,
4 = task or registry prerequisites missing.

Cost: every OLLAMA_ client loses in-flight generation at the stop (one agent tick).
Pick the moment; the script itself takes ~2 min including verify.

3. Update this doc's table + justification in the same commit.

DO NOT use the pre-2026-11-20 UAC flow (apply script before this rewrite) and DO NOT run
`ollama serve` by hand: both bypass the task/warm-env path (the 10-10 script
taskkilled /IM ollama.exe — would have killed the MPBox instance too — and relaunched
from its caller's env instead of the task).

## Verification checklist (any time)

```
reg query HKCU\Environment | findstr /i OLLAMA            # 11/11 values
netstat -ano | findstr :11434                             # ONLY 127.0.0.1:11434 LISTENING
curl -s http://127.0.0.1:11434/api/version                 # {"version":"0.40.3"} or newer
powershell -File scripts\apply_ollama_env_restart.ps1 -DryRun   # full live verify, exit 0
```

Tooling gotcha (for future harness authors): server-task.err.log contains chunks of
invalid UTF-8 inside llama-server dumps; decoding regions as UTF-8 silently shifts
string indexes vs byte offsets (observed ~7.7 KB drift 2026-11-20). Read with Latin-1
(cp28591, byte<->char 1:1) when mapping string positions to file offsets, as the apply
script does.

## Post-bench note (for t_9bb9b19c)

KEEP_ALIVE 10m + MAX_LOADED_MODELS 2 are the latency-relevant knobs (eviction timing,
model residency); MAX_QUEUE only changes rejection behavior at >64-deep floods (zero
impact on the sequential path). App-side ModelBus serialization is already stress-proven.
