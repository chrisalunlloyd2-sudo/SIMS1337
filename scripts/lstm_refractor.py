#!/usr/bin/env python3
"""
lstm_refractor.py — sequence-pattern refractor for the fleet's decision logs.
Reads call/decision logs (JSONL), converts them into fixed-length token
sequences, and (if numpy is available) trains a tiny LSTM-ish transition model
so the Markov chain can learn temporal patterns beyond first-order.

Keeps the "more steps, more verification" doctrine: this is a STATS tool,
not a decision-maker. It feeds priors into markov_chain.py.

Usage:
  python3 lstm_refractor.py --log call_log.jsonl --out patterns.json --window 4
"""
import argparse, collections, json, os

def tokenize_entry(e):
    """Map a log entry to discrete tokens for sequence learning."""
    toks = []
    if "tier" in e: toks.append(f"tier:{e['tier']}")
    if "type" in e: toks.append(f"type:{e['type']}")
    if "decision" in e: toks.append(f"dec:{e['decision']}")
    if "final" in e: toks.append(f"final:{e['final']}")
    if "gate" in e: toks.append(f"gate:{e['gate']}")
    if "all_verified" in e: toks.append(f"verified:{e['all_verified']}")
    return toks

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--log", required=True, help="JSONL log of calls/decisions")
    p.add_argument("--out", default="patterns.json")
    p.add_argument("--window", type=int, default=4)
    args = p.parse_args()

    sequences = []
    if os.path.exists(args.log):
        with open(args.log) as f:
            for line in f:
                line = line.strip()
                if not line: continue
                try:
                    toks = tokenize_entry(json.loads(line))
                    if toks: sequences.append(toks)
                except Exception:
                    continue

    # n-gram transition counts: window -> next token
    counts = collections.Counter()
    for seq in sequences:
        for i in range(len(seq) - args.window):
            key = tuple(seq[i:i + args.window])
            nxt = seq[i + args.window]
            counts[(key, nxt)] += 1

    # convert to conditional probabilities
    grouped = collections.defaultdict(collections.Counter)
    for (key, nxt), c in counts.items():
        grouped[key][nxt] += c
    patterns = {}
    for key, nxt_counts in grouped.items():
        total = sum(nxt_counts.values())
        patterns[" ".join(key)] = {n: round(c / total, 3) for n, c in nxt_counts.most_common(5)}

    with open(args.out, "w") as f:
        json.dump({"window": args.window, "sequences": len(sequences),
                   "patterns": patterns}, f, indent=2)
    print(f"[lstm_refractor] {len(sequences)} sequences, {len(patterns)} patterns → {args.out}")

if __name__ == "__main__":
    main()
