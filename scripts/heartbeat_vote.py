#!/usr/bin/env python3
"""SIMS1337 heartbeat model voting (hour-rotated proposal).

Fixes recurring voter quirks (2026-09-27):
- num_predict raised 40 -> 120 (deepseek-r1:1.5b returned empty at <=40)
- gemma2:2b added as 5th voter / tiebreaker (tinyllama echo-abstains)
- verdict = first standalone APPROVE/REJECT match, echo-stripped

Usage: python scripts/heartbeat_vote.py [proposal_index]
Writes result JSON to scripts/.last_vote.json
"""
import json
import re
import sys
import urllib.request
from datetime import datetime, timezone

OLLAMA = "http://localhost:11434/api/generate"
PROPOSALS = [
    "Hospital admissions diagnostics",
    "Brute Foundry code review",
    "Knowledge Graph node auto-seeding for all backend systems",
    "Server orchestration load balancing",
    "Self-exploration and RAG retrieval",
    "Memory optimization and pruning",
    "Error logging aggregation",
    "Design improvements for the GodHand UI",
]
VOTERS = [
    "qwen2.5:0.5b",
    "tinyllama:1.1b",
    "llama3.2:1b",
    "deepseek-r1:1.5b",
    "gemma2:2b",  # tiebreaker / swap-in for echo/empty abstainers
]


def call(model: str, prompt: str, timeout: int = 75) -> str:
    body = json.dumps({
        "model": model,
        "prompt": prompt,
        "stream": False,
        "options": {"num_predict": 120, "temperature": 0.1},
    }).encode("utf-8")
    req = urllib.request.Request(OLLAMA, data=body,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            data = json.loads(r.read().decode("utf-8", errors="replace"))
        return data.get("response", "").strip()
    except Exception as e:
        return f"__ERROR__ {e}"


def classify(response: str, prompt: str) -> str:
    text = response.strip()
    # strip echoed prompt: find prompt's last 25 chars, cut everything before it
    tail = prompt[-25:].strip()
    idx = text.find(tail)
    if idx >= 0 and idx < 200:
        text = text[idx + len(tail):]
    m = re.search(r"\b(APPROVE|REJECT)\b", text, re.IGNORECASE)
    return m.group(1).upper() if m else "ABSTAIN"


def main() -> None:
    idx = int(sys.argv[1]) if len(sys.argv) > 1 else datetime.now().hour
    proposal = PROPOSALS[idx % len(PROPOSALS)]
    prompt = (
        f"Proposal: {proposal} in the SIMS1337 app. "
        "Vote on this proposal. Reply starting with exactly one word, "
        "either APPROVE or REJECT, then at most 15 words why."
    )
    print(f"Proposal #{idx} ({idx % len(PROPOSALS)}): {proposal}")
    print("-" * 60)
    results = {}
    for model in VOTERS:
        t0 = datetime.now()
        resp = call(model, prompt)
        dt = (datetime.now() - t0).total_seconds()
        verdict = classify(resp, prompt) if not resp.startswith("__ERROR__") else "ABSTAIN"
        snippet = resp[:90].replace("\n", " ")
        results[model] = {"verdict": verdict, "seconds": round(dt, 1), "raw": resp[:300]}
        print(f"{model:20s} {verdict:8s} ({dt:.1f}s)  {snippet}")
    approve = sum(1 for v in results.values() if v["verdict"] == "APPROVE")
    reject = sum(1 for v in results.values() if v["verdict"] == "REJECT")
    passed = approve >= 3
    print("-" * 60)
    print(f"Tally: {approve} APPROVE / {reject} REJECT -> "
          f"{'PASSED' if passed else 'NOT PASSED'} (needed 3+)")
    out = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "proposal_index": idx,
        "proposal": proposal,
        "votes": results,
        "approve": approve,
        "reject": reject,
        "passed": passed,
    }
    with open("scripts/.last_vote.json", "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print("Saved scripts/.last_vote.json")


if __name__ == "__main__":
    main()