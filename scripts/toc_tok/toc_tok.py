#!/usr/bin/env python3
"""
toc_tok.py — TOC-TOK Tree (Table of Contents → Tree of Knowledge)

A hex-anchored knowledge tree for the SIMS1337 fleet. Projects, phases,
tasks and knowledge live as NODES in a tree; every node is anchored to a
hex coordinate so models can navigate by TREE PATH *and* by SPACE (hex FOW).

    root (0,0)
    └── projects/
        ├── SIMS1337 (2,0)
        │   ├── phases/phase4-agents (2,1)
        │   └── knowledge/markov-voting (2,2)
        ├── api-orchestrator (-2,1)
        └── MatrixWinCE (3,0)

Why: a model spawns at hex (q,r) → FOWGate tells it what it can SEE →
TOC-TOK tells it WHAT LIVES THERE. Tree + map = orientation. That is the
"seamless onboarding" — every SLM knows where it is and what's around it.

Usage:
  python3 toc_tok.py init                  # create empty tree
  python3 toc_tok.py add /projects/SIMS1337 --hex 2,0 --type project
  python3 toc_tok.py add /projects/SIMS1337/phases/phase4 --hex 2,1 --type phase
  python3 toc_tok.py tree                  # print whole tree
  python3 toc_tok.py at 2,1                # what lives at/near a hex
  python3 toc_tok.py search markov         # find nodes by keyword
  python3 toc_tok.py path /projects/SIMS1337/phases/phase4  # full subtree
"""
import argparse, json, os, sys, time

DEFAULT_FILE = os.environ.get("TOC_TOK_FILE", os.path.join(os.path.dirname(__file__), "toc_tok.json"))

def load(path=DEFAULT_FILE):
    if os.path.exists(path):
        return json.load(open(path))
    return {"root": {"path": "/", "title": "root", "type": "root", "hex": "0,0",
                     "tags": [], "content": "", "updated": time.time(), "children": {}}}

def save(tree, path=DEFAULT_FILE):
    """Persist tree, then auto-sync to SOV KV (gist/KV updates from nodes)."""
    json.dump(tree, open(path, "w"), indent=2)
    _auto_sync(path)

def _auto_sync(path):
    """Hook: after any tree mutation, upsert toc.* keys into SOV KV.
    Never raises — sync is best-effort; the tree write always succeeds.
    Portability: locates node_kv_sync.py by repo-relative search, so it
    works identically on the sandbox, MatrixWinCE, and Termux."""
    try:
        kv_file = os.environ.get("SOV_KV_FILE", "/root/sov/kv/data.json")
        if not os.path.exists(kv_file):
            return  # no SOV store here (e.g. sandbox) — skip silently
        here = os.path.dirname(os.path.abspath(__file__))
        # walk up from toc_tok/ to find any repo root containing sync/node_kv_sync.py
        d = here
        found = None
        while os.path.dirname(d) != d:  # up to filesystem root
            for cand in (os.path.join(d, "sync"), os.path.join(d, "MatrixWinCE", "sync"),
                         os.path.join(d, "scripts", "sync")):
                if os.path.isfile(os.path.join(cand, "node_kv_sync.py")):
                    found = cand
                    break
            if found:
                break
            d = os.path.dirname(d)
        if not found:
            return  # no sync module anywhere up the tree — skip silently
        sys.path.insert(0, found)
        from node_kv_sync import sync as _sync
        _sync(path, kv_file)
    except Exception:
        pass  # best-effort

def _mkdir(tree, parts, meta):
    """Create intermediate nodes along a path; returns (node, created)."""
    node = tree["root"]
    created = []
    for i, part in enumerate(parts[:-1]):
        if part not in node["children"]:
            child = {"path": "/" + "/".join(parts[:i+1]), "title": part,
                     "type": "folder", "hex": meta.get("hex", "0,0"),
                     "tags": [], "content": "", "updated": time.time(), "children": {}}
            node["children"][part] = child
            created.append(child)
        node = node["children"][part]
    return node, created

def add_node(tree, path, meta):
    parts = [p for p in path.split("/") if p]
    parent, created = _mkdir(tree, parts, meta)
    name = parts[-1]
    if name in parent["children"]:
        node = parent["children"][name]
        node.update({k: v for k, v in meta.items() if v is not None})
        node["updated"] = time.time()
        return node, False
    node = {"path": path, "title": meta.get("title") or name, "type": meta.get("type", "task"),
            "hex": meta.get("hex", "0,0"), "tags": meta.get("tags", []),
            "content": meta.get("content", ""), "updated": time.time(), "children": {}}
    parent["children"][name] = node
    return node, True

def find_by_hex(tree, target_hex):
    """Return nodes anchored at or within 1-hop of a hex."""
    def dist(a, b):
        aq, ar = map(int, a.split(",")); bq, br = map(int, b.split(","))
        return max(abs(aq-bq), abs(ar-br), abs(aq+ar-bq-br))
    out = []
    def walk(node):
        if node.get("hex"):
            d = dist(node["hex"], target_hex)
            if d == 0: out.append((0, node))
            elif d == 1: out.append((1, node))
        for c in node.get("children", {}).values(): walk(c)
    walk(tree["root"])
    out.sort(key=lambda x: x[0])
    return out

def search(tree, query):
    q = query.lower()
    out = []
    def walk(node):
        blob = (str(node.get("title") or "") + " " + " ".join(node.get("tags") or [])
                + " " + str(node.get("content") or "") + " " + str(node.get("path") or "")).lower()
        if q in blob: out.append(node)
        for c in node.get("children", {}).values(): walk(c)
    walk(tree["root"])
    return out

def get_path(tree, path):
    parts = [p for p in path.split("/") if p]
    node = tree["root"]
    for p in parts:
        if p not in node.get("children", {}): return None
        node = node["children"][p]
    return node

def subtree(node, depth=0, buf=None):
    if buf is None: buf = []
    buf.append("  " * depth + f"[{node.get('type','?')}] {node.get('path','/')} @ {node.get('hex','0,0')}"
               + (f" — {node.get('title')}" if node.get('title') != node.get('path','').split('/')[-1] else ""))
    for c in node.get("children", {}).values():
        subtree(c, depth + 1, buf)
    return buf

# --------------------------------------------------------------------------
def cmd_init(a): save(load(a.file)); print(f"tree initialized at {a.file}")
def cmd_add(a):
    t = load(a.file)
    meta = {"title": a.title, "type": a.type, "hex": a.hex,
            "tags": a.tags.split(",") if a.tags else [], "content": a.content}
    node, created = add_node(t, a.path, meta)
    save(t, a.file)
    print(("created " if created else "updated ") + node["path"] + " @ " + node["hex"])
def cmd_tree(a):
    t = load(a.file)
    print("\n".join(subtree(t["root"])))
def cmd_at(a):
    t = load(a.file)
    hits = find_by_hex(t, a.hex)
    if not hits: print(f"nothing at/near {a.hex}"); return
    for dist, n in hits:
        mark = "◉" if dist == 0 else "○"
        print(f"{mark} {n['path']} [{n['type']}] @ {n['hex']}")
def cmd_search(a):
    t = load(a.file)
    for n in search(t, a.query):
        print(f"  {n['path']} [{n['type']}] @ {n['hex']}")
def cmd_path(a):
    t = load(a.file)
    node = get_path(t, a.path)
    if not node: print(f"no node at {a.path}"); return
    print("\n".join(subtree(node)))

import re as _re
_HEX_TOK = _re.compile(r"^-?\d+,-?\d+$")

_VALUE_OPTS = {"--title", "--type", "--hex", "--tags", "--content", "--file"}

def _protect_hex(argv):
    """Prefix hex-coordinate tokens (e.g. -2,1) with '--' so argparse treats
    them as values, not flags. If the hex is already a VALUE of an option
    (e.g. --hex 4,4), pass it through untouched — no '--' separator."""
    out = []
    i = 0
    prev = None  # previous token (could be an option expecting a value)
    while i < len(argv):
        a = argv[i]
        if a == "--":
            out.extend(argv[i:]); break
        if _HEX_TOK.match(a) and prev not in _VALUE_OPTS:
            # standalone hex (positional like 'at 2,1' or negative flag) → protect
            out.append("--")
            out.append(a)
            i += 1
            continue
        out.append(a)
        prev = a
        i += 1
    return out

def main():
    p = argparse.ArgumentParser(description="TOC-TOK hex-anchored knowledge tree")
    p.add_argument("--file", default=DEFAULT_FILE)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("init").set_defaults(fn=cmd_init)
    a = sub.add_parser("add"); a.add_argument("path"); a.add_argument("--title"); a.add_argument("--type", default="task"); a.add_argument("--hex", default="0,0"); a.add_argument("--tags"); a.add_argument("--content"); a.set_defaults(fn=cmd_add)
    # allow negative hex like --hex=-2,1 (argparse treats -2 as a flag otherwise)
    import argparse as _ap
    _ap.ArgumentParser._negative_number_matcher = lambda *a, **k: None
    sub.add_parser("tree").set_defaults(fn=cmd_tree)
    a = sub.add_parser("at"); a.add_argument("hex"); a.set_defaults(fn=cmd_at)
    a = sub.add_parser("search"); a.add_argument("query"); a.set_defaults(fn=cmd_search)
    a = sub.add_parser("path"); a.add_argument("path"); a.set_defaults(fn=cmd_path)
    args = p.parse_args(_protect_hex(sys.argv[1:]))
    args.fn(args)

if __name__ == "__main__":
    main()
