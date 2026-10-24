"""logit_trade.py - bots trade logit nudges (word -> small bias) between tailnet nodes. Pure functions; transport is the mesh envelope.

An OFFER is what a node learned works: {"node", "seq", "bias": {term: float}}. A node never adopts an offer as-is:
  - terms are length-limited, plain text only; non-numbers and non-finite values are dropped
  - the offer is scaled by a trust weight (default 0.3) and every merged value is capped (default +/-1.0)
  - at most MAX_TERMS terms are taken per offer; an older seq from the same node is ignored (no replay)
  - merges keep provenance, so any trade can be undone: unmerge(state, node)
  - offers are signed with HMAC-SHA256 using a key the CALLER supplies (never stored or printed here)
  python logit_trade.py selftest
"""
import hashlib, hmac, json, math, re, sys

MAX_TERMS = 200
TERM = re.compile(r"^[A-Za-z0-9'_\- ]{1,32}$")


def make_offer(node, seq, bias):
    clean = {t: round(float(v), 4) for t, v in bias.items() if TERM.match(t) and isinstance(v, (int, float)) and math.isfinite(v)}
    return {"node": node, "seq": int(seq), "bias": dict(list(clean.items())[:MAX_TERMS])}


def sign(offer, key):
    body = json.dumps(offer, sort_keys=True, separators=(",", ":")).encode()
    return hmac.new(key, body, hashlib.sha256).hexdigest()


def verify(offer, sig, key):
    return hmac.compare_digest(sign(offer, key), sig or "")


def new_state():
    return {"bias": {}, "seen": {}, "from": {}}      # from[node][term] = amount that node contributed


def merge(state, offer, trust=0.3, cap=1.0):
    """-> (state, applied_count). Mutates and returns state."""
    node, seq = offer.get("node"), offer.get("seq", -1)
    if not isinstance(node, str) or seq <= state["seen"].get(node, -1):
        return state, 0
    n = 0
    for t, v in list(offer.get("bias", {}).items())[:MAX_TERMS]:
        if not (isinstance(t, str) and TERM.match(t) and isinstance(v, (int, float)) and math.isfinite(v)):
            continue
        cur = state["bias"].get(t, 0.0)
        new = max(-cap, min(cap, cur + trust * v))
        got = new - cur
        if got:
            state["bias"][t] = round(new, 4)
            state["from"].setdefault(node, {})[t] = round(state["from"].get(node, {}).get(t, 0.0) + got, 4)
            n += 1
    state["seen"][node] = seq
    return state, n


def unmerge(state, node):
    """Take back everything a node contributed."""
    for t, amt in state["from"].pop(node, {}).items():
        v = round(state["bias"].get(t, 0.0) - amt, 4)
        if v:
            state["bias"][t] = v
        else:
            state["bias"].pop(t, None)
    state["seen"].pop(node, None)
    return state


def selftest():
    r = []
    def t(n, c):
        r.append(bool(c)); print(("PASS " if c else "FAIL ") + n)
    key = b"test-key-not-a-secret"
    o = make_offer("laptop", 1, {"so": 0.6, "delve": -0.8, "bad;term": 5, "nan": float("nan")})
    t("odd terms and non-finite dropped", set(o["bias"]) == {"so", "delve"})
    sig = sign(o, key)
    t("signature verifies", verify(o, sig, key))
    o2 = json.loads(json.dumps(o)); o2["bias"]["so"] = 0.9
    t("tampered offer fails", not verify(o2, sig, key))
    t("wrong key fails", not verify(o, sig, b"other"))
    s, n = merge(new_state(), o)
    t("trust scales the merge (0.6 x 0.3)", s["bias"]["so"] == 0.18 and n == 2)
    s, n = merge(s, o)
    t("same seq is ignored (no replay)", n == 0 and s["bias"]["so"] == 0.18)
    big = make_offer("hub", 1, {"so": 1.0})
    for i in range(2, 30):
        merge(s, make_offer("hub", i, {"so": 1.0}))
    t("merged values stay capped", s["bias"]["so"] <= 1.0)
    unmerge(s, "hub")
    t("a node's trades can be undone", s["bias"].get("so") == 0.18)
    t("big offers are cut to MAX_TERMS", len(make_offer("x", 1, {"w%d" % i: 0.1 for i in range(500)})["bias"]) == MAX_TERMS)
    print("logit_trade %d/%d passed" % (sum(r), len(r)))
    return 0 if all(r) else 1


if __name__ == "__main__":
    if sys.argv[1:2] == ["selftest"]:
        sys.exit(selftest())
    print(__doc__)
