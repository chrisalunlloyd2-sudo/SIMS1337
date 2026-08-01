"""Tests for hessian_learning.py — verify the math is real, not vibes."""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np
from hessian_learning import (MLP, fisher_diagonal, ewc_cost,
                              curvature_scaled_delta, adaptive_rank_allocation,
                              curvature_gate)

passed = 0
def check(name, cond):
    global passed
    assert cond, f"FAIL: {name}"
    passed += 1
    print(f"  ✓ {name}")

rng = np.random.default_rng(3)
X = rng.standard_normal((32, 6)); Y = rng.standard_normal((32, 2))

print("== Fisher estimation ==")
m = MLP([6, 24, 2], seed=0)
F = fisher_diagonal(m, X, Y, n_samples=8)
flat = np.concatenate([f.ravel() for f in F])
check("fisher shapes match params", [f.shape for f in F] == [p.shape for p in m.params()])
check("fisher non-negative (squared grads)", float(flat.min()) >= 0)
check("fisher has signal (not all zero)", float(flat.max()) > 0)

print("== EWC cost semantics ==")
prev = m.params()
same = [p.copy() for p in prev]
check("zero movement -> zero cost", ewc_cost(F, prev, same) == 0.0)
big = [p + 5.0 for p in prev]
small = [p + 0.001 for p in prev]
cost_big, cost_small = ewc_cost(F, prev, big), ewc_cost(F, prev, small)
check("bigger movement -> bigger cost", cost_big > cost_small)

print("== Curvature scaling ==")
delta = [np.full(p.shape, 0.1) for p in prev]
scaled = curvature_scaled_delta(delta, F, lam=1.0)
# every scaled element must be <= raw (F >= 0 -> denominator >= 1)
check("scaled <= raw everywhere",
      all(float(s.max()) <= 0.1 + 1e-12 for s in scaled))
# high-curvature params shrink MORE than low-curvature
hi = flat.argmax(); lo = flat.argmin()
# find which layer/idx those belong to
sizes = [p.size for p in prev]; offs = np.cumsum([0] + sizes)
def locate(idx):
    for li in range(len(sizes)):
        if offs[li] <= idx < offs[li+1]:
            return li, idx - offs[li]
li_hi, j_hi = locate(hi); li_lo, j_lo = locate(lo)
scaled_hi = scaled[li_hi].ravel()[j_hi]
scaled_lo = scaled[li_lo].ravel()[j_lo]
check("high-curvature param moves less than low-curvature", scaled_hi < scaled_lo)

print("== Adaptive rank ==")
ranks = adaptive_rank_allocation(F, m.sizes, 8)
check("rank sum == budget", sum(ranks.values()) == 8)
check("all ranks >= 1", all(r >= 1 for r in ranks.values()))

print("== Curvature gate ==")
gated, cost, verdict = curvature_gate(F, prev, delta, threshold=1e-6)
check("gate triggers on tiny threshold", verdict.startswith("GATED"))
gated2, cost2, verdict2 = curvature_gate(F, prev, delta, threshold=1e6)
check("gate passes on huge threshold", verdict2 == "PASS")

print(f"\nALL {passed} TESTS PASSED")
