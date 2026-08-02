#!/usr/bin/env python3
"""
agent_messenger.py — REAL inter-agent messaging via the house GGUF server
==========================================================================
No mocks. No simulations. Agents talk through the real GGUF server
(:5000, house format) backed by a real llama.cpp model.

  POST /api/generate {"prompt","max_tokens"} -> {"model","response","done"}

Memory is REAL: every exchange is appended to agent_<name>.jsonl and the
last N turns are loaded back as context, so agents remember what happened.
If the server is down, we RAISE — we never fabricate a reply.
"""
import json, os, sys, time, urllib.request, urllib.error

BASE = os.environ.get("GGUF_BASE", "http://localhost:5000")
MEM_DIR = os.environ.get("AGENT_MEM_DIR",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "memory"))
CTX_TURNS = int(os.environ.get("AGENT_CTX_TURNS", "8"))
TIMEOUT = int(os.environ.get("AGENT_TIMEOUT", "120"))


def healthz():
    try:
        with urllib.request.urlopen(f"{BASE}/healthz", timeout=5) as r:
            return r.status == 200
    except Exception:
        return False


def generate(prompt, max_tokens=64):
    """Real call to the GGUF server. Raises on any failure — never fabricates."""
    body = json.dumps({"prompt": prompt, "max_tokens": max_tokens}).encode()
    req = urllib.request.Request(f"{BASE}/api/generate", data=body,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
            data = json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"GGUF server HTTP {e.code}: {e.read().decode()[:200]}")
    except Exception as e:
        raise RuntimeError(f"GGUF server unreachable at {BASE}: {e}")
    resp = (data.get("response") or "").strip()
    if not resp:
        raise RuntimeError(f"GGUF server returned empty response for: {prompt[:60]}...")
    return resp, data.get("model", "?")


class AgentMemory:
    """Real append-only memory per agent: memory/agent_<name>.jsonl"""

    def __init__(self, name):
        self.name = name
        os.makedirs(MEM_DIR, exist_ok=True)
        self.path = os.path.join(MEM_DIR, f"agent_{name}.jsonl")

    def append(self, role, text, meta=None):
        entry = {"t": time.time(), "agent": self.name, "role": role,
                 "text": text, "meta": meta or {}}
        with open(self.path, "a") as f:
            f.write(json.dumps(entry) + "\n")

    def recent(self, n=CTX_TURNS):
        if not os.path.exists(self.path):
            return []
        with open(self.path) as f:
            lines = [json.loads(l) for l in f if l.strip()]
        return lines[-n:]

    def count(self):
        if not os.path.exists(self.path):
            return 0
        with open(self.path) as f:
            return sum(1 for l in f if l.strip())


def build_prompt(agent_name, role, task, memory):
    """Real context: role + last memory turns + task. No hallucinated history."""
    parts = [f"You are {agent_name}, the {role} agent in a crew.",
             "Speak plainly. If you write code, output it inside ```java ... ``` blocks."]
    for m in memory.recent():
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
