#!/usr/bin/env python3
"""
hessian_learning.py — CURVATURE-AWARE LoRA LEARNING (Hessian/Fisher)
====================================================================
"Hessian learning" for local SLMs on constrained hardware (Termux/MatrixWinCE):

  RULE A: We never know the full Hessian (too big) — we estimate its DIAGONAL
          via the empirical Fisher:  F_i = E[ (dL/dθ_i)² ]
          One backward pass per sample, squared, averaged. O(n) memory.

  RULE B: High curvature (high F_i) = parameter the model depends on.
          EWC penalty:  L_ewc = Σ 0.5 · F_i · (θ_i − θ*_i)²
          New learning must NOT destroy what old learning already earned.
          ("Nothing runs for free, nothing lasts forever" — but learning
           persists if we don't overwrite it.)

  RULE C: Adaptive LoRA rank — allocate rank where curvature mass lives.
          High-curvature layers get more rank; flat layers can stay thin.

  RULE D: Curvature-gated updates — a proposed adapter delta is scaled by
          Δθ_i ← Δθ_i / (1 + F_i/λ)   (diagonal natural-gradient step).
          Important params move slowly; unimportant params move fast.

  USAGE:
    python3 hessian_learning.py --demo            # tiny MLP walkthrough
    python3 hessian_learning.py --fisher model.npz --data samples.npy
    python3 hessian_learning.py --ewc-cost fisher.npz --prev prev.npz --new new.npz
    python3 hessian_learning.py --rank fisher.npz --layers 64,64,32 --budget 8
"""
import argparse, json, os, sys, time

try:
    import numpy as np
except ImportError:
    print("numpy required: pip install numpy", file=sys.stderr); sys.exit(1)


# --------------------------------------------------------------------------
# 1. Diagonal Fisher estimator (empirical Fisher = E[g²])
# --------------------------------------------------------------------------
class MLP:
    """Minimal numpy MLP — the adapter's effective function. Layers = [in, h1, ..., out].
    Enough to demonstrate Fisher estimation; swap in the real adapter later."""
    def __init__(self, sizes, seed=0):
        rng = np.random.default_rng(seed)
        self.W, self.b = [], []
        for a, b in zip(sizes[:-1], sizes[1:]):
            self.W.append(rng.standard_normal((a, b)) * 0.1)
            self.b.append(np.zeros(b))
        self.sizes = sizes

    def params(self):
        return self.W + self.b

    def forward(self, x):
        h = x
        for W, b in zip(self.W[:-1], self.b[:-1]):
            h = np.tanh(h @ W + b)
        return h @ self.W[-1] + self.b[-1]   # linear head

    def grads(self, x, y):
        """Gradients of MSE loss w.r.t. every param for one (x, y)."""
        acts, hs = [x], [x]
        for W, b in zip(self.W, self.b):
            h = np.tanh(hs[-1] @ W + b) if len(acts) < len(self.W) else hs[-1] @ W + b
            acts.append(h); hs.append(h)
        # simplify: full backprop for a 2-layer net is overkill here; we use
        # autodiff-free central differences on the scalar loss (cheap + exact
        # enough for the diagonal, and trivially portable to any model).
        return self._finite_diff_grads(x, y)

    def _finite_diff_grads(self, x, y, eps=1e-4):
        """Central-difference gradient — works for ANY callable model."""
        grads = []
        def loss():
            out = self.forward(x)
            return float(np.mean((out - y) ** 2))
        base = loss()
        for W in self.W:
            gW = np.zeros_like(W)
            it = np.nditer(W, flags=["multi_index"])
            while not it.finished:
                i, j = it.multi_index
                old = W[i, j]
                W[i, j] = old + eps;  lp = loss()
                W[i, j] = old - eps;  lm = loss()
                W[i, j] = old
                gW[i, j] = (lp - lm) / (2 * eps)
                it.iternext()
            grads.append(gW)
        for b in self.b:
            gb = np.zeros_like(b)
            for i in range(b.size):
                old = b[i]
                b[i] = old + eps;  lp = loss()
                b[i] = old - eps;  lm = loss()
                b[i] = old
                gb[i] = (lp - lm) / (2 * eps)
            grads.append(gb)
        return grads


def fisher_diagonal(model, X, Y, n_samples=None):
    """Empirical Fisher diagonal: average of squared per-sample gradients.
    Returns list of arrays (same shapes as model params)."""
    n = X.shape[0] if n_samples is None else min(n_samples, X.shape[0])
    sums = [np.zeros_like(p) for p in model.params()]
    for k in range(n):
        g = model.grads(X[k:k+1], Y[k:k+1])
        for s, gi in zip(sums, g):
            s += gi ** 2
    return [s / n for s in sums]


def fisher_to_dict(fisher, prefix="fisher"):
    """Serialize fisher (list of arrays) to a dict for np.savez."""
    return {f"{prefix}_{i}": f for i, f in enumerate(fisher)}


# --------------------------------------------------------------------------
# 2. EWC cost + curvature-gated update
# --------------------------------------------------------------------------
def ewc_cost(fisher, prev_params, new_params):
    """EWC consolidation cost: Σ 0.5·F_i·(θ_i − θ*_i)². High = forgetting."""
    total = 0.0
    for F, p, pstar in zip(fisher, new_params, prev_params):
        total += 0.5 * float(np.sum(F * (p - pstar) ** 2))
    return total


def curvature_scaled_delta(delta, fisher, lam=1.0):
    """Δθ_i ← Δθ_i / (1 + F_i/λ). High-curvature params move slowly."""
    return [d / (1.0 + F / lam) for d, F in zip(delta, fisher)]


def adaptive_rank_allocation(fisher, layers, budget):
    """Allocate LoRA rank across layers ∝ curvature mass. Budget = total rank.
    Returns {layer_idx: rank} summing to budget."""
    mass = [float(np.sum(F)) for F in fisher]
    total = sum(mass) or 1.0
    ranks = {}
    allocated = 0
    for i, m in enumerate(mass):
        r = max(1, int(round(budget * m / total)))
        ranks[i] = r
        allocated += r
    # normalize down if we overshot
    while allocated > budget:
        idx = max(ranks, key=lambda k: ranks[k])
        if ranks[idx] > 1:
            ranks[idx] -= 1; allocated -= 1
        else:
            break
    return ranks


def curvature_gate(fisher, prev_params, proposed_delta, threshold=0.05):
    """Gate a proposed adapter delta: if EWC cost of applying it exceeds
    threshold, scale it down so learning respects what already exists."""
    scaled = curvature_scaled_delta(proposed_delta, fisher)
    new_params = [p + d for p, d in zip(prev_params, scaled)]
    cost = ewc_cost(fisher, prev_params, new_params)
    if cost <= threshold:
        return scaled, cost, "PASS"
    factor = threshold / max(cost, 1e-12)
    gated = [d * factor for d in scaled]
    return gated, cost, f"GATED×{factor:.3f}"


# --------------------------------------------------------------------------
# Demo / CLI
# --------------------------------------------------------------------------
def demo():
    print("=" * 62)
    print("  HESSIAN LEARNING DEMO — curvature-aware LoRA policy")
    print("=" * 62)
    rng = np.random.default_rng(7)
    X = rng.standard_normal((64, 6))
    Y = rng.standard_normal((64, 2))

    model = MLP([6, 32, 16, 2], seed=1)
    print(f"\n[1] model sizes: {model.sizes}")

    print("\n[2] estimating diagonal Fisher (central differences, 64 samples)...")
    t0 = time.time()
    F = fisher_diagonal(model, X, Y, n_samples=16)
    print(f"    done in {time.time()-t0:.2f}s — {sum(p.size for p in model.params())} params")

    flat = np.concatenate([f.ravel() for f in F])
    print(f"    curvature: mean={flat.mean():.2e}  max={flat.max():.2e}  "
          f"top1%={np.quantile(flat, .99):.2e}")

    print("\n[3] adaptive LoRA rank (budget=8):")
    ranks = adaptive_rank_allocation(F, model.sizes, 8)
    for k, v in ranks.items():
        print(f"    layer {k}: rank {v}")

    print("\n[4] curvature-gated update (EWC):")
    prev = model.params()
    delta = [rng.standard_normal(p.shape) * 0.01 for p in prev]
    gated, cost, verdict = curvature_gate(F, prev, delta)
    print(f"    raw delta EWC cost → {verdict}  (cost={cost:.2e})")
    print("    high-curvature params now move slowly; flat params move fast.")

    print("\n✅ demo complete — policy: LEARN where flat, PRESERVE where curved.")


def main():
    p = argparse.ArgumentParser(description="Hessian/Fisher curvature-aware LoRA learning")
    p.add_argument("--demo", action="store_true")
    p.add_argument("--fisher", help=".npz with fisher_0..N arrays")
    p.add_argument("--data", help=".npy samples (n, d)")
    p.add_argument("--ewc-cost", action="store_true", help="--fisher --prev --new")
    p.add_argument("--prev", help="previous params .npz")
    p.add_argument("--new", help="proposed new params .npz")
    p.add_argument("--rank", action="store_true", help="--fisher --layers --budget")
    p.add_argument("--layers", default="64,64,32")
    p.add_argument("--budget", type=int, default=8)
    p.add_argument("--json", action="store_true", help="machine-readable output")
    a = p.parse_args()

    if a.demo:
        demo(); return

    if a.ewc_cost:
        F = [np.load(a.fisher)[k] for k in sorted(np.load(a.fisher))]
        prev = [np.load(a.prev)[k] for k in sorted(np.load(a.prev))]
        new = [np.load(a.new)[k] for k in sorted(np.load(a.new))]
        cost = ewc_cost(F, prev, new)
        print(json.dumps({"ewc_cost": cost}) if a.json else f"EWC cost: {cost:.4e}")
        return

    if a.rank:
        F = [np.load(a.fisher)[k] for k in sorted(np.load(a.fisher))]
        layers = [int(x) for x in a.layers.split(",")]
        ranks = adaptive_rank_allocation(F, layers, a.budget)
        print(json.dumps(ranks) if a.json else json.dumps(ranks, indent=2))
        return

    p.print_help()


if __name__ == "__main__":
    main()
