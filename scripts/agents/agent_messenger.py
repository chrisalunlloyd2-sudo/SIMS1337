#!/usr/bin/env python3
"""
agent_messenger.py — crew→gateway messaging layer for LIVE-1 (t_51758f17).

2026-11-20 update: this file used to expect the lost GGUF house server on
:5000 (`gguf_server_v2.py`, no longer in the repo). It now speaks to the
UNIFY-1 ollama_orchestrator gateway (127.0.0.1:11435) that sits in front of
Ollama :11434, using the same ollama-shape `POST /api/generate` contract the
gateway's compat lane implements (`{"prompt","num_predict"}` ->
`{"model","response","done"}`). Crew jobs ride the `X-MP-Caller: crew`
priority queue (game > crew > batch) and are serialized behind the game by
the gateway's single-flight worker — that IS the C1 frame-budget mechanism.

Doctrine is unchanged: if the server is down we RAISE — no mocks, no
fabricated replies, no retries-that-fabricate. A caller may catch, report
and stop; the layer itself never invents tokens.

Env overrides (unchanged names):
  MP_GATEWAY_BASE / GGUF_BASE  — default http://127.0.0.1:11435 (:5000 is
                                 dead; both vars are honored so old callers
                                 and new envs keep working)
  AGENT_MEM_DIR — memory dir (default ../../memory)
  AGENT_CTX_TURNS — memory turns replayed as context (default 8)
  AGENT_TIMEOUT — seconds per generate (default 90; gateway caps per-model)
  AGENT_NUM_THREAD — num_thread passthrough, clamped by the gateway 1..16.
                     OPT-IN ONLY ("" default sends nothing): Ollama restarts
                     the runner when num_thread differs from the loaded one,
                     which 503s against a busy slot (observed 2026-11-20).

Windows note: this module runs as a Hermes --no-agent cron script, where the
interpreter is the plain venv python 3.11 without a UTF-8 console; that is
why the print fallback below replaces non-ASCII before printing.
"""
import json, os, sys, time, urllib.request, urllib.error


def _env(name, default):
    return os.environ.get(name, default)


DEFAULT_BASE = "http://127.0.0.1:11435"
BASE = _env("MP_GATEWAY_BASE", None) or _env("GGUF_BASE", None) or DEFAULT_BASE
MEM_DIR = _env(
    "AGENT_MEM_DIR",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "memory"),
)
CTX_TURNS = int(os.environ.get("AGENT_CTX_TURNS", "8"))
TIMEOUT = int(os.environ.get("AGENT_TIMEOUT", "90"))
# num_thread opt-in ONLY: Ollama restarts the runner when num_thread differs
# from the loaded one, which 503s against a busy slot (observed 2026-11-20);
# default OFF lets Ollama keep its own thread count. Set AGENT_NUM_THREAD=2
# to override knowingly.
NUM_THREAD = os.environ.get("AGENT_NUM_THREAD") or ""   # "" = send nothing


def healthz(base=None):
    """True iff the gateway answers and reports Ollama itself up."""
    base = base or BASE
    try:
        with urllib.request.urlopen(f"{base}/health", timeout=15) as r:
            body = json.loads(r.read().decode() or "{}")
            return r.status == 200 and bool(body.get("ollama_up"))
    except Exception:
        return False


def generate(prompt, max_tokens=64, model=None, timeout=None, retries=2):
    """One real generate call through the gateway. Raises on terminal failure —
    never fabricates. A transient busy-load HTTP 503/500 from Ollama (slot
    0 occupied by a long inference, num_parallel=1) is retried a bounded
    number of times with the SAME prompt; every other failure is terminal."""
    payload = {
        "model": model or _env("AGENT_MODEL", "qwen2.5:0.5b"),
        "prompt": prompt,
        "num_predict": int(max_tokens),
        "stream": False,
    }
    if NUM_THREAD not in (None, ""):
        payload["num_thread"] = int(NUM_THREAD)
    body = json.dumps(payload).encode()
    last = None
    for attempt in range(1 + max(0, retries)):
        req = urllib.request.Request(
            f"{BASE}/api/generate", data=body,
            headers={"Content-Type": "application/json", "X-MP-Caller": "crew"})
        try:
            with urllib.request.urlopen(req, timeout=timeout or TIMEOUT) as r:
                data = json.loads(r.read().decode())
            resp = (data.get("response") or "").strip()
            if not resp:
                raise RuntimeError(
                    f"gateway returned empty response for: {prompt[:60]}...")
            return resp, data.get("model", "?")
        except urllib.error.HTTPError as e:
            detail = ""
            try:
                detail = e.read().decode()[:200]
            except Exception:
                pass
            last = RuntimeError(f"gateway HTTP {e.code}: {detail}")
            if e.code in (503, 500, 502, 504) and attempt < retries:
                time.sleep(3 + 4 * attempt)   # busy slot / swap in progress
                continue
            raise last
        except Exception as e:
            raise RuntimeError(f"gateway unreachable at {BASE}: {e}")
    raise last


class AgentMemory:
    """REAL append-only memory per agent: AGENT_MEM_DIR/agent_<name>.jsonl"""

    def __init__(self, name):
        self.name = name
        os.makedirs(MEM_DIR, exist_ok=True)
        self.path = os.path.join(MEM_DIR, f"agent_{name}.jsonl")

    def append(self, role, text, meta=None):
        entry = {"t": time.time(), "agent": self.name, "role": role,
                 "text": text, "meta": meta or {}}
        with open(self.path, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry) + "\n")

    def recent(self, n=CTX_TURNS):
        if not os.path.exists(self.path):
            return []
        with open(self.path, encoding="utf-8") as f:
            lines = [json.loads(l) for l in f if l.strip()]
        return lines[-n:]

    def count(self):
        if not os.path.exists(self.path):
            return 0
        with open(self.path, encoding="utf-8") as f:
            return sum(1 for l in f if l.strip())


def build_prompt(agent_name, role, task, memory, persona=None):
    """Real context: role + persona + last memory turns + task."""
    parts = [f"You are {agent_name}, the {role} agent in a crew.",
             "Speak plainly. Be concise. If you write code, output it inside ```java ... ``` blocks."]
    if persona:
        parts.append(f"Persona: {persona}")
    mem = memory.recent()
    if mem:
        parts.append("Recent crew memory:")
        for m in mem:
            parts.append(f"[{m['role']}]: {m['text'][:300]}")
    parts.append(f"[task]: {task}")
    return "\n".join(parts)


def extract_java(text):
    """Pull the first ```java ... ``` block; return None if absent."""
    if "```java" not in text:
        return None
    start = text.index("```java") + len("```java")
    end = text.find("```", start)
    if end == -1:
        return None
    return text[start:end].strip()
