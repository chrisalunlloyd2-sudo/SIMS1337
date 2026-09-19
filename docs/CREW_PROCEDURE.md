# CREW PROCEDURE — The Integrated Learning Doctrine
> v2.0 | 2026-08-01 | local-first, small models, verified steps, curvature-aware learning

Every agent in the crew follows ONE procedure. Orientation, verification,
decision, action, learning — in that order, every time, forever.

```
┌────────────────────────────────────────────────────────────────┐
│ 1. ORIENT   boarding pass (TOC-TOK tree + hex FOW + continuity) │
│ 2. VERIFY   reply BOARD ID — prove context landed               │
│ 3. DECIDE   Markov chain — N models verify each claim           │
│ 4. ACT      single model, warm, budget-checked (relay chain)    │
│ 5. LEARN    outcome → transition matrix (Markov)                │
│ 6. ADAPT    Hessian Fisher → curvature-gated LoRA update        │
│ 7. REPORT   email summary → user (or verbal, via audio loop)    │
└────────────────────────────────────────────────────────────────┘
```

## Step 1 — ORIENT (every spawn)
- `onboard.py` generates a boarding pass: BOARD ID, hex position, FOW 1-hop,
  knowledge tree, anchored tasks, last 3 decisions (continuity).
- Injected by `batch_termux/scripts/onboard_injector.py` into EVERY model call.
- No spawn is amnesiac. No spawn works without knowing where it is.

## Step 2 — VERIFY (before any work)
- Model must reply its BOARD ID (BP-XXXX) or the daemon treats the call as lost.
- `is_verification()` in the injector confirms receipt; work proceeds only after.
- Three verification layers total: onboarding (BP-XXXX), Markov (per-claim),
  outcome (did it prove right later).

## Step 3 — DECIDE (Markov chain, never a single guess)
- `api-orchestrator/full_decision.py`: route → collective facet voting →
  markov_chain.py verified claim chain → gate → outcome learning.
- Chain confidence = product of per-claim verifications. Weak links re-verified.
- Doctrine: complex ≠ bigger model. Complex = MORE small models, verified steps.

## Step 4 — ACT (single warm model, never parallel)
- `matrixwince/scheduler/model_lifecycle.py`: ONE model resident at a time.
- Budget math first: `model_size ≤ total_ram − 800MB reserve` else QUEUED.
- Task completes → closes its model → spawns next → next warms itself.
- Storage-fenced (eMMC/HDD/SSD): weights read from disk, slow-but-stable.

## Step 5 — LEARN (Markov outcome feedback)
- After real outcomes, `nightly_pipeline.sh` step 3: strengthen transitions
  that proved correct, weaken those that failed (SQLite `markov_transitions.db`).
- 30% prior / 70% evidence blend. Same question re-asked converges.

## Step 6 — ADAPT (Hessian/Fisher curvature-aware LoRA)
- `SIMS1337/lora/hessian_learning.py` — the curvature layer:
  - **Fisher diagonal** F_i = E[(∂L/∂θ_i)²] — which params the model depends on.
  - **EWC penalty** ½ΣF_i(θ_i − θ*_i)² — new learning must not destroy old.
  - **Adaptive rank** — LoRA rank allocated ∝ curvature mass per layer.
  - **Curvature-gated delta** Δθ_i ← Δθ_i/(1+F_i/λ) — curved params move slow,
    flat params move fast. "LEARN where flat, PRESERVE where curved."
- `train_lora.py --hessian` emits `lora_hessian_policy-<tag>.json` before
  training: rank allocation + EWC overwrite warning.
- Nightly: `nightly_pipeline.sh` step 4 runs the Fisher demo + logs the policy.

## Step 7 — REPORT
- Email digest (existing 07:00/19:00/22:00 cycle) OR verbal read-out via the
  MatrixWinCE audio loop (see `docs/AUDIO_LOOP.md` in MatrixWinCE).

## The rules that never change
1. **Never delete, only add** — the tree grows; adapters only layer on.
2. **Nothing runs for free** — every action is budget-checked and logged.
3. **Nothing lasts forever** — but EWC means learned knowledge persists until
   deliberately superseded, not accidentally overwritten.
4. **Verify everything** — orientation, claims, outcomes. Trust is earned in steps.
5. **User is the cloud decider** — free APIs only as weak-consensus escalation;
   the user approves anything that changes the system.
