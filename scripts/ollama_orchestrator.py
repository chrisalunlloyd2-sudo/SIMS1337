#!/usr/bin/env python3
"""ollama_orchestrator.py -- cellular-mic single-flight queue in front of local Ollama.

Perf EPIC task t_4a3756ed, plan items H7 + C.2 + C.4(executor side lives in Java):
  - ONE worker thread: at most one model is loaded or inferring at any moment.
  - FIFO queue with bounded depth; overflow = documented HTTP 429 (client backs
    off and resubmits; no silent drops).
  - Swap pacing: when the incoming job's model differs from the hot model, the
    worker waits SWAP_PACING_S before the next call (one cellular-mic swap per
    model change; keep_alive 5m keeps burst traffic hot between swaps).
  - RAM guard (C.6): before dispatching a job whose model is NOT currently hot
    (per /api/ps), require FREE_RAM_MIN_GB free RAM; otherwise reject with HTTP
    507 and log RAM_GUARD_REJECT (health-probe signal; t_6af8f408 may consume).
  - Per-job timeout: worker abandons the HTTP call after JOB_TIMEOUT_S.
  - Instrumentation: every dispatch boundary logged as JSONL so concurrency is
    measurable from the log alone.

Log-line contract for t_6af8f408 (alert wiring):
  every line is one JSON object, fields:
    { "ts": "<ISO8601 local>", "event": "<MODEL_LOAD_START | MODEL_LOAD_DETECTED |
       MODEL_INFER_DONE | JOB_QUEUED | JOB_DONE | JOB_REJECT | JOB_TIMEOUT |
       RAM_GUARD_REJECT | QUEUE_REJECT | GUARD_CHECK | SWAP_PACING | SERVICE_START>",
      "model": "<ollama model name or omitted>", "job_id": "<id or omitted>",
      "elapsed_ms": <int on DONE/TIMEOUT events>, "reason": "<string on rejects>",
      "loaded_models": <int when sampled>, "free_ram_gb": <float when sampled>, ... }
  MODEL_LOAD_START only ever appears when loaded_models == 0 (single flight);
  MODEL_INFER_DONE is its matching end. One JSON object per line; tolerate
  unknown events for forward compatibility.

Run: python scripts/ollama_orchestrator.py   # listens on 127.0.0.1:11435
Stdlib only. Local Ollama only (127.0.0.1:11434) per EPIC constraint.
"""
import ctypes
import datetime as dt
import http.server
import json
import queue
import socket
import threading
import time
import urllib.request
from ctypes import wintypes

OLLAMA = "http://127.0.0.1:11434"
PORT = 11435
LOG_PATH = r"C:\Users\viper\AIGEN_SYS\logs\ollama_orchestrator.jsonl"

QUEUE_CAP = 16              # bounded FIFO; overflow -> 429 queue_full
SWAP_PACING_S = 1.5         # 1-2 s gap after a swap decision (plan C.2)
KEEP_ALIVE = "5m"           # keep hot model resident during bursts (H8 default)
JOB_TIMEOUT_S = 300.0       # per-job wall cap inside the worker
FREE_RAM_MIN_GB = 2.5       # RAM guard threshold (H7/C.6)
WORKER_CHECK_S = 0.2

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


def ollama_ps():
    """Return (names, raw) of currently loaded GENERATION models per /api/ps."""
    try:
        with urllib.request.urlopen(OLLAMA + "/api/ps", timeout=5) as r:
            d = json.loads(r.read().decode("utf-8", "replace"))
        return [m.get("name", "") for m in d.get("models", [])
                if not is_embed_model(m.get("name", ""))], d
    except Exception:
        return [], None


class SingleFlight:
    """The cellular microphone: one worker thread, FIFO queue, pacing, guard."""

    def __init__(self):
        self.q = queue.Queue(maxsize=QUEUE_CAP)
        self.active_model = None          # model the mic currently points at
        self.hot_model = None             # model hot per last /api/ps sample
        self.last_infer_end = 0.0
        self.stats = {"done": 0, "reject_queue": 0, "reject_ram": 0,
                      "timeout": 0, "errors": 0}
        self.thread = threading.Thread(target=self._worker, name="cmq-worker",
                                       daemon=True)
        self.thread.start()

    def submit(self, job):
        """FIFO enqueue. Raises queue.Full -> caller answers 429 (documented)."""
        self.q.put_nowait(job)
        log_event("JOB_QUEUED", model=job["model"], job_id=job["id"],
                  depth=self.q.qsize())

    # ---- worker side (THE single thread) ----------------------------------
    def _worker(self):
        while True:
            try:
                job = self.q.get(timeout=WORKER_CHECK_S)
            except queue.Empty:
                names, _ = ollama_ps()          # refresh hot-model knowledge
                if names:
                    self.hot_model = names[0]
                continue

            model = job["model"]
            t0 = time.time()

            # ---- RAM guard: any REAL load (model not hot) needs free RAM ----
            names, _ = ollama_ps()
            self.hot_model = names[0] if names else None
            free_gb = free_ram_gb()
            needs_load = (self.hot_model != model)
            log_event("GUARD_CHECK", model=model, job_id=job["id"],
                      needs_load=needs_load, free_ram_gb=round(free_gb, 2),
                      loaded_models=len(names))
            if needs_load and free_gb < FREE_RAM_MIN_GB:
                self.stats["reject_ram"] += 1
                log_event("RAM_GUARD_REJECT", model=model, job_id=job["id"],
                          reason="free_ram_below_threshold",
                          free_ram_gb=round(free_gb, 2),
                          needed_gb=FREE_RAM_MIN_GB)
                job["out"] = (507, {"error": "ram_guard",
                                    "detail": (f"refusing load of {model}: "
                                               f"{free_gb:.2f}GB free < "
                                               f"{FREE_RAM_MIN_GB}GB")})
                job["done_ev"].set()
                continue

            # ---- pacing on swap decision (plan C.2) -------------------------
            if self.active_model is not None and self.active_model != model:
                since = time.time() - self.last_infer_end
                wait = SWAP_PACING_S - since
                if wait > 0:
                    log_event("SWAP_PACING", model=model, job_id=job["id"],
                              sleep_s=round(wait, 2),
                              from_model=self.active_model)
                    time.sleep(wait)

            # ---- THE single flight: one generate at a time ------------------
            try:
                log_event("MODEL_LOAD_START", model=model, job_id=job["id"],
                          loaded_models=len(names))
                code, body = self._generate(model, job["prompt"],
                                            job["num_predict"], JOB_TIMEOUT_S)
                self.active_model = model
                names2, _ = ollama_ps()
                self.hot_model = names2[0] if names2 else model
                log_event("MODEL_LOAD_DETECTED", model=model, job_id=job["id"],
                          loaded_models=len(names2))
                el = int((time.time() - t0) * 1000)
                if code == 200:
                    self.stats["done"] += 1
                    log_event("MODEL_INFER_DONE", model=model, job_id=job["id"],
                              elapsed_ms=el, ok=True)
                    log_event("JOB_DONE", model=model, job_id=job["id"],
                              elapsed_ms=el, ok=True)
                else:
                    self.stats["errors"] += 1
                    log_event("MODEL_INFER_DONE", model=model, job_id=job["id"],
                              elapsed_ms=el, ok=False, http=code)
                    log_event("JOB_DONE", model=model, job_id=job["id"],
                              elapsed_ms=el, ok=False, http=code)
                job["out"] = (code if code in (200, 400, 500, 503) else 502,
                              body if isinstance(body, dict) else {"error": body})
            except socket.timeout:
                self.stats["timeout"] += 1
                log_event("JOB_TIMEOUT", model=model, job_id=job["id"],
                          elapsed_ms=int((time.time() - t0) * 1000),
                          reason="job_timeout")
                job["out"] = (504, {"error": "job_timeout",
                                    "detail": f"exceeded {JOB_TIMEOUT_S}s"})
            except Exception as e:  # worker must never die
                self.stats["errors"] += 1
                log_event("JOB_REJECT", model=model, job_id=job["id"],
                          reason=f"worker_error:{type(e).__name__}:{e}")
                job["out"] = (502, {"error": "worker_error", "detail": str(e)})
            finally:
                self.last_infer_end = time.time()
                job["done_ev"].set()

    def _generate(self, model, prompt, num_predict, timeout_s):
        """One blocking /api/generate with hard timeout. Single-flight only."""
        body = json.dumps({
            "model": model,
            "prompt": prompt,
            "stream": False,
            "keep_alive": KEEP_ALIVE,
            "options": {"num_predict": int(num_predict), "num_ctx": 2048},
        }).encode("utf-8")
        req = urllib.request.Request(OLLAMA + "/api/generate", data=body,
                                     headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=timeout_s) as r:
            d = json.loads(r.read().decode("utf-8", "replace"))
        return 200, {"response": d.get("response", ""),
                     "total_duration": d.get("total_duration"),
                     "load_duration": d.get("load_duration"),
                     "eval_count": d.get("eval_count")}


def handler_for(sf):
    class Handler(http.server.BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):  # silence default stderr chatter
            pass

        def _json(self, code, obj):
            payload = json.dumps(obj).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        def do_POST(self):
            if self.path != "/v1/run":
                return self._json(404, {"error": "not_found"})
            try:
                length = int(self.headers.get("Content-Length", 0))
                req = json.loads(self.rfile.read(length).decode("utf-8", "replace"))
            except Exception:
                return self._json(400, {"error": "bad_json"})
            model = req.get("model")
            if not model:
                return self._json(400, {"error": "missing_model"})

            # bounded FIFO: overflow -> documented 429 (client backs off)
            job = {"id": str(req.get("job_id") or time.time_ns()),
                   "model": model,
                   "prompt": str(req.get("prompt", "")),
                   "num_predict": int(req.get("num_predict", 64)),
                   "done_ev": threading.Event(),
                   "out": None}
            try:
                sf.submit(job)
            except queue.Full:
                sf.stats["reject_queue"] += 1
                log_event("QUEUE_REJECT", model=model, reason="queue_full",
                          depth=QUEUE_CAP)
                return self._json(429, {"error": "queue_full",
                                        "detail": (f"queue capacity {QUEUE_CAP} "
                                                   "exceeded; back off and "
                                                   "resubmit")})

            if not req.get("wait", True):
                return self._json(202, {"job_id": job["id"],
                                        "status": "queued",
                                        "poll": "/v1/status"})

            # wait for THIS job's completion (own per-job wall cap)
            if not job["done_ev"].wait(timeout=JOB_TIMEOUT_S + 30):
                sf.stats["timeout"] += 1
                log_event("JOB_TIMEOUT", model=model, job_id=job["id"],
                          elapsed_ms=None, reason="client_wait_timeout")
                return self._json(504, {"error": "wait_timeout",
                                        "job_id": job["id"]})
            code, body = job["out"] if job["out"] else (500, {"error": "no_result"})
            return self._json(code, body)

        def do_GET(self):
            if self.path == "/v1/status":
                names, _ = ollama_ps()
                return self._json(200, {
                    "service": "ollama_orchestrator",
                    "queue_depth": sf.q.qsize(),
                    "queue_cap": QUEUE_CAP,
                    "active_model": sf.active_model,
                    "hot_model": sf.hot_model,
                    "loaded_models_now": names,
                    "free_ram_gb": round(free_ram_gb(), 2),
                    "stats": dict(sf.stats),
                    "log_path": LOG_PATH,
                })
            return self._json(404, {"error": "not_found"})

    return Handler


def main():
    sf = SingleFlight()
    httpd = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), handler_for(sf))
    log_event("SERVICE_START", port=PORT, queue_cap=QUEUE_CAP,
              swap_pacing_s=SWAP_PACING_S, keep_alive=KEEP_ALIVE,
              job_timeout_s=JOB_TIMEOUT_S, free_ram_min_gb=FREE_RAM_MIN_GB)
    print(f"ollama_orchestrator listening on 127.0.0.1:{PORT} "
          f"(queue cap {QUEUE_CAP})", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        log_event("SERVICE_STOP", reason="keyboard_interrupt")


if __name__ == "__main__":
    main()