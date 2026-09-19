#!/usr/bin/env python3
"""
onboard.py — SLM ONBOARDING BOARDING PASS

When an SLM spawns into the hex FOW matrix, it needs ORIENTATION and
CONTINUITY. This generates the boarding-pass prompt:

    ═══ BOARDING PASS ═══
    AGENT:      Alpha
    MODEL:      qwen2.5:0.5b
    HEX:        (2,1)
    VISIBLE:    (2,1) (3,1) (3,0) (2,0) (1,0) (1,1) (1,2)   [FOW 1-hop]
    ROLE:       phase4-agents
    KNOWLEDGE:  what lives here (TOC-TOK at/near hex)
    TASKS:      tasks anchored here
    CONTINUITY: last decisions/context from chain logs (if any)
    MISSION:    explicit directive

Three pillars of seamless onboarding:
  1. POSITION  — hex + FOW visibility (HexCoord/FOWGate math, ported)
  2. KNOWLEDGE — TOC-TOK tree: what lives here + parent path
  3. CONTINUITY— Markov chain logs + TOC-TOK content: what was decided before

Usage:
  python3 onboard.py --model qwen2.5:0.5b --hex 2,1 --role phase4-agents \
      --mission "Verify phase 4 agents are FOW-aware"
  python3 onboard.py --model tinyllama:1.1b --hex=-2,1 --role governance \
      --mission "Vote on deploy" --continuity chain_decisions.jsonl
"""
import argparse, json, os, sys, time, uuid
from toc_tok import load as toc_load, find_by_hex, get_path, subtree, save as toc_save

DEFAULT_TOC = os.environ.get("TOC_TOK_FILE",
                            os.path.join(os.path.dirname(__file__), "toc_tok.json"))

# --- Hex math (ported from HexCoord.java / hex-fow gist) --------------------
def hex_distance(a, b):
    dq, dr = a[0]-b[0], a[1]-b[1]
    return max(abs(dq), abs(dr), abs(dq+dr))

def one_hop(q, r):
    dirs = [(1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1)]
    return [(q,r)] + [(q+dq, r+dr) for dq, dr in dirs]

# --- Continuity: read last decisions from chain logs -----------------------
def read_continuity(path, limit=5):
    out = []
    if not path or not os.path.exists(path):
        return out
    try:
        with open(path) as f:
            for line in f:
                line = line.strip()
                if not line: continue
                try: out.append(json.loads(line))
                except Exception: continue
    except Exception:
        return out
    return out[-limit:]

# --- Board ID + hex claim + verification (enhanced onboarding) -------------
CLAIM_FILE = os.environ.get("TOC_TOK_CLAIMS",
                            os.path.join(os.path.dirname(__file__), "claims.json"))

def claim_hex(hex_str, agent, model):
    """Record who is occupying a hex (prevents two agents working same cell)."""
    claims = {}
    if os.path.exists(CLAIM_FILE):
        try: claims = json.load(open(CLAIM_FILE))
        except Exception: pass
    claims[hex_str] = {"agent": agent, "model": model, "claimed_at": time.time()}
    json.dump(claims, open(CLAIM_FILE, "w"), indent=2)
    return claims

def release_hex(hex_str):
    claims = {}
    if os.path.exists(CLAIM_FILE):
        try: claims = json.load(open(CLAIM_FILE))
        except Exception: pass
    claims.pop(hex_str, None)
    json.dump(claims, open(CLAIM_FILE, "w"), indent=2)

def verify_pass(board_id):
    """Onboarding verification poll: agent confirms it received the pass."""
    ver = {}
    vf = os.path.join(os.path.dirname(__file__), "onboard_verified.json")
    if os.path.exists(vf):
        try: ver = json.load(open(vf))
        except Exception: pass
    ver[board_id] = {"verified_at": time.time()}
    json.dump(ver, open(vf, "w"), indent=2)
    return board_id

def sync_tree(toc_file):
    """Auto-update the tree's 'last_onboarded' field after onboarding."""
    try:
        tree = toc_load(toc_file)
        tree["root"]["last_onboarded"] = time.time()
        toc_save(tree, toc_file)
        return True
    except Exception:
        return False

# --- Boarding pass generation ----------------------------------------------
def build_pass(model, hex_str, role, mission, toc_file, continuity_file, board_id=None):
    board_id = board_id or f"BP-{uuid.uuid4().hex[:8].upper()}"
    q, r = map(int, hex_str.split(","))
    visible = one_hop(q, r)
    tree = toc_load(toc_file)

    # knowledge: nodes at/near hex
    near = find_by_hex(tree, hex_str)
    at_hex = [n for d, n in near if d == 0]
    hop1 = [n for d, n in near if d == 1]

    # tasks anchored here
    tasks = [n for n in at_hex if n.get("type") == "task"]

    # parent knowledge path (closest project ancestor)
    parent_path = None
    for d, n in near:
        if n.get("type") == "project":
            parent_path = n["path"]; break

    lines = []
    lines.append("═══ SLM ONBOARDING — BOARDING PASS ═══")
    lines.append(f"BOARD ID:   {board_id}")
    lines.append(f"AGENT:      {role}")
    lines.append(f"MODEL:      {model}")
    lines.append(f"HEX:        ({q},{r})")
    lines.append(f"VISIBLE:    " + " ".join(f"({vq},{vr})" for vq, vr in visible) + "   [FOW 1-hop]")
    lines.append(f"ROLE:       {role}")
    lines.append("")

    # knowledge section
    lines.append("KNOWLEDGE (what lives here):")
    if at_hex:
        for n in at_hex:
            lines.append(f"  ◉ {n['path']} [{n.get('type')}] — {n.get('content') or n.get('title')}")
    else:
        lines.append("  ◉ (nothing anchored exactly here)")
    if hop1:
        lines.append("  ○ within 1-hop:")
        for n in hop1[:6]:
            lines.append(f"     ○ {n['path']} [{n.get('type')}]")
    if parent_path:
        lines.append(f"  PROJECT CONTEXT: {parent_path}")
    lines.append("")

    # tasks
    if tasks:
        lines.append("TASKS ANCHORED HERE:")
        for t in tasks:
            lines.append(f"  ▸ {t['path']}: {t.get('content') or t.get('title')}")
    lines.append("")

    # continuity
    cont = read_continuity(continuity_file)
    if cont:
        lines.append("CONTINUITY (recent decisions):")
        for c in cont:
            dec = c.get("final") or c.get("gate") or c.get("decision", "?")
            conf = c.get("chain_confidence") or c.get("confidence") or c.get("overall_consensus") or "?"
            q_ = (c.get("question") or c.get("proposal") or "")[:80]
            lines.append(f"  ▸ {dec} (conf={conf}) — {q_}")
    else:
        lines.append("CONTINUITY: (no prior context — fresh spawn)")
    lines.append("")

    lines.append(f"MISSION: {mission}")
    lines.append("═══ END BOARDING PASS ═══")
    lines.append("")
    lines.append("You are now operational in the hex FOW matrix. Use the boarding")
    lines.append("pass above as your persistent context. Act only within your")
    lines.append("visible hexes. If the task requires knowledge outside your")
    lines.append("visibility, state that clearly instead of guessing.")
    lines.append("")
    lines.append("VERIFICATION: reply with your BOARD ID to confirm receipt.")
    return "\n".join(lines), board_id

def main():
    p = argparse.ArgumentParser(description="Generate an SLM boarding pass")
    p.add_argument("--model", default="qwen2.5:0.5b")
    p.add_argument("--hex", required=True, help="spawn hex, e.g. 2,1 or -2,1")
    p.add_argument("--role", default="agent")
    p.add_argument("--mission", default="Await instructions within your visible hexes.")
    p.add_argument("--toc", default=DEFAULT_TOC)
    p.add_argument("--continuity", default="", help="path to chain_decisions.jsonl etc.")
    p.add_argument("--out", help="write pass to file instead of stdout")
    args = p.parse_args()
    text, board_id = build_pass(args.model, args.hex, args.role, args.mission,
                                args.toc, args.continuity)
    # enhanced onboarding: claim the hex + touch the tree
    claim_hex(args.hex, args.role, args.model)
    sync_tree(args.toc)
    if args.out:
        with open(args.out, "w") as f: f.write(text)
        print(f"boarding pass written to {args.out} (BOARD ID: {board_id})")
    else:
        print(text)
        print(f"[onboard] BOARD ID: {board_id}", file=sys.stderr)

if __name__ == "__main__":
    main()
