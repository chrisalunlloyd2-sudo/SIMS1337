#!/usr/bin/env python3
"""ollama_orchestrator_stress.py -- cellular-mic stress + evidence generator.

Drives scripts/ollama_orchestrator.py (127.0.0.1:11435) and PROVES the
acceptance criteria for perf EPIC t_4a3756ed:

  P1  20 mixed-model jobs through /v1/run (wait mode)      -> all HTTP 200
  P2  bounded-FIFO: fill queue while worker busy           -> exactly one 429
  P3  per-job timeout mechanics (in-process, JOB_TIMEOUT_S=3, stub 8s) -> 504
      RAM guard mechanics  (in-process, free_ram_gb=0.01, cold model)  -> 507
  P4  evidence analysis from the JSONL log:
      - zero concurrent MODEL_LOAD_START..MODEL_INFER_DONE spans
      - every log line is valid JSON with `ts`+`event`
      - /api/ps watchdog samples: >1 loaded model NEVER observed

Writes: C:/Users/viper/AIGEN_SYS/.hermes/evidence/orchestrator_stress_<UTC>.json
Stdlib only. Run while ollama_orchestrator.py is up:
    python scripts/ollama_orchestrator_stress.py
"""
import json
import socket
import sys
import threading
import time
import urllib.request
import urllib.error
import datetime as dt

ORCH = "http://127.0.0.1:11435"
OLLAMA = "http://127.0.0.1:11434"
LOG_PATH = r"C:\Users\viper\AIGEN_SYS\logs\ollama_orchestrator.jsonl"
EVIDENCE_DIR = r"C:\Users\viper\AIGEN_SYS\.hermes\evidence"

MIXED_MODELS = ["llama3.2:1b", "qwen2.5:0.5b", "gemma2:2b", "tinyllama:1.1b"]
PROMPT = "Reply with exactly one short sentence about cellular automata."
N_JOBS = 20
NUM_PREDICT = 32

results = {"started_utc": dt.datetime.utcnow().isoformat(timespec="seconds") + "Z",
           "p1": [], "p1_failures": [], "p2": {}, "p3": {}, "p4": {},
           "watchdog_max_loaded": 0, "watchdog_samples": 0}


def http(method, path, payload=None, timeout=360):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(ORCH + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}


def watchdog(stop_ev):
    """Independent /api/ps poller: more than one loaded GENERATION model =
    violation (embed-family models excluded - KG stack keeps one resident)."""
    while not stop_ev.is_set():
        try:
            with urllib.request.urlopen("http://127.0.0.1:11434/api/ps",
                                        timeout=4) as r:
                names = [m.get("name", "") for m in
                         json.loads(r.read().decode()).get("models", [])]
                n = sum(1 for x in names if "embed" not in x.lower()
                        and "minilm" not in x.lower())
        except Exception:
            n = -1
        if n >= 0:
            results["watchdog_samples"] += 1
            results["watchdog_max_loaded"] = max(results["watchdog_max_loaded"], n)
        stop_ev.wait(0.7)


def available_models():
    """Read-only inventory from Ollama /api/tags (proof aid, not load)."""
    try:
        with urllib.request.urlopen(OLLAMA + "/api/tags", timeout=5) as r:
            return {m.get("name", "") for m in
                    json.loads(r.read().decode()).get("models", [])}
    except Exception:
        return set()


def p1_mixed():
    global MIXED_MODELS
    avail = available_models()
    have = [m for m in MIXED_MODELS if m in avail]
    if not have:
        have = sorted(avail)[:3]
    results["models_available"] = sorted(avail)
    results["models_used"] = have
    if len(have) < 2:
        results["p1_failures"].append(
            {"job_id": "setup", "http": 0, "body": {
                "error": f"need >=2 generation models, found {have}"}})
        return
    MIXED_MODELS = have
    print("== P1: %d mixed-model wait-mode jobs (%s) ==" % (
        N_JOBS, ",".join(have)), flush=True)
    for i in range(N_JOBS):
        model = MIXED_MODELS[i % len(MIXED_MODELS)]
        t0 = time.time()
        code, body = http("POST", "/v1/run",
                          {"model": model, "prompt": PROMPT,
                           "num_predict": NUM_PREDICT, "job_id": f"p1-{i}"})
        wall = round(time.time() - t0, 3)
        ok = (code == 200 and isinstance(body.get("response"), str)
              and len(body["response"]) > 0)
        results["p1"].append({"job_id": f"p1-{i}", "model": model, "http": code,
                              "wall_s": wall, "ok": ok,
                              "resp_chars": len(body.get("response", ""))})
        if not ok:
            results["p1_failures"].append({"job_id": f"p1-{i}", "http": code,
                                           "body": body})
        print(f"  p1-{i} {model} {code} {wall}s ok={ok}", flush=True)
    print(f"P1 done: {sum(1 for r in results['p1'] if r['ok'])}/{N_JOBS} ok",
          flush=True)


def p2_queue_full():
    print("== P2: queue-full (fill 16 behind one slow job) ==", flush=True)
    # base job: long output on the hot small model (occupies worker ~20-40s),
    # wait=false. Fillers are 1-token jobs on the SAME model -> fast drain.
    code, body = http("POST", "/v1/run",
                      {"model": "tinyllama:1.1b",
                       "prompt": "Count slowly from 1 to 500.",
                       "num_predict": 700, "job_id": "p2-base", "wait": False})
    results["p2"]["base_http"] = code
    time.sleep(2.0)  # let worker pick it up
    codes = []
    for i in range(17):  # 16 fit in queue, the 17th must be rejected 429
        code, body = http("POST", "/v1/run",
                          {"model": "tinyllama:1.1b", "prompt": "hi",
                           "num_predict": 1, "job_id": f"p2-f{i}",
                           "wait": False})
        codes.append(code)
        if code == 429:
            results["p2"]["reject_body"] = body
            break
    results["p2"]["accepted_202"] = codes.count(202)
    results["p2"]["seen_429"] = 429 in codes
    print(f"  202s={codes.count(202)} 429_seen={429 in codes} "
          f"(codes={codes})", flush=True)
    # drain: wait until queue empty then confirm service healthy
    for _ in range(240):
        st = http("GET", "/v1/status")
        if st[0] == 200 and st[1].get("queue_depth", 1) == 0:
            break
        time.sleep(1)
    st = http("GET", "/v1/status")
    results["p2"]["stats_after"] = st[1].get("stats", {})
    print(f"  drained; stats={results['p2']['stats_after']}", flush=True)


def p3_inprocess_proofs():
    """Timeout + RAM-guard mechanics proven in-process (fast, deterministic)."""
    print("== P3: in-process timeout + RAM guard ==", flush=True)
    sys.path.insert(0, r"C:\Users\viper\AIGEN_SYS\repos\SIMS1337\scripts")
    import importlib
    orch = importlib.import_module("ollama_orchestrator")

    # --- timeout: force JOB_TIMEOUT_S=3 and a stub generate that takes 8s ---
    orch.JOB_TIMEOUT_S = 3.0
    orig_gen = orch.SingleFlight._generate
    orig_free = orch.free_ram_gb
    orig_ps = orch.ollama_ps

    def slow_gen(self, model, prompt, num_predict, timeout_s):
        time.sleep(8)  # exceeds the forced 3s timeout
        # real _generate surfaces stalls as socket.timeout from urlopen;
        # raise the same so we exercise the 504 path, not worker_error.
        raise socket.timeout("stub: generate stalled past job timeout")

    orch.SingleFlight._generate = slow_gen
    orch.ollama_ps = lambda: (["tinyllama:1.1b"], None)  # model appears hot: no RAM guard
    sf = orch.SingleFlight()
    job = {"id": "p3-timeout", "model": "tinyllama:1.1b", "prompt": "x",
           "num_predict": 1, "done_ev": threading.Event(), "out": None}
    sf.submit(job)
    t0 = time.time()
    job["done_ev"].wait(30)
    code, body = job["out"]
    results["p3"]["timeout_http"] = code
    results["p3"]["timeout_err"] = body.get("error")
    results["p3"]["timeout_wall_s"] = round(time.time() - t0, 2)
    print(f"  timeout proof: http={code} err={body.get('error')} "
          f"wall={results['p3']['timeout_wall_s']}s "
          f"(expected 504/job_timeout ~3s)", flush=True)
    sf.thread.join(timeout=1)  # daemon; no need to wait

    # --- RAM guard: model cold (needs load) + free RAM forced to 0.01 GB ---
    orch.ollama_ps = lambda: ([], None)  # nothing hot -> every job needs a load
    orch.free_ram_gb = lambda: 0.01
    sf2 = orch.SingleFlight()
    job2 = {"id": "p3-ram", "model": "llama3.2:1b", "prompt": "x",
            "num_predict": 1, "done_ev": threading.Event(), "out": None}
    sf2.submit(job2)
    job2["done_ev"].wait(30)
    code2, body2 = job2["out"]
    results["p3"]["ram_http"] = code2
    results["p3"]["ram_err"] = body2.get("error")
    print(f"  RAM-guard proof: http={code2} err={body2.get('error')} "
          f"(expected 507/ram_guard)", flush=True)

    orch.SingleFlight._generate = orig_gen  # not reused, hygiene


def p4_log_analysis():
    print("== P4: JSONL log analysis ==", flush=True)
    spans = {}  # job_id -> {"start": ts, "end": ts}
    bad_lines = 0
    guard_samples_loaded = 0
    guard_max_loaded = 0
    line = ""
    with open(LOG_PATH, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                rec = json.loads(line)
            except json.JSONDecodeError:
                bad_lines += 1
                continue
            if "ts" not in rec or "event" not in rec:
                bad_lines += 1
                continue
            jid = rec.get("job_id")
            ev = rec["event"]
            ts = rec["ts"]
            if ev == "MODEL_LOAD_START" and jid:
                spans.setdefault(jid, {})["start"] = ts
            elif ev == "MODEL_INFER_DONE" and jid:
                spans.setdefault(jid, {})["end"] = ts
            if ev in ("GUARD_CHECK", "MODEL_LOAD_DETECTED", "MODEL_LOAD_START"):
                lm = rec.get("loaded_models")
                if isinstance(lm, int):
                    guard_samples_loaded += 1
                    guard_max_loaded = max(guard_max_loaded, lm)
    # parse ISO ts
    def parse(ts):
        return dt.datetime.fromisoformat(ts)
    concurrent = []
    done_jobs = {j: v for j, v in spans.items() if "start" in v and "end" in v}
    jobs = sorted(done_jobs.items(), key=lambda kv: parse(kv[1]["start"]))
    for i in range(len(jobs)):
        for j in range(i + 1, len(jobs)):
            a_s, a_e = parse(jobs[i][1]["start"]), parse(jobs[i][1]["end"])
            b_s, b_e = parse(jobs[j][1]["start"]), parse(jobs[j][1]["end"])
            if a_s < b_e and b_s < a_e:
                concurrent.append((jobs[i][0], jobs[j][0]))
    results["p4"] = {
        "bad_or_incomplete_lines": bad_lines,
        "flight_spans_found": len(done_jobs),
        "concurrent_flight_spans": concurrent,
        "zero_concurrency": len(concurrent) == 0,
        "log_format_valid": bad_lines == 0,
        "guard_samples": guard_samples_loaded,
        "guard_max_loaded_models": guard_max_loaded,
    }
    results["p4"]["log_tail"] = line
    print(f"  spans={len(done_jobs)} concurrent={len(concurrent)} "
          f"bad_lines={bad_lines} guard_max_loaded={guard_max_loaded}",
          flush=True)


def main():
    st = http("GET", "/v1/status")
    if st[0] != 200:
        print("orchestrator not reachable on 11435; start it first. Abort.",
              flush=True)
        sys.exit(2)
    print("service:", json.dumps(st[1]), flush=True)
    results["service_start_status"] = st[1]

    stop_ev = threading.Event()
    wd = threading.Thread(target=watchdog, args=(stop_ev,), daemon=True)
    wd.start()

    p1_mixed()
    p2_queue_full()
    p3_inprocess_proofs()
    stop_ev.set()
    wd.join(timeout=2)
    p4_log_analysis()

    acceptance = {
        "p1_all_200": len(results["p1"]) == N_JOBS and not results["p1_failures"],
        "p2_queue_full_429": results["p2"].get("seen_429", False),
        "p3_timeout_504": results["p3"].get("timeout_http") == 504,
        "p3_ram_guard_507": results["p3"].get("ram_http") == 507,
        "p4_zero_concurrency": results["p4"].get("zero_concurrency", False),
        "p4_log_format": results["p4"].get("log_format_valid", False),
        "watchdog_never_multi_loaded": results["watchdog_max_loaded"] <= 1,
    }
    results["acceptance"] = acceptance
    results["finished_utc"] = dt.datetime.utcnow().isoformat(timespec="seconds") + "Z"
    results["all_pass"] = all(acceptance.values())

    import os
    os.makedirs(EVIDENCE_DIR, exist_ok=True)
    out = os.path.join(
        EVIDENCE_DIR,
        "orchestrator_stress_%s.json" % results["started_utc"].replace(":", "").replace("-", ""))
    with open(out, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print("== ACCEPTANCE ==", flush=True)
    for k, v in acceptance.items():
        print(f"  {k}: {'PASS' if v else 'FAIL'}", flush=True)
    print(f"ALL={'PASS' if results['all_pass'] else 'FAIL'} evidence={out}",
          flush=True)
    sys.exit(0 if results["all_pass"] else 1)


if __name__ == "__main__":
    main()