#!/usr/bin/env python3
"""
crew_loop.py — REAL 4-agent crew through the house GGUF server
================================================================
4 tiny models (one shared server, relayed — never parallel, doctrine).
Each agent: real memory (disk) → real prompt → real POST :5000 →
real tokens → memory append. If the server is down, we STOP. No mocks.

  python3 scripts/agents/crew_loop.py --rounds 2
  GGUF_BASE=http://localhost:5000 AGENT_MEM_DIR=memory python3 scripts/agents/crew_loop.py
"""
import argparse, json, os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from agent_messenger import AgentMemory, build_prompt, generate, healthz, extract_java

CREW = [
    ("alpha",  "CodeAgent",   "writes a tiny Java function. Output ONLY ```java ... ```."),
    ("beta",   "ReviewAgent", "reviews code for bugs. Reply with OK or a real bug report."),
    ("gamma",  "DeployAgent", "decides: DEPLOY or REJECT, one word + one reason."),
    ("delta",  "ResearchAgent", "notes what the crew learned. One sentence."),
]


def run_round(memories, round_no):
    print(f"\n=== ROUND {round_no} ===")
    outputs = {}
    for name, role, task in CREW:
        mem = memories[name]
        prompt = build_prompt(name, role, task, mem)
        try:
            text, model = generate(prompt, max_tokens=96)
        except RuntimeError as e:
            print(f"  ✗ {name}: REAL FAILURE — {e}")
            raise SystemExit(f"crew aborted: {e}")
        mem.append("agent", text, {"round": round_no})
        outputs[name] = text
        print(f"  ✓ {name} [{model}]: {text[:100]}")
        code = extract_java(text)
        if code:
            print(f"    → extracted {len(code)} chars of Java")
            outputs[f"{name}_code"] = code
    # beta reviews alpha's code if present
    if "alpha_code" in outputs:
        prompt = f"Review this Java for bugs:\n{outputs['alpha_code']}\nReply: OK or BUG: <what>"
        try:
            review, _ = generate(prompt, max_tokens=48)
            memories["beta"].append("review", review)
            print(f"  ★ beta review: {review[:80]}")
        except RuntimeError as e:
            print(f"  ✗ beta review FAILED — {e}")
    return outputs


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--rounds", type=int, default=2)
    p.add_argument("--require-health", action="store_true", help="abort if server down")
    a = p.parse_args()

    if not healthz():
        print("GGUF server DOWN at " + os.environ.get("GGUF_BASE", "http://localhost:5000"))
        print("Start it:  python3 gguf_server_v2.py 5000 /path/model.gguf --no-placeholder")
        raise SystemExit(1)

    memories = {n: AgentMemory(n) for n, *_ in CREW}
    print(f"Crew online: {[n for n, *_ in CREW]}")
    print(f"Memory dir: {memories['alpha'].path}")
    for r in range(1, a.rounds + 1):
        run_round(memories, r)
    print(f"\n✅ {a.rounds} rounds done. Memory files: {len(CREW)} agents.")
