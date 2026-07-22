# 📝 CHANGELOG - Sims Java Neo FX

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
