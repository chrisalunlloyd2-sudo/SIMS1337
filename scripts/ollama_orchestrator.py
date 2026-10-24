#!/usr/bin/env python3
"""ollama_orchestrator.py -- UNIFY-1 local model gateway in front of Ollama.

Born as the cellular-mic single-flight queue (perf EPIC t_4a3756ed, items H7 +
C.2 + C.4). UNIFY-1 (kanban t_0c47b2b6) makes it the ONLY path to local Ollama
by adding model policy, priority queues, per-caller hard caps, /health and
/stats, keeping the loopback-only bind.

Core guarantees
  - ONE generation worker thread: at most one model loaded or inferring at any
    moment (cellular-mic doctrine); one small embed model may live beside it.
  - PRIORITY queues (UNIFY-1): game > crew > batch. Each class has its own
    bounded FIFO; the worker always drains game first, so game agents can
    never starve behind crew or batch work. Overflow = documented 429.
  - MODEL POLICY (UNIFY-1): every request is admitted against
    AIGEN_SYS/config/model_policy.json (same tables ops_health.py and
    ModelPolicy.java consume). Unknown/banned/cloud names are denied, RAM is
    size-aware (free - model ram >= floor), tier L needs the exclusive
    window (sole resident + game idle + size+3000 MB), tier M with the game
    running needs +1000 MB headroom.
  - HARD CAPS (UNIFY-1): num_ctx fixed 8192, num_predict clamped to the
    model's policy max_tokens, per-request timeout from policy timeout_s,
    optional num_thread passthrough (clamped 1..16).
  - Swap pacing: when the incoming model differs from the hot model, the
    worker waits SWAP_PACING_S before the next call.
  - Per-job timeout: worker abandons the HTTP call after the job's cap.
  - Instrumentation: every dispatch boundary logged as JSONL so concurrency,
    denies and queued depth are measurable from the log alone.

HTTP surface (127.0.0.1 only, never non-loopback)
  POST /v1/run            native single-flight job (UNIFY-1 contract)
      body: {model, prompt, caller?, num_predict?, wait?, job_id?, num_thread?}
      caller in {game,crew,batch}; default batch. wait=true (default) blocks.
  POST /v1/embed          native embeddings lane (separate single-flight)
      body: {model, input, caller?}
  POST /api/generate      ollama-shape compat; maps onto /v1/run
  POST /api/chat          ollama-shape compat; messages folded into one prompt
  POST /api/embeddings    ollama-shape compat; maps onto /v1/embed
  GET  /health            liveness: ollama_up, queued depths, game_running
  GET  /stats             counters per caller class + recent ring (20 events)
  GET  /v1/status          legacy status (kept; t_6af8f408 consumers)

Compat responses keep ollama's keys (response / message.content / embedding)
so migrated callers parse them unchanged. stream=true is NOT supported
through the gateway (v1: always non-streaming). Caller class on compat
endpoints comes from the X-MP-Caller header, else body "caller", else batch.

Status codes
  200 ok | 202 queued (wait=false) | 400 bad request | 403 POLICY identity
  deny (banned/cloud/unknown/embed misuse) | 429 queue_full | 502 upstream
  error | 504 job or wait timeout | 507 POLICY resource deny (RAM floor,
  max_loaded, tier window). Identity and resource denies both log POLICY_DENY.

Log-line contract for t_6af8f408 (alert wiring): every line is one JSON
object; events: SERVICE_START/SERVICE_STOP, JOB_QUEUED, GUARD_CHECK,
POLICY_DENY, RAM_GUARD_REJECT, QUEUE_REJECT, SWAP_PACING, MODEL_LOAD_START,
MODEL_LOAD_DETECTED, MODEL_INFER_DONE, JOB_DONE, JOB_TIMEOUT, JOB_REJECT,
EMBED_START/EMBED_DONE, EMBED_QUEUED. One JSON object per line; tolerate
unknown events for forward compatibility. MODEL_LOAD_START only ever appears
when loaded_models == 0 (single flight); MODEL_INFER_DONE is its matching end.

Run: python scripts/ollama_orchestrator.py [--port 11435] [--policy <path>]
Stdlib only. Local Ollama only (127.0.0.1:11434) per EPIC constraint; the
gateway itself is the sole service allowed to touch it directly.
"""
import argparse
import ctypes
import datetime as dt
import http.server
import json
import queue
import re
import socket
import subprocess
import threading
import time
import urllib.request
from ctypes import wintypes

OLLAMA = "http://127.0.0.1:11434"
PORT = 11435
BIND_HOST = "127.0.0.1"     # loopback only, never exposed
LOG_PATH = r"C:\Users\viper\AIGEN_SYS\logs\ollama_orchestrator.jsonl"
POLICY_PATH = r"C:\Users\viper\AIGEN_SYS\config\model_policy.json"

QUEUE_CAPS = {"game": 8, "crew": 8, "batch": 16}   # per-class bounded FIFO
PRIORITY = ("game", "crew", "batch")               # worker drains in this order
SWAP_PACING_S = 1.5         # 1-2 s gap after a swap decision (plan C.2)
KEEP_ALIVE = "5m"           # keep hot model resident during bursts (H8 default)
EMBED_KEEP_ALIVE = "1m"     # embed lane keeps its model warm briefly
JOB_TIMEOUT_S = 300.0       # upper bound; real cap = min(policy, this)
FREE_RAM_MIN_GB = 2.5       # legacy absolute floor for embeds (pre-policy)
WORKER_CHECK_S = 0.2
NUM_CTX = 8192              # hard cap (UNIFY-1)
NUM_THREAD_CLAMP = (1, 16)
MAX_TOKENS_FLOOR = 8        # never let a policy of 0 turn into unbounded
CALLERS = QUEUE_CAPS.keys()
RING_SIZE = 20

_log_lock = threading.Lock()


def now_iso():
    return dt.datetime.now().astimezone().isoformat(timespec="milliseconds")


def log_event(event, model=None, job_id=None, elapsed_ms=None, **extra):
    """One JSON object per line to LOG_PATH + console. THE instrumentation."""
    rec = {"ts": now_iso(), "event": event}
    if model is not None:
        rec["model"] = model
    if job_id is not None:
        rec["job_id"] = job_id
    if elapsed_ms is not None:
        rec["elapsed_ms"] = int(elapsed_ms)
    rec.update(extra)
    line = json.dumps(rec, ensure_ascii=False)
    with _log_lock:
        with open(LOG_PATH, "a", encoding="utf-8") as f:
            f.write(line + "\n")
    print(line, flush=True)


class MEMORYSTATUSEX(ctypes.Structure):
    _fields_ = [
        ("dwLength", wintypes.DWORD),
        ("ullTotalPhys", ctypes.c_uint64),
        ("ullAvailPhys", ctypes.c_uint64),
        ("ullTotalPageFile", ctypes.c_uint64),
        ("ullAvailPageFile", ctypes.c_uint64),
        ("ullTotalVirtual", ctypes.c_uint64),
        ("ullAvailVirtual", ctypes.c_uint64),
        ("ullAvailExtendedVirtual", ctypes.c_size_t),
    ]


def free_ram_gb():
    s = MEMORYSTATUSEX()
    s.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
    ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(s))
    return s.ullAvailPhys / 1024**3


EMBED_MARKERS = ("embed", "minilm", "bge", "mxbai")


def is_embed_model(name):
    """Cellular-mic counts GENERATION models only; the KG/memory stack keeps
    one small embed model (e.g. nomic-embed-text) resident by design."""
    n = (name or "").lower()
    return any(m in n for m in EMBED_MARKERS)


# ---------------------------------------------------------------------------
# Model policy (UNIFY-1). Tables mirror ~/.aria/engine/ops_health.py
# model_decision + com.aigen.sims.laws.ModelPolicy (same JSON file).
# ---------------------------------------------------------------------------

CLOUD_RE = re.compile(r"cloud|ollama\.com", re.I)

_EMBEDDED_POLICY = {
    "version": 1,
    "ram_floor_mb": 1500,
    "max_loaded": 2,
    "banned_name_patterns": ["cloud", "ollama.com", "gpt-oss:120b", "glm-4.6", "kimi"],
    # tier: S|M|L|embed|restricted ; ram_mb, max_tokens, timeout_s honoured here
    "models": {
        "qwen2.5:0.5b":          {"tier": "S", "ram_mb": 700,  "max_tokens": 256, "timeout_s": 60},
        "tinyllama:1.1b":        {"tier": "S", "ram_mb": 900,  "max_tokens": 256, "timeout_s": 60},
        "llama3.2:1b":           {"tier": "S", "ram_mb": 1500, "max_tokens": 256, "timeout_s": 90},
        "phi:latest":            {"tier": "S", "ram_mb": 1900, "max_tokens": 256, "timeout_s": 90},
        "gemma2:2b":             {"tier": "M", "ram_mb": 2200, "max_tokens": 384, "timeout_s": 120},
        "llama3.2:3b":           {"tier": "M", "ram_mb": 2600, "max_tokens": 384, "timeout_s": 150},
        "qwen2.5-coder:3b":      {"tier": "M", "ram_mb": 2600, "max_tokens": 512, "timeout_s": 180},
        "phi3:mini":             {"tier": "M", "ram_mb": 2900, "max_tokens": 384, "timeout_s": 150},
        "deepseek-r1:1.5b":      {"tier": "restricted", "ram_mb": 1700, "max_tokens": 192, "timeout_s": 45},
        "nomic-embed-text:latest": {"tier": "embed", "ram_mb": 400, "max_tokens": 0, "timeout_s": 30},
        "mistral:7b":            {"tier": "L", "ram_mb": 5600, "max_tokens": 512, "timeout_s": 300},
        "codellama:7b":          {"tier": "L", "ram_mb": 5000, "max_tokens": 512, "timeout_s": 300},
    },
}


def load_policy(path=None):
    """Load model_policy.json; embedded tables on any failure (never deny-all)."""
    path = path or POLICY_PATH
    try:
        with open(path, encoding="utf-8") as f:
            pol = json.load(f)
        if not isinstance(pol.get("models"), dict) or not pol["models"]:
            raise ValueError("policy has no models table")
        return pol, path, "file:" + path
    except Exception as e:  # law 7: graceful degradation, warn loudly
        log_event("POLICY_FALLBACK", reason="embedded defaults (%s: %s)" % (type(e).__name__, str(e)[:80]))
        return _EMBEDDED_POLICY, path, "embedded-defaults"


def model_decision(policy, name, free_mb, resident=(), game_running=False, kind="infer"):
    """PURE. May this model run now? -> (allow, reason, caps{max_tokens,timeout_s}).
    Ported from ops_health.py model_decision: cloud-looking names are ALWAYS
    denied; unknown names denied (default deny); RAM is size-aware
    (free - model cost must leave the floor); tier L needs the exclusive
    window; tier M with the game running needs +1000 MB headroom."""
    low = str(name).lower()
    if CLOUD_RE.search(low):
        return False, "PRIMARY DIRECTIVE: cloud model names are never allowed", {}
    if policy is None:
        return False, "no model policy loaded: default deny", {}
    for pat in policy.get("banned_name_patterns", []):
        if pat.lower() in low:
            return False, "banned pattern %r" % pat, {}
    m = (policy.get("models") or {}).get(name)
    if m is None:
        return False, "model not in policy (default deny)", {}
    caps = {"max_tokens": m.get("max_tokens", 0), "timeout_s": m.get("timeout_s", 60)}
    tier, need, floor = m.get("tier"), m.get("ram_mb", 0), policy.get("ram_floor_mb", 1500)
    if tier == "banned":
        return False, "banned: " + m.get("notes", ""), {}
    already = name in set(resident)
    if tier == "embed" and kind != "embed":
        return False, "embedding-only model", {}
    if kind == "embed" and tier != "embed":
        return False, "kind=embed requires an embed-tier model", {}
    if not already:
        if len(set(resident)) >= policy.get("max_loaded", 2):
            return False, "already %d models resident (max %d)" % (
                len(set(resident)), policy.get("max_loaded", 2)), {}
        if free_mb - need < floor:
            return False, "RAM: %d MB free - %d model < floor %d" % (free_mb, need, floor), {}
    if tier == "L":
        if set(resident) - {name}:
            return False, "tier L needs the exclusive window: other models resident: %s" % ", ".join(sorted(set(resident) - {name})), {}
        if game_running:
            return False, "tier L waits for the game to be idle", {}
        if free_mb - need < 3000 and not already:
            return False, "tier L needs size + 3000 MB free", {}
    if tier == "M" and game_running and free_mb - need < floor + 1000:
        return False, "tier M with the game running needs +1000 MB headroom", {}
    return True, "ok (%s)" % tier, caps


def identity_deny(reason):
    """Is this a policy-identity deny (banned/cloud/unknown/misuse -> 403)
    as opposed to a resource admission deny (-> 507)?"""
    r = (reason or "").lower()
    return ("primary directive" in r or "banned" in r or "not in policy" in r
            or "embedding-only" in r or "requires an embed-tier" in r
            or "no model policy" in r)


class GameSampler(threading.Thread):
    """Cached game_running detection for the tier-L window. Samples every
    GAME_SAMPLE_S via PowerShell CIM; a miss means not-running (fail open
    for batch work; ops_health remains the authoritative watchdog)."""

    GAME_SAMPLE_S = 15.0

    def __init__(self):
        super().__init__(daemon=True, name="game-sampler")
        self.game_running = False
        self.last_seen = None
        self.last_error = None
        self.start()

    def _probe(self):
        # java/jawaw command lines mentioning mindpalace == the game is up
        cmd = ["powershell", "-NoProfile", "-Command",
               "Get-CimInstance Win32_Process | Where-Object { "
               "($_.Name -like 'java*') -and ($_.CommandLine -match 'mindpalace') } "
               "| Select-Object -ExpandProperty ProcessId"]
        try:
            out = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            self.last_error = None
            if out.returncode == 0 and out.stdout.strip():
                self.game_running = True
                self.last_seen = now_iso()
            else:
                self.game_running = False
        except Exception as e:
            self.last_error = "%s: %s" % (type(e).__name__, str(e)[:80])
            self.game_running = False

    def run(self):
        while True:
            self._probe()
            time.sleep(self.GAME_SAMPLE_S)


class SingleFlight:
    """The cellular microphone with UNIFY-1 priority queues + policy:

    one generation worker thread; three per-class FIFOs drained game>crew>
    batch; every job admitted against model_policy.json before dispatch."""

    def __init__(self, policy, policy_source):
        self.policy = policy
        self.policy_source = policy_source
        self.queues = {c: queue.Queue(maxsize=QUEUE_CAPS[c]) for c in PRIORITY}
        self.active_model = None          # model the mic currently points at
        self.hot_model = None             # model hot per last /api/ps sample
        self.last_infer_end = 0.0
        self.started = time.time()
        self.recent = []                  # ring of recent events (/stats)
        self.stats = {"done": 0, "reject_queue": 0, "reject_ram": 0,
                      "timeout": 0, "errors": 0, "policy_deny_identity": 0,
                      "policy_deny_resource": 0, "queued_wait": 0}
        for c in PRIORITY:
            self.stats["done_" + c] = 0
            self.stats["reject_queue_" + c] = 0
        self.lock = threading.Lock()
        self.thread = threading.Thread(target=self._worker, name="cmq-worker",
                                       daemon=True)
        self.thread.start()

    # ---- ring buffer for /stats -------------------------------------------
    def _ring_add(self, event):
        with self.lock:
            self.recent.append(event)
            if len(self.recent) > RING_SIZE:
                del self.recent[0]

    def submit(self, caller, job):
        """Priority-FIFO enqueue. Raises queue.Full -> caller gets 429."""
        c = caller if caller in PRIORITY else "batch"
        self.queues[c].put_nowait(job)
        log_event("JOB_QUEUED", model=job["model"], job_id=job["id"],
                  caller=c, depth=self.queues[c].qsize())
        self._ring_add({"ts": now_iso(), "event": "JOB_QUEUED", "caller": c})

    def _next_job_once(self):
        """Non-blocking drain game -> crew -> batch. None when all empty."""
        for c in PRIORITY:
            try:
                return c, self.queues[c].get_nowait()
            except queue.Empty:
                pass
        return None, None

    def _next_job(self):
        """Drain game -> crew -> batch. THE no-starvation guarantee for game."""
        while True:
            caller, job = self._next_job_once()
            if job is not None:
                return caller, job
            time.sleep(WORKER_CHECK_S)

    # ---- worker side (THE single thread) ----------------------------------
    def _worker(self):
        while True:
            caller, job = self._next_job()
            self._process_job(caller, job)

    def _process_job(self, caller, job):
        model = job["model"]
        t0 = time.time()

        # ---- snapshot residency + free RAM, then policy decision -------
        names, _ = ollama_ps()
        self.hot_model = names[0] if names else None
        free_gb = free_ram_gb()
        needs_load = (self.hot_model != model)
        log_event("GUARD_CHECK", model=model, job_id=job["id"],
                  needs_load=needs_load, free_ram_gb=round(free_gb, 2),
                  loaded_models=len(names), caller=caller)
        ok, why, caps = model_decision(
            self.policy, model, free_gb * 1024.0,
            resident=names, game_running=self._game_flag(), kind="infer")
        if not ok:
            code = 403 if identity_deny(why) else 507
            if code == 403:
                self.stats["policy_deny_identity"] += 1
                log_event("POLICY_DENY", model=model, job_id=job["id"],
                          reason=why, http=403, caller=caller)
            elif why.startswith("RAM:"):
                # size-aware floor miss -> keep the legacy health signal
                self.stats["reject_ram"] += 1
                log_event("RAM_GUARD_REJECT", model=model, job_id=job["id"],
                          reason="size_aware_ram_floor", detail=why,
                          free_ram_gb=round(free_gb, 2), caller=caller)
            else:
                self.stats["policy_deny_resource"] += 1
                log_event("POLICY_DENY", model=model, job_id=job["id"],
                          reason=why, http=507, caller=caller)
            job["out"] = (code, {"error": "policy_deny" if code == 403
                                 else "resource_denied", "detail": why})
            job["done_ev"].set()
            return

        # hard caps from policy (UNIFY-1)
        num_predict = max(min(int(job["num_predict"]), int(caps["max_tokens"])
                              or MAX_TOKENS_FLOOR), 1)
        timeout_s = min(JOB_TIMEOUT_S, float(caps["timeout_s"]))
        num_thread = job["num_thread"]

        # ---- pacing on swap decision (plan C.2) -------------------------
        if self.active_model is not None and self.active_model != model:
            since = time.time() - self.last_infer_end
            wait = SWAP_PACING_S - since
            if wait > 0:
                log_event("SWAP_PACING", model=model, job_id=job["id"],
                          sleep_s=round(wait, 2), from_model=self.active_model,
                          caller=caller)
                time.sleep(wait)

        # ---- THE single flight: one generate at a time ------------------
        try:
            log_event("MODEL_LOAD_START", model=model, job_id=job["id"],
                      loaded_models=len(names), caller=caller)
            code, body = self._generate(model, job["prompt"], num_predict,
                                        timeout_s, num_thread)
            self.active_model = model
            names2, _ = ollama_ps()
            self.hot_model = names2[0] if names2 else model
            log_event("MODEL_LOAD_DETECTED", model=model, job_id=job["id"],
                      loaded_models=len(names2))
            el = int((time.time() - t0) * 1000)
            if code == 200:
                self.stats["done"] += 1
                self.stats["done_" + caller] += 1
                log_event("MODEL_INFER_DONE", model=model, job_id=job["id"],
                          elapsed_ms=el, ok=True, caller=caller)
                log_event("JOB_DONE", model=model, job_id=job["id"],
                          elapsed_ms=el, ok=True, caller=caller)
            else:
                self.stats["errors"] += 1
                log_event("MODEL_INFER_DONE", model=model, job_id=job["id"],
                          elapsed_ms=el, ok=False, http=code, caller=caller)
                log_event("JOB_DONE", model=model, job_id=job["id"],
                          elapsed_ms=el, ok=False, http=code, caller=caller)
            job["out"] = (code if code in (200, 400, 500, 503) else 502,
                          body if isinstance(body, dict) else {"error": body})
        except socket.timeout:
            self.stats["timeout"] += 1
            log_event("JOB_TIMEOUT", model=model, job_id=job["id"],
                      elapsed_ms=int((time.time() - t0) * 1000),
                      reason="job_timeout", caller=caller)
            job["out"] = (504, {"error": "job_timeout",
                                "detail": f"exceeded {timeout_s}s"})
        except Exception as e:  # worker must never die
            self.stats["errors"] += 1
            log_event("JOB_REJECT", model=model, job_id=job["id"],
                      reason=f"worker_error:{type(e).__name__}:{e}",
                      caller=caller)
            job["out"] = (502, {"error": "worker_error", "detail": str(e)})
        finally:
            self.last_infer_end = time.time()
            job["done_ev"].set()

    def _game_flag(self):
        return bool(getattr(GAME_SAMPLER, "game_running", False))

    def _generate(self, model, prompt, num_predict, timeout_s, num_thread=None):
        """One blocking /api/generate with hard timeout. Single-flight only."""
        opts = {"num_predict": int(num_predict), "num_ctx": NUM_CTX}
        if num_thread is not None:
            opts["num_thread"] = int(num_thread)
        body = json.dumps({
            "model": model,
            "prompt": prompt,
            "stream": False,
            "keep_alive": KEEP_ALIVE,
            "options": opts,
        }).encode("utf-8")
        req = urllib.request.Request(OLLAMA + "/api/generate", data=body,
                                     headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=timeout_s) as r:
            d = json.loads(r.read().decode("utf-8", "replace"))
        return 200, {"response": d.get("response", ""),
                     "total_duration": d.get("total_duration"),
                     "load_duration": d.get("load_duration"),
                     "eval_count": d.get("eval_count")}


def ollama_ps():
    """Return (names, raw) of currently loaded GENERATION models per /api/ps."""
    try:
        with urllib.request.urlopen(OLLAMA + "/api/ps", timeout=5) as r:
            d = json.loads(r.read().decode("utf-8", "replace"))
        return [m.get("name", "") for m in d.get("models", [])
                if not is_embed_model(m.get("name", ""))], d
    except Exception:
        return [], None


class EmbedLane:
    """Single-flight embeddings lane: the small embed model lives BESIDE the
    generation model (is_embed_model models are excluded from residency
    counting), but embed requests themselves serialise one at a time."""

    def __init__(self, policy):
        self.policy = policy
        self.q = queue.Queue(maxsize=QUEUE_CAPS["batch"])
        self.stats = {"done": 0, "reject_queue": 0, "deny": 0, "errors": 0,
                      "timeout": 0}
        self.thread = threading.Thread(target=self._worker, name="embed-worker",
                                       daemon=True)
        self.thread.start()

    def submit(self, job):
        self.q.put_nowait(job)
        log_event("EMBED_QUEUED", model=job["model"], job_id=job["id"],
                  depth=self.q.qsize())

    def _worker(self):
        while True:
            try:
                job = self.q.get(timeout=WORKER_CHECK_S)
            except queue.Empty:
                continue
            model = job["model"]
            t0 = time.time()
            names, _ = ollama_ps()
            free_gb = free_ram_gb()
            ok, why, caps = model_decision(
                self.policy, model, free_gb * 1024.0, resident=names,
                game_running=False, kind="embed")
            if not ok:
                code = 403 if identity_deny(why) else 507
                self.stats["deny" if code == 403 else "reject_queue"] += 1
                log_event("POLICY_DENY", model=model, job_id=job["id"],
                          reason=why, http=code, lane="embed")
                job["out"] = (code, {"error": "policy_deny" if code == 403
                                     else "resource_denied", "detail": why})
                job["done_ev"].set()
                continue
            try:
                log_event("EMBED_START", model=model, job_id=job["id"])
                body = json.dumps({"model": model, "input": job["input"],
                                   "keep_alive": EMBED_KEEP_ALIVE}).encode("utf-8")
                req = urllib.request.Request(
                    OLLAMA + "/api/embeddings", data=body,
                    headers={"Content-Type": "application/json"})
                with urllib.request.urlopen(req, timeout=float(caps["timeout_s"])) as r:
                    d = json.loads(r.read().decode("utf-8", "replace"))
                el = int((time.time() - t0) * 1000)
                self.stats["done"] += 1
                log_event("EMBED_DONE", model=model, job_id=job["id"],
                          elapsed_ms=el, ok=True)
                job["out"] = (200, {"embedding": d.get("embedding", [])})
            except socket.timeout:
                self.stats["timeout"] += 1
                log_event("JOB_TIMEOUT", model=model, job_id=job["id"],
                          reason="embed_timeout")
                job["out"] = (504, {"error": "embed_timeout"})
            except Exception as e:
                self.stats["errors"] += 1
                log_event("JOB_REJECT", model=model, job_id=job["id"],
                          reason=f"embed_error:{type(e).__name__}:{e}")
                job["out"] = (502, {"error": "embed_error", "detail": str(e)})
            finally:
                job["done_ev"].set()


GAME_SAMPLER = None  # set in main(); guarded by getattr for import-as-library


def handler_for(sf, el):
    class Handler(http.server.BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, fmt, *args):  # silence default stderr chatter
            pass

        def _json(self, code, obj):
            payload = json.dumps(obj).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        # ---- POST routing ---------------------------------------------------
        def do_POST(self):
            path = self.path.split("?", 1)[0]
            try:
                length = int(self.headers.get("Content-Length", 0))
                req = json.loads(self.rfile.read(length).decode("utf-8", "replace")
                                 or "{}")
            except Exception:
                return self._json(400, {"error": "bad_json"})
            if not isinstance(req, dict):
                return self._json(400, {"error": "bad_json"})
            caller = self.headers.get("X-MP-Caller", "").strip().lower() \
                or str(req.get("caller", "batch")).strip().lower()
            if caller not in CALLERS:
                caller = "batch"

            if path in ("/v1/run", "/api/generate", "/api/chat"):
                return self._run_job(path, req, caller)
            if path in ("/v1/embed", "/api/embeddings"):
                return self._embed(path, req, caller)
            return self._json(404, {"error": "not_found"})

        # ---- /v1/run + ollama compat ----------------------------------------
        def _run_job(self, path, req, caller):
            model = req.get("model")
            if not model:
                return self._json(400, {"error": "missing_model"})
            prompt = req.get("prompt")
            if path == "/api/chat":
                msgs = req.get("messages") or []
                if not isinstance(msgs, list) or not msgs:
                    return self._json(400, {"error": "missing_messages"})
                parts = []
                for m in msgs:
                    if isinstance(m, dict) and m.get("content"):
                        parts.append(str(m["content"]))
                prompt = "\n\n".join(parts)
            if not isinstance(prompt, str) or prompt == "":
                return self._json(400, {"error": "missing_prompt"})
            opts = req.get("options") or {}
            num_predict = req.get("num_predict", opts.get("num_predict", 64))
            try:
                num_predict = int(num_predict)
            except (TypeError, ValueError):
                num_predict = 64
            num_thread = req.get("num_thread", opts.get("num_thread"))
            if num_thread is not None:
                try:
                    nt = int(num_thread)
                    num_thread = min(max(nt, NUM_THREAD_CLAMP[0]), NUM_THREAD_CLAMP[1])
                except (TypeError, ValueError):
                    num_thread = None
            wait = bool(req.get("wait", True)) if path == "/v1/run" else True
            job = {"id": str(req.get("job_id") or time.time_ns()),
                   "model": model,
                   "prompt": prompt,
                   "num_predict": num_predict,
                   "num_thread": num_thread,
                   "caller": caller,
                   "done_ev": threading.Event(),
                   "out": None}

            # identity policy gate BEFORE enqueue: never queue a banned name.
            # Identity checks don't depend on residency -> skip /api/ps probe.
            ok, why, _caps = model_decision(
                sf.policy, model, 0,
                resident=(), game_running=sf._game_flag(), kind="infer")
            if not ok and identity_deny(why):
                sf.stats["policy_deny_identity"] += 1
                log_event("POLICY_DENY", model=model, reason=why, http=403,
                          caller=caller, stage="prequeue")
                return self._json(403, {"error": "policy_deny", "detail": why})

            try:
                sf.submit(caller, job)
            except queue.Full:
                c = caller if caller in PRIORITY else "batch"
                sf.stats["reject_queue"] += 1
                sf.stats["reject_queue_" + c] += 1
                log_event("QUEUE_REJECT", model=model, reason="queue_full",
                          caller=caller, depth=QUEUE_CAPS[c])
                return self._json(429, {"error": "queue_full",
                                        "detail": (f"{c} queue capacity "
                                                   f"{QUEUE_CAPS[c]} exceeded; "
                                                   "back off and resubmit")})

            if not wait:
                return self._json(202, {"job_id": job["id"], "status": "queued",
                                        "poll": "/v1/status"})

            # wait for THIS job's completion (own per-job wall cap)
            wait_s = JOB_TIMEOUT_S + 30
            if not job["done_ev"].wait(timeout=wait_s):
                sf.stats["timeout"] += 1
                log_event("JOB_TIMEOUT", model=model, job_id=job["id"],
                          elapsed_ms=None, reason="client_wait_timeout",
                          caller=caller)
                return self._json(504, {"error": "wait_timeout",
                                        "job_id": job["id"]})
            code, body = job["out"] if job["out"] else (500, {"error": "no_result"})
            if path == "/api/chat":
                body = dict(body) if isinstance(body, dict) else {"error": body}
                body["message"] = {"role": "assistant",
                                   "content": body.get("response", "")}
            elif path == "/api/generate":
                body = dict(body) if isinstance(body, dict) else {"error": body}
                body["model"] = model
                body["done"] = code == 200
            return self._json(code, body)

        # ---- /v1/embed + ollama compat --------------------------------------
        def _embed(self, path, req, caller):
            model = req.get("model")
            if not model:
                return self._json(400, {"error": "missing_model"})
            inp = req.get("input", req.get("prompt"))
            if path == "/api/embeddings" and inp is None:
                inp = req.get("prompt")
            if inp is None:
                return self._json(400, {"error": "missing_input"})
            ok, why, _caps = model_decision(
                sf.policy, model, free_ram_gb() * 1024.0,
                resident=(), game_running=False, kind="embed")
            if not ok and identity_deny(why):
                el.stats["deny"] += 1
                log_event("POLICY_DENY", model=model, reason=why, http=403,
                          caller=caller, stage="prequeue", lane="embed")
                return self._json(403, {"error": "policy_deny", "detail": why})
            job = {"id": str(req.get("job_id") or time.time_ns()),
                   "model": model, "input": inp,
                   "caller": caller, "done_ev": threading.Event(), "out": None}
            try:
                el.submit(job)
            except queue.Full:
                el.stats["reject_queue"] += 1
                log_event("QUEUE_REJECT", model=model, reason="queue_full",
                          caller=caller, lane="embed")
                return self._json(429, {"error": "queue_full", "lane": "embed"})
            if not job["done_ev"].wait(timeout=JOB_TIMEOUT_S + 30):
                el.stats["timeout"] += 1
                return self._json(504, {"error": "wait_timeout",
                                        "job_id": job["id"]})
            code, body = job["out"] if job["out"] else (500, {"error": "no_result"})
            return self._json(code, body)

        # ---- GET routing ------------------------------------------------------
        def do_GET(self):
            path = self.path.split("?", 1)[0]
            if path == "/health":
                names, _ = ollama_ps()
                return self._json(200, {
                    "status": "ok" if names else "degraded",
                    "service": "ollama_orchestrator",
                    "port": PORT,
                    "bind": BIND_HOST,
                    "ollama_up": bool(names),
                    "uptime_s": int(time.time() - sf.started),
                    "game_running": sf._game_flag(),
                    "policy_source": sf.policy_source,
                    "policy_max_tokens_note": "per-model caps in /stats",
                    "free_ram_gb": round(free_ram_gb(), 2),
                    "loaded_generation_models": names,
                })
            if path == "/stats":
                names, _ = ollama_ps()
                return self._json(200, {
                    "service": "ollama_orchestrator",
                    "queue_depths": {c: sf.queues[c].qsize() for c in PRIORITY},
                    "queue_caps": dict(QUEUE_CAPS),
                    "active_model": sf.active_model,
                    "hot_model": sf.hot_model,
                    "loaded_generation_models": names,
                    "game_running": sf._game_flag(),
                    "stats": dict(sf.stats),
                    "embed_stats": dict(el.stats),
                    "recent": sf.recent[-RING_SIZE:],
                    "log_path": LOG_PATH,
                })
            if path == "/v1/status":
                names, _ = ollama_ps()
                return self._json(200, {
                    "service": "ollama_orchestrator",
                    "queue_depth": sum(sf.queues[c].qsize() for c in PRIORITY),
                    "queue_cap": sum(QUEUE_CAPS.values()),
                    "queue_depths": {c: sf.queues[c].qsize() for c in PRIORITY},
                    "active_model": sf.active_model,
                    "hot_model": sf.hot_model,
                    "loaded_models_now": names,
                    "game_running": sf._game_flag(),
                    "policy_source": sf.policy_source,
                    "free_ram_gb": round(free_ram_gb(), 2),
                    "stats": dict(sf.stats),
                    "log_path": LOG_PATH,
                })
            return self._json(404, {"error": "not_found"})

    return Handler


def main():
    global PORT, POLICY_PATH
    ap = argparse.ArgumentParser(description="UNIFY-1 local model gateway")
    ap.add_argument("--port", type=int, default=int(__import__("os").environ.get("MP_ORCH_PORT", PORT)))
    ap.add_argument("--policy", default=__import__("os").environ.get("MP_ORCH_POLICY", POLICY_PATH))
    args = ap.parse_args()
    PORT = args.port
    POLICY_PATH = args.policy

    global GAME_SAMPLER
    policy, path, source = load_policy(args.policy)
    GAME_SAMPLER = GameSampler()
    sf = SingleFlight(policy, source)
    el = EmbedLane(policy)
    # bind host is loopback-ONLY by construction; never expose to the LAN
    httpd = http.server.ThreadingHTTPServer((BIND_HOST, PORT), handler_for(sf, el))
    log_event("SERVICE_START", port=PORT, bind=BIND_HOST, queue_caps=QUEUE_CAPS,
              priority="> ".join(PRIORITY), swap_pacing_s=SWAP_PACING_S,
              keep_alive=KEEP_ALIVE, job_timeout_s=JOB_TIMEOUT_S,
              num_ctx=NUM_CTX, policy_source=source)
    print(f"ollama_orchestrator listening on {BIND_HOST}:{PORT} "
          f"(priority {' > '.join(PRIORITY)}, caps {QUEUE_CAPS}, "
          f"policy {source})", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        log_event("SERVICE_STOP", reason="keyboard_interrupt")


if __name__ == "__main__":
    main()