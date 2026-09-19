# SIMS1337 — Architecture Blueprint
# v0.18.0 — 2026-08-02
# "Nothing lives forever, nothing runs for free."

## SYSTEM OVERVIEW

SIMS1337 is a multi-agent SLM orchestration grid running on Windows 10.
10 local Ollama models (0.5B–7B) operate in a 61-hex grid with FOW,
voting, debate, dreaming, self-modification, and autonomous evolution.

```
┌─────────────────────────────────────────────────────────────┐
│                    GODHAND APP (JavaFX)                      │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │
│  │ Hex Map │ │ God Chat │ │ Model    │ │ Activity Log   │  │
│  │ (61 hex)│ │ (500 ln) │ │ Pool     │ │ (buffered 2s)  │  │
│  └─────────┘ └──────────┘ └──────────┘ └────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              EMBEDDED HTTP SERVER (:8899)             │   │
│  │  /api/status  /api/hexmap  /api/metrics  /api/maslow │   │
│  │  /api/peers   /api/euler   /api/audio   /api/clock   │   │
│  │  /api/evidence /api/kg/search /api/websearch          │   │
│  │  /api/toctok  /api/desktop /api/pipeline              │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   SCHEDULER POOL                       │   │
│  │  chatScheduler (ScheduledThreadPoolExecutor)          │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐             │   │
│  │  │ Night    │ │ Agent    │ │ Maslow   │  ...30+      │   │
│  │  │ Cycle    │ │ Autonomy │ │ Needs    │  tasks       │   │
│  │  │ 30min    │ │ 5min     │ │ 10min    │             │   │
│  │  └──────────┘ └──────────┘ └──────────┘             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────┐    ┌──────────────┐    ┌──────────────────┐
│   OLLAMA    │    │  AIGEN_SYS   │    │  EXTERNAL APIs   │
│  localhost  │    │  45 repos    │    │  DuckDuckGo      │
│  :11434     │    │  KG graph    │    │  Cloudflare time │
│  10 models  │    │  Cross-repo  │    │  Gist (GitHub)   │
└─────────────┘    └──────────────┘    └──────────────────┘
```

## DATA FLOW

```
User Input → GodHandApp.start()
  ├─ initAllSystems() — 50+ init methods
  ├─ webDashboardInit() — HTTP server on :8899
  ├─ startAllModelLoops() — per-model chat loops
  └─ nightCycleArm() — autonomous 30min cycle

Night Cycle (every 30min):
  Dream → Debate → Vote → Deploy → Email
    │        │       │       │        │
    ▼        ▼       ▼       ▼        ▼
  6 models  8 models 8 models compile  send
  generate  argue   YES/NO   +push   summary

Agent Autonomy (every 5min):
  Alpha(0,0) → Beta(2,-1) → Gamma(-2,1)
    │              │              │
    ▼              ▼              ▼
  Ollama        Ollama        Ollama
  generate      generate      generate
  message       message       message

Maslow Needs (every 10min):
  For each model:
    Find highest-urgency need
    Vote YES/NO via roleBasedVote
    Address (-30) or escalate (+10)
    Decay all needs by 1
```

## COMPONENT MAP

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| UI | GodHandApp.java | JavaFX GUI, 50+ init methods, HTTP server |
| UI | WebDashboard.java | V2 dashboard on :8900 (pending deps) |
| Engine | TocTokTree.java | Hex-anchored knowledge tree |
| Engine | KVStore.java | File-backed key-value store |
| Engine | KnowledgeGraph.java | In-memory entity graph |
| Engine | AStarPathfinder.java | A* pathfinding (20×20 grid) |
| Desktop | DesktopPane.java | Drag-drop agent/task desktop |
| Desktop | Agent.java | Desktop-paradigm agent wrapper |
| Gate | NyxGate.java | Symbolic AST bracket verification |
| Gate | AlgebraicCorrector.java | Bracket balance scanner |
| Scheduler | PipelineScheduler.java | Event-driven mining→deploy→tune→grow |
| Scheduler | EventBus.java | Pub/sub event system |
| Commander | AegisCommander.java | Strategic command layer |
| Commander | PipelineMonitor.java | Pipeline health monitoring |
| Deploy | DeployOrchestrator.java | Gated deploy pipeline |
| Deploy | GateKeeper.java | Proposal approval gates |
| Mining | CodeMinerOrchestrator.java | Cross-repo code mining |
| LoRA | LoRATuner.java | Adapter auto-tuning |
| LoRA | AdapterRegistry.java | LoRA adapter registry |
| Voting | WeightedQuorumVote.java | Weighted quorum voting |
| Voting | HexCoord.java | Hex coordinate math |
| Phase1 | QuorumVoting.java | FOW-gated quorum voting |
| Phase1 | FOWGate.java | Fog of War visibility |
| Bridge | BruteMiner.java | Brute Foundry bridge |

## JVM CONFIGURATION

```
-Dprism.order=sw          # Software rendering (no GPU freeze)
-Dprism.vsync=false        # Disable vsync
-XX:+UseG1GC              # G1 garbage collector
-XX:MaxGCPauseMillis=200   # Max 200ms GC pause
-XX:GCTimeRatio=9          # 90% app time, 10% GC
-XX:+DisableExplicitGC     # Prevent System.gc() calls
-Xms256m -Xmx768m          # 256MB initial, 768MB max heap
```

## RATE LIMITING

- Ollama calls: 1 per 5 minutes (300s gap)
- 12 calls/hour, ~288 calls/day
- All scheduler intervals ≥ 300s
- Tasks staggered by 30s to prevent JavaFX thread flood
- UI updates batched every 2 seconds (bufferedLog)
- GodChat capped at 500 lines
- Overnight data rotates at 10MB
