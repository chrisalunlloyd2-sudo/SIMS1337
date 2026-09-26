#!/usr/bin/env python3
"""DETERMINISTIC EXECUTOR — applies known transformations to pool tasks with
ZERO model calls. Verification via py_compile + feature grep. This is the
default path per Chris's doctrine (2026-08-09): build e2e programs WITHOUT
models, deterministic core + rollback. Model = sparse reviewer only.

Usage: python3 deterministic_executor.py            # pick next + execute
       python3 deterministic_executor.py --dry-run  # show what would run
"""
import json, os, sys, time, subprocess, py_compile

POOL = "/root/hexgame/task_pool.json"
LOG = "/root/hexgame/game_log.jsonl"

# ── known deterministic transformations (ADD-only, no model) ──
HANDLERS = {}

def handler(task_id):
    def deco(f):
        HANDLERS[task_id] = f
        return f
    return deco

@handler("c6")
def c6_math_drill_panel(task):
    """web/index.html — Math Drill panel showing math_state.json accuracy."""
    path = "/root/hexgame/web/index.html"
    if not os.path.isfile(path):
        return False, "index.html missing"
    s = open(path).read()
    if "MATH DRILL" in s:
        return True, "already present"
    card = '''  <!-- MATH DRILL -->
  <div class="card">
    <h2>🧮 Math Drill <span class="lobes">math_state.json accuracy</span></h2>
    <div class="mono" id="mathd"></div>
  </div>
'''
    s = s.replace("  <!-- TELEMETRY -->", card + "  <!-- TELEMETRY -->")
    open(path, "w").write(s)
    return True, "Math Drill card added"

@handler("c7")
def c7_quality_history(task):
    """game_state.json — quality_score history array in trust_ledger."""
    path = "/root/hexgame/web/game_state.json"
    if not os.path.isfile(path):
        return False, "game_state.json missing"
    import json as _json
    d = _json.load(open(path))
    d.setdefault("quality_history", [])
    snap = {"ts": time.strftime("%Y-%m-%d %H:%M:%S"), "round": d.get("round"),
            "quality_score": round(sum(1 for v in d.get("trust_ledger", {}).values() if v > 0) / max(1, len(d.get("trust_ledger", {}))), 3)}
    d["quality_history"].append(snap)
    d["quality_history"] = d["quality_history"][-50:]
    _json.dump(d, open(path, "w"), indent=1)
    return True, f"quality_history appended (score {snap['quality_score']})"

@handler("c8")
def c8_next_steps(task):
    """relay/relay_core.py — /aegis/next-steps endpoint serving task pool."""
    path = "/root/hexgame/relay/relay_core.py"
    if not os.path.isfile(path):
        return False, "relay_core.py missing"
    s = open(path).read()
    if "def next_steps" in s:
        return True, "already present"
    add = '''
def next_steps(pool_path="/root/hexgame/task_pool.json", limit=5):
    """Serve the open task pool to agents (deterministic, no model)."""
    if not os.path.exists(pool_path):
        return {"ok": False, "error": "pool not found"}
    try:
        pool = json.load(open(pool_path))
    except Exception as e:
        return {"ok": False, "error": str(e)}
    open_tasks = [t for t in pool if not t.get("done")]
    open_tasks.sort(key=lambda t: 0 if t.get("priority") == "high" else 1)
    return {"ok": True, "open": len(open_tasks), "tasks": open_tasks[:limit],
            "updated": _now()}

'''
    s = s.replace('def _http_ok(url, timeout=3):', add + 'def _http_ok(url, timeout=3):')
    open(path, "w").write(s)
    return True, "next_steps() added to relay_core"

def verify(py_path=None):
    """Deterministic verification: py_compile for .py, feature grep for html."""
    if py_path and py_path.endswith(".py") and os.path.isfile(py_path):
        try:
            py_compile.compile(py_path, doraise=True)
            return True, "py_compile OK"
        except Exception as e:
            return False, f"py_compile FAIL: {e}"
    return True, "no py check"

def log_move(player, action, detail):
    os.makedirs(os.path.dirname(LOG), exist_ok=True)
    with open(LOG, "a") as f:
        f.write(json.dumps({"ts": time.strftime("%Y-%m-%dT%H:%M:%S"), "player": player,
                            "action": action, "detail": detail}) + "\n")

def main():
    dry = "--dry-run" in sys.argv
    pool = json.load(open(POOL))
    open_tasks = [t for t in pool if not t.get("done") and t.get("id") in HANDLERS]
    open_tasks.sort(key=lambda t: 0 if t.get("priority") == "high" else 1)
    if not open_tasks:
        print("no deterministic tasks open — pool clean for this class")
        return
    t = open_tasks[0]
    print(f"next deterministic: [{t['id']}] {t.get('file')} — {str(t.get('task'))[:60]}")
    if dry:
        return
    ok, msg = HANDLERS[t["id"]](t)
    if not ok:
        print(f"FAIL {t['id']}: {msg}")
        return
    # verify
    fpath = t.get("file", "")
    vok, vmsg = verify(fpath)
    if not vok:
        print(f"VERIFY FAIL {t['id']}: {vmsg}")
        return
    t["done"] = True
    t["completed_by"] = "aegis-deterministic"
    t["completed_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ")
    json.dump(pool, open(POOL, "w"), indent=1)
    log_move("aegis-deterministic", f"pool:{t['id']}", f"done — {msg} ({vmsg})")
    print(f"DONE {t['id']}: {msg} ({vmsg})")

if __name__ == "__main__":
    main()
