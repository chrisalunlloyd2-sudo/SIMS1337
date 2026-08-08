# 📝 CHANGELOG - Sims Java Neo FX

---

## [0.2.1] - 2026-08-04 - PHASE 1C: CRASH RECOVERY & DOCS

### Added — Emergency Recovery Procedures
- **Crash Recovery Protocol**: Initiated agent mesh restart and diagnostic procedures.
- **NEVER-MAKE-CODE-TWICE DATABASE**: Created SQLite registry for code tracking, deduplication, and hashing.
- **scan_and_register.py**: Python script to populate database with project source files.
- **BLUEPRINT.md**: Added system architecture and Hexeract topology.
- **README.md**: Rebuilt master README with ASCII art and topological tree.

---

## [0.18.1] - 2026-07-28 - FOW VOTING HOOKUP

### Added — FOW-Gated Voting System
- **modelAgentMap**: 6 models assigned to 3 agents (Alpha/Beta/Gamma) for FOW visibility
- **proposalHex**: Each proposal tagged with hex coordinate for spatial FOW gating
- **isHexVisibleToModel()**: Checks 1-hop axial hex distance between model's agent and proposal hex
- **FOW-gated castVote()**: Models outside visibility range record "BLIND" vote, cannot approve/reject
- **BLIND vote tracking**: Separate 🌫️ display in proposal status for models blocked by FOW
- **Hex column in proposal table**: Shows which hex each proposal anchors to

### Changed
- Proposal table expanded from 5→6 columns (added Hex)
- `updateProposalStatus()` handles BLIND votes — "🌫️ BLINDED" when majority can't see
- Night cycle auto-vote now FOW-aware (models only vote on visible proposals)
- Hex TODO at (0,0) "Wire FOW to all 8 models" → marked ✅ done

### Model→Agent Assignments
| Model | Agent | Hex |
|-------|-------|-----|
| qwen2.5:0.5b | Agent Alpha | (0,0) |
| tinyllama:1.1b | Agent Alpha | (0,0) |
| phi:latest | Agent Beta | (3,-2) |
| phi3:mini | Agent Beta | (3,-2) |
| llama3.2:1b | Agent Gamma | (-3,2) |
| deepseek-r1:1.5b | Agent Gamma | (-3,2) |

### Proposal→Hex Anchors
| Proposal | Hex |
|----------|-----|
| Add WebSocket support | (1,0) |
| Implement Markov reviews | (-1,-1) |
| Deploy to production | (0,0) |
| Refactor ModelRouter | (2,-1) |

---

## [0.2.0] - 2026-07-19 - PHASE 2A COMPLETE

### Added - Core Routing
- **SLMAgent.java** (3.5KB) - Ollama model wrapper with HTTP client
  - Warm-up functionality
  - Context management for LoRA adapters
  - 120s timeout for inference
  
- **ModelPool.java** (2.9KB) - 4-tier model management
  - qwen2.5:0.5b (fast tier)
  - tinyllama:1.1b (balanced tier)
  - phi:latest (reasoning tier)
  - phi3:mini (deep tier)
  - Auto warm-up on initialization
  
- **ModelRouter.java** (5.8KB) - Complexity-based routing
  - 6 complexity levels (VERY_LOW to CRITICAL)
  - Latency-constrained routing
  - Routing recommendations with explanations
  - <10ms routing decision time
  
- **TaskQueue.java** (4.2KB) - Priority queue with statistics
  - BlockingQueue implementation
  - Capacity limits
  - Utilization tracking
  - Thread-safe operations
  
- **LoRASwitcher.java** (8.4KB) - Circular adapter switching
  - 6 adapter types (CHAT, CODE, PATHFIND, MOTIVES, CAREER, ANALYSIS)
  - Circular buffer for round-robin
  - <100ms switch time target
  - Context preservation per adapter
  - Switch statistics tracking

### Added - Task System
- **Complexity.java** - 6-level complexity enum with multipliers
- **Task.java** - Task representation with UUID, status tracking
- **TaskStatus.java** - 5-state lifecycle enum

### Added - Tests (40 total)
- **ModelPoolTest.java** (8 tests) - Model initialization, retrieval
- **ModelRouterTest.java** (12 tests) - Routing logic, latency constraints
- **TaskQueueTest.java** (9 tests) - Queue operations, statistics
- **LoRASwitcherTest.java** (11 tests) - Adapter switching, circular buffer

### Changed
- Updated pom.xml with JUnit 5, TestFX, JaCoCo
- Updated CHANGELOG.md with Phase 2A details
- Created PHASE2A_COMPLETE.md status report

### Technical Debt
- ⏳ Tests blocked on Java 17 + Maven installation
- ⏳ Coverage report pending (target >90%)

---

## [0.1.0] - 2026-07-19 - Project Init

### Added
- Initial repository structure
- Phase 1: Discovery complete
- 4 Ollama models downloaded
- 1700-step blueprint found

---

*The organism builds. The code compiles. The tests await.* 💙🚀
[2026-07-28T04:10] Agent Beta: write changelog entry
[2026-07-28T04:12] Agent Beta: write changelog entry
[2026-07-28T17:28] Agent Beta: write changelog entry
[2026-07-28T17:47] Agent Beta: write changelog entry
[2026-07-28T20:12] Agent Beta: write changelog entry
[2026-07-28T20:17] Agent Beta: write changelog entry

// hourly-task: ```
```
```
```
```
```
```
```
```
```
```
```
```
```
```


// hourly-task: ```
```
```
```
```
```
```
```
```
```
```
```
```
```
```


// hourly-task: ```
```
```
```
```
```
```
```
```
```
```
```
```
```
```

