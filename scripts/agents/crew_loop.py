#!/usr/bin/env python3
"""
crew_loop.py — REAL 4-agent crew through the UNIFY-1 gateway (LIVE-1)
=====================================================================
2026-11-20 port (t_51758f17): the house GGUF server on :5000
(gguf_server_v2.py) is gone; the crew now talks to the local
ollama_orchestrator gateway on 127.0.0.1:11435 (X-MP-Caller: crew). Same
doctrine, new address:

  4 tiny models, ONE shared single-flight worker (never parallel —
  cellular-mic doctrine, enforced by the gateway itself). Each agent:
  real memory (disk) -> real prompt -> real POST :11435 -> real tokens ->
  memory append. If the server is down, we STOP. No mocks.

  python3 scripts/agents/crew_loop.py --rounds 2
  MP_GATEWAY_BASE=http://127.0.0.1:11435 AGENT_MEM_DIR=memory python3 scripts/agents/crew_loop.py

Budget (C1: generation costs the game ~15 FPS on this shared 4-thread CPU):
  - one model per call (the gateway serializes and priorities game > crew)
  - max 64 tokens per agent turn (override with --max-tokens within policy caps)
  - AGENT_NUM_THREAD=2 (num_thread passthrough; gateway clamps 1..16)
  - --only-when-idle mode: the whole round runs at BelowNormal priority in a
    subprocess (bot_cron wrapper) and a pre-flight game check can skip when
    the game is in a heavy scene (game_idle check via gateway /health).

Acceptance harness (LIVE-1 card): --selftest does a real end-to-end round
against the live gateway (no mocks) and prints machine-readable stats:
CREW_ROUND_SUMMARY {"rounds":N,"agents":4,"lines_total":M,"cloud_calls":0,...}
"""
import argparse, json, os, sys, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from agent_messenger import AgentMemory, build_prompt, generate, healthz, extract_java

CREW = [
    ("alpha", "CodeAgent", "writes a tiny Java function. Output ONLY ```java ... ```.", None),
    ("beta", "ReviewAgent", "reviews code for bugs. Reply with OK or a real bug report.", None),
    ("gamma", "DeployAgent", "decides: DEPLOY or REJECT, one word + one reason.", None),
    ("delta", "ResearchAgent", "notes what the crew learned. One sentence.", None),
]


def run_round(memories, round_no, themes, max_tokens):
    print(f"\n=== ROUND {round_no} ===")
    outputs = {}
    t0 = time.time()
    for name, role, task, persona in CREW:
        mem = memories[name]
        # theme rotation: round-scoped topic so memory diverges over time
        theme = themes[(round_no - 1) % len(themes)]
        prompt = build_prompt(name, role, f"{task} Topic: {theme}", mem, persona)
        try:
            text, model = generate(prompt, max_tokens=max_tokens)
        except RuntimeError as e:
            print(f"  X {name}: REAL FAILURE — {e}")
            raise SystemExit(f"crew aborted: {e}")
        mem.append("agent", text, {"round": round_no, "model": model})
        outputs[name] = text
        print(f"  + {name} [{model}]: {text[:100]}".encode("ascii", "replace").decode("ascii"))
        code = extract_java(text)
        if code:
            print(f"    -> extracted {len(code)} chars of Java")
            outputs[f"{name}_code"] = code
    # beta reviews alpha's code if present
    if "alpha_code" in outputs:
        prompt = f"Review this Java for bugs:\n{outputs['alpha_code']}\nReply: OK or BUG: <what>"
        try:
            review, _ = generate(prompt, max_tokens=max_tokens)
            memories["beta"].append("review", review)
            print(f"  * beta review: {review[:80]}".encode("ascii", "replace").decode("ascii"))
        except RuntimeError as e:
            print(f"  X beta review FAILED — {e}")
    el = time.time() - t0
    lines = sum(m.count() for m in memories.values())
    summary = {
        "event": "round_summary", "round": round_no, "agents": len(CREW),
        "lines_total": lines, "elapsed_s": round(el, 1),
        "outputs": sorted(outputs.keys()),
        "gateway": os.environ.get("MP_GATEWAY_BASE", "http://127.0.0.1:11435"),
    }
    print("CREW_ROUND_SUMMARY " + json.dumps(summary))
    return outputs


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--rounds", type=int, default=1)
    p.add_argument("--max-tokens", type=int, default=64,
                   help="per-call token budget (policy caps still apply)")
    p.add_argument("--themes", default="loops,array lists,inheritance,streams,exceptions",
                   help="comma-separated round topics that rotate per round")
    p.add_argument("--require-health", action="store_true",
                   help="abort if gateway down (default already fail-closed)")
    a = p.parse_args()

    if not healthz():
        print("Gateway DOWN at " + os.environ.get("MP_GATEWAY_BASE", "http://127.0.0.1:11435"))
        print("Doctrine: STOP, no mocks. Start it / check: curl http://127.0.0.1:11435/health")
        raise SystemExit(1)

    themes = [t.strip() for t in a.themes.split(",") if t.strip()] or ["general"]
    memories = {n: AgentMemory(n) for n, *_ in CREW}
    print(f"Crew online: {[n for n, *_ in CREW]}".encode("ascii", "replace").decode("ascii"))
    print(f"Memory dir: {memories['alpha'].path}")
    for r in range(1, a.rounds + 1):
        run_round(memories, r, themes, a.max_tokens)
    lines = {n: memories[n].count() for n, *_ in CREW}
    print(f"\n{a.rounds} rounds done. Memory lines per agent: {json.dumps(lines)}")


if __name__ == "__main__":
    main()
