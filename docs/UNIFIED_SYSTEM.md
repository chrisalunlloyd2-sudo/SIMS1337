# SIMS1337 — UNIFIED SYSTEM MAP (2026-08-07)
> The one map that connects: real math, interstitial scheduling, the white paper,
> the cortex-series emails (last batch), the 5 phases, Karoo, and every agent.
> "The packages and parts are not at all connected but the organization is there."
> — This doc IS the connection.

## The spine (one orchestration loop)
```
Conductor (swarm_scheduler.py / Orchestrator pattern)
  → clarify intent → route to agent (Scout/Worker/Sentinel/Hive)
  → agent acts (real git-verifiable move) → verify (quality gate)
  → learn (A/B ledger, trust, LoRA) → dream (consolidation, prune)
  → vote (QuorumVoting) → ship (DeployOrchestrator → git)
  → report (webui / relay / email)
```

## Every part, its home, its role
| Part | Home | Role | Connected via |
|------|------|------|---------------|
| **Real math** | hexgame/math_pipeline.py | deterministic math tasks (levels, problem types) | interstitial scheduler probes + webui Math Drill |
| **Interstitial scheduler** | hexgame/swarm_scheduler.py | gap-filler task dispatcher (10-min cadence) | task_pool.json → pool_next.json → hourly pipeline |
| **White paper** | SIMS1337_White_Paper.md (repo root) | the design canon | every doc links back to it |
| **Orchestration layer** | cortex email → Orchestrator pattern | the brainstem (world.step → stabilize → repair → render) | swarm_scheduler + game_daemon |
| **Executive cortex** | cortex email → clarify/intent/genome/personality/behavior lobes | intent routing | INTENTS map in webui (repair→Worker etc.) |
| **Evolution engine** | cortex email → fitness = stability − strain | adaptation | A/B ledger (ab_state.json), trust ledger |
| **Agent comm protocol** | cortex email + relay/ (inbox/outbox.jsonl) | qwen↔aegis bridge | :5000 relay endpoints |
| **Memory consolidation** | cortex email + SOV KV/KG/Logit | long-term memory | heartbeat_consolidated.py |
| **Dream-cycle / foresight** | cortex email + dream rounds (22:00) | pruning + forecasting | nightly dream runner |
| **Role evolution / planning** | cortex email + scripts/agents/ | agent roles + plans | crew_loop.py, agent_messenger.py |
| **Quorum voting** | src/.../phase1/QuorumVoting.java | agent voting (new agents, upgrades) | scripts/agent_voting.py (new) |
| **Mining** | src/.../agents/CodeAgent + Karoo GP | find new code | karoo_gp repo |
| **Deploy** | src/.../deploy/DeployOrchestrator.java | ship to git | backup.sh doctrine |
| **LoRA training** | lora/hessian_learning.py + scripts/train_lora.py | model improvement | nightly pipeline step 4 |
| **TOC-TOK tree** | scripts/toc_tok/ | hex-anchored knowledge tree | onboarding boarding pass |
| **GUI** | GodHandApp.java (JavaFX) + web/hexeract-gui | the visual | Command Deck webui (:5000) |
| **Karoo (revival)** | karoo_gp repo → wrapper → hex/Euler store → graphs | comparative genetic stats + vision | Phase 6 roadmap |

## The math connection (real math + interstitial)
```
math_pipeline.py (problem generator, levels 1-4, verified answers)
  ← swarm_scheduler probes (c2: add partial fractions/trig/quadratics)
  → results → game_log.jsonl → webui Math Drill panel (c6)
  → every answer verified (quality gate), never guessed
```

## The voting connection (new agents + upgrades)
```
QuorumVoting (Java) ← scripts/agent_voting.py (Python bridge, NEW)
  proposal types: NEW_AGENT | REPO_CREATE | MATRIX_UPGRADE
  → votes cast via email protocol (strict Aegis protocol, internal agents only)
  → tally → threshold → approve/reject
  → approve: design ships to git as NEW REPO named after the agent (auto)
  → reject: recorded, no ship (user/Moe can veto)
```

## The backup connection (doctrine)
```
EVERY agent action: backup.sh --snapshot BEFORE → act → backup.sh --push AFTER
  → logs/backup_receipt.jsonl (continuous audit)
  → reports/BACKUP_STATUS.md (drift detector)
  → GitHub main = source of truth. Local = disposable. NEVER delete.
```

## Where things were disconnected (and the fix)
1. **math_pipeline** ran standalone → now probed by scheduler (c2 in pool).
2. **voting/ + mining/ dirs** were empty ghosts (real code is Java in src/) → unified map above; backup.sh --status shows drift.
3. **Karoo GP** went quiet (machine2 worker scheme) → revival plan in ROADMAP_NEXT.md: comparative wrapper → hex/Euler store → PNG graphs → vision loop.
4. **Servers lacked learning telemetry** → pipe_ops spans + metrics; NEXT: real learning telemetry + node throttling (roadmap).
5. **Gitpages** (analytics + voting) lacked live data → vote_data.json + swarm panels pushed (2026-08-07).

## Doctrine (one line each)
- ADD never DELETE. GitHub main is truth. Backup at all times.
- Complex = more small models voting through verified steps (Markov chaining).
- Deterministic core (state machines, hex, symbolic) + LLM for serendipity (quorum-for-fun).
- ONE model cell active per node; serial a→b→c→d; 10s cooldown; verify-before-accept.
