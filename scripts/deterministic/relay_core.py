#!/usr/bin/env python3
"""Aegis↔Qwen Relay Core — file-based inbox/outbox over localhost.
Qwen (Termux) POSTs commands to :5000/relay/send; Aegis reads inbox.jsonl,
acts, writes replies to outbox.jsonl which Qwen polls at /relay/poll.
No cloud, no auth needed (loopback only). Persistent across restarts."""
import json, os, time, threading

RELAY_DIR = "/root/hexgame/relay"
INBOX = os.path.join(RELAY_DIR, "inbox.jsonl")
OUTBOX = os.path.join(RELAY_DIR, "outbox.jsonl")
LOCK = threading.Lock()

def _now():
    return time.strftime("%Y-%m-%dT%H:%M:%S%z")

def _append(path, obj):
    with LOCK:
        with open(path, "a") as f:
            f.write(json.dumps(obj) + "\n")

def _read_all(path):
    if not os.path.exists(path):
        return []
    out = []
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line:
                try: out.append(json.loads(line))
                except Exception: pass
    return out

def send(msg_type, sender, body, meta=None):
    """Append a message to the inbox (Qwen→Aegis or Aegis self-note)."""
    m = {"ts": _now(), "type": msg_type, "from": sender, "body": body}
    if meta: m["meta"] = meta
    _append(INBOX, m)
    return m

def reply(to_ts, sender, body, meta=None):
    """Append a reply to the outbox (Aegis→Qwen)."""
    m = {"ts": _now(), "in_reply_to": to_ts, "from": sender, "body": body}
    if meta: m["meta"] = meta
    _append(OUTBOX, m)
    return m

def poll_outbox(since_ts=None):
    """Return outbox messages newer than since_ts (Qwen polls this)."""
    msgs = _read_all(OUTBOX)
    if since_ts:
        msgs = [m for m in msgs if m["ts"] > since_ts]
    return msgs

def get_inbox(limit=50):
    """Return recent inbox messages (Aegis reads this on heartbeat)."""
    return _read_all(INBOX)[-limit:]

def state_summary():
    """Quick state snapshot for Qwen's orientation."""
    s = {}
    try:
        with open("/root/hexgame/game_state.json") as f:
            s["game"] = json.load(f)
    except Exception:
        s["game"] = {"error": "game_state.json not readable"}
    try:
        s["servers"] = {
            "deck_5000": _http_ok("http://localhost:5000/healthz"),
            "llama_5001": _http_ok("http://localhost:5001/health"),
        }
    except Exception:
        s["servers"] = {"error": "health check failed"}
    return s


def next_steps(pool_path="/root/hexgame/task_pool.json", limit=5):
    """Serve the open task pool to agents (deterministic, no model)."""
    import json as _json, os
    if not os.path.exists(pool_path):
        return {"ok": False, "error": "pool not found"}
    try:
        pool = _json.load(open(pool_path))
    except Exception as e:
        return {"ok": False, "error": str(e)}
    open_tasks = [t for t in pool if not t.get("done")]
    open_tasks.sort(key=lambda t: 0 if t.get("priority") == "high" else (1 if t.get("priority") == "medium" else 2))
    return {"ok": True, "open": len(open_tasks), "tasks": open_tasks[:limit],
            "updated": _now()}

def _http_ok(url, timeout=3):
    import urllib.request
    try:
        urllib.request.urlopen(url, timeout=timeout)
        return "UP"
    except Exception:
        return "DOWN"
