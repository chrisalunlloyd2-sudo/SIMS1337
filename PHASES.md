# SIMS1337 — COMPLETION PHASES & STEPS
# All phases chain real local SLM models via Ollama.
# "Nothing lives forever, nothing runs for free."
# "Never delete, only merge and advance."

## PHASE 1: MODEL INFRASTRUCTURE ✅ (DONE)
- [x] 9 models installed: qwen2.5:0.5b, tinyllama:1.1b, llama3.2:1b, deepseek-r1:1.5b, phi:latest, gemma2:2b, phi3:mini, codellama:7b, mistral:7b
- [x] nomic-embed-text pulled (274MB, embeddings/RAG)
- [x] Context windows fixed via API: tinyllama 2K→8K, phi 2K→8K, all others 16K-32K
- [x] mistral:7b wired into voting specialties (all 4 categories)
- [x] Q4_K_M downloader script: scripts/download_gguf.py (9 models, HuggingFace)

## PHASE 2: CORE ENGINE ✅ (DONE)
- [x] GodHandApp JavaFX GUI with dashboard at :8899
- [x] Real Ollama voting (8 models vote YES/NO via API)
- [x] Real Ollama debate (8 models argue FOR/AGAINST)
- [x] Overnight data logging to logs/overnight-data.json
- [x] Night cycle: dream→debate→vote→deploy→email (30min)
- [x] Rate limiter: 5-min gap, 12 calls/hour, ~288/day
- [x] Finite dreaming: 1 round × 6 models
- [x] Guardian disabled (was spawn loop root cause)
- [x] Quorum voting capped at 48 cycles, 30min interval
- [x] Clipboard crash fix: -Dprism.order=sw

## PHASE 3: TERMINAL CLI ✅ (DONE)
- [x] sims bash CLI: 27 one-word commands
- [x] sims.bat Windows CMD wrapper
- [x] sims.ps1 PowerShell version
- [x] Chain-linking: sims chain <m1> <m2> <q>
- [x] All commands hit real Ollama API

## PHASE 4: AGENT AUTONOMY ✅ (DONE)
- [x] 3 agents on hex grid: Alpha(0,0), Beta(2,-1), Gamma(-2,1)
- [x] Agent autonomy round-robin every 5min
- [x] SLM model assignment per agent
- [x] Real inter-agent messaging via Ollama (10min cycle)
- [x] Agent task completion tracking
- [x] Agent memory persistence to gists

## PHASE 5: HEX MAP & FOW ✅ (DONE)
- [x] 61-hex grid, 4D Q/R/Z+time pulse
- [x] FOW 1-hop visibility
- [x] 16 hex TODOs
- [x] Live SVG hex map at /api/hexmap
- [x] Agent movement visualization
- [x] FOW reveal in SVG

## PHASE 6: KNOWLEDGE GRAPH ✅ (DONE)
- [x] Cross-repo KG: 48 repos indexed
- [x] 71 KG nodes
- [x] RAG queries against KG
- [x] nomic-embed-text semantic search at /api/kg/search
- [x] Pre-embedding on startup (background)
- [x] Cosine similarity ranking

## PHASE 7: SELF-MODIFICATION ✅ (DONE)
- [x] Self-modify engine initialized
- [x] Plugin hot-reload
- [x] Auto-commit on changes
- [x] Compile-gate safety (compileGate() method)
- [x] Rollback on failed compile with backup restore
- [x] Diff review before auto-merge

## PHASE 8: GIST SYNC ✅ (DONE)
- [x] 8 gists defined (neuromorphic-lineage, memories-db, etc.)
- [x] GIST_TOKEN from environment variable
- [x] Auto-sync agent memories to gists (10min)
- [x] Pull gist updates on startup

## PHASE 9: NIGHT OWL COLLECTIVE ✅ (DONE)
- [x] 8 personas defined
- [x] 5min reasoning per persona
- [x] Synthesis via deepseek-r1
- [x] Real Ollama reasoning (ollamaOrFallback)
- [x] Collective memory across sessions

## PHASE 10: CODE WIZARD ✅ (DONE)
- [x] 10min auto-review/refactor
- [x] Real code analysis via codellama:7b
- [x] Auto-suggestion generation
- [x] Safe refactor application

## PHASE 11: TOPOLOGIST ✅ (DONE)
- [x] 5min bottleneck detection
- [x] Auto-connect suggestions
- [x] Real topology analysis via deepseek-r1:1.5b
- [x] Topology insight generation

## PHASE 12: DEPLOYMENT & MONITORING ✅ (DONE)
- [x] Nightly deploy of approved proposals
- [x] Email summary (night cycle phase 5)
- [x] Health dashboard with alerts
- [x] Self-healing recovery (60s health checks)
- [x] Auto-restart on crash (without spawn loop)

## PHASE 13: Q4_K_M UPGRADE PATH ✅ (DONE)
- [x] Download Q4_K_M GGUFs from HuggingFace (scripts/download_gguf.py)
- [x] 9 models supported, ~15GB total
- [x] Auto-Modelfile generation
- [x] ollama create integration

## PHASE 14: PRODUCTION HARDENING ✅ (DONE)
- [x] Windows service wrapper (scripts/service.bat)
- [x] Auto-start on boot via schtasks
- [x] Log rotation (overnight data rotates at 10MB)
- [x] GodChat capped at 500 lines
- [x] JVM stability flags (G1GC, DisableExplicitGC, vsync off)
- [x] Crash recovery without manual intervention

## PHASE 15: WEB SEARCH 🔄 (NEW)
- [x] DuckDuckGo API integration at /api/websearch
- [x] No API key required
- [x] Abstract + related topics extraction
- [ ] Agent web search capability (agents can search)
- [ ] Search result caching

## PHASE 16: DISTRIBUTED SCALING 📋 (NEW)
- [ ] Multi-instance coordination
- [ ] Load balancing across Ollama instances
- [ ] Model sharding (split large models across GPUs)
- [ ] Cross-machine agent communication

## PHASE 17: RELIABILITY ENGINEERING 📋 (NEW)
- [ ] Circuit breakers for Ollama API calls
- [ ] Backpressure handling
- [ ] Retry with exponential backoff
- [ ] Dead letter queue for failed operations
- [ ] Health check aggregation

## PHASE 18: OBSERVABILITY 📋 (NEW)
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Metrics dashboard (Prometheus-compatible)
- [ ] Structured logging (JSON format)
- [ ] Alert thresholds on error rate, latency, memory
- [ ] Replayable event logs

## PHASE 19: MODEL LIFECYCLE MANAGEMENT 📋 (NEW)
- [ ] Model versioning (track which model version produced which output)
- [ ] Shadow deployments (test new model alongside production)
- [ ] A/B routing between model versions
- [ ] Latency-aware scheduling (route to fastest available model)
- [ ] Safety scoring for model outputs

## PHASE 20: GOVERNANCE & SECURITY 📋 (NEW)
- [ ] Role-based access control for dashboard
- [ ] Capability isolation (sandboxed execution)
- [ ] Formal verification of generated code
- [ ] Dependency graph constraints
- [ ] Proposal validation pipeline
- [ ] Static analysis of generated tools
- [ ] Versioned manifests with rollback guarantees
- [ ] Artifact signing
- [ ] Immutable logs
- [ ] Secure build pipeline (CI/CD with policy gates)

## PHASE 21: DOCUMENTATION & RUNBOOKS 📋 (NEW)
- [ ] Architecture blueprints (ASCII data flow diagrams)
- [ ] Threat models
- [ ] Operational runbooks
- [ ] SLA definitions
- [ ] Compliance documentation
- [ ] CREW_PROCEDURE.md integration

## PHASE 22: REAL AGENT BUILD (from GitHub) 📋 (NEW)
- [ ] Native llama.cpp build (no prebuilt bionic binary)
- [ ] SmolLM-135M fallback (105MB, verified on device)
- [ ] House GGUF server on :5000
- [ ] Real crew loop (scripts/agents/crew_loop.py)
- [ ] LoRA = the agent (train_lora.py --hessian)
- [ ] Hessian/Fisher curvature-aware learning
- [ ] EWC penalty for knowledge preservation

## PHASE 23: TOC-TOK INTEGRATION 📋 (NEW)
- [ ] Hex-anchored knowledge tree
- [ ] SLM onboarding boarding pass
- [ ] Board ID stamping + verification
- [ ] Drag-drop hex map canvas (editor.html)
- [ ] TocTokTree Java read-side

## PHASE 24: WORLD AS DESKTOP 📋 (NEW)
- [ ] Desktop paradigm agents (Agent.java, Task.java)
- [ ] A* pathfinding (AStarPathfinder.java)
- [ ] KV store (KVStore.java)
- [ ] Knowledge graph engine (KnowledgeGraph.java)
- [ ] Pipeline scheduler integration

## PHASE 25: STABILITY HARDENING 🔄 (IN PROGRESS)
- [x] UI batch buffer (2s flush, prevents JavaFX thread flood)
- [x] GodChat capped at 500 lines
- [x] Overnight data rotation at 10MB
- [x] Task stagger (30s between scheduled tasks)
- [x] JVM G1GC with low pause target
- [ ] Replace remaining Platform.runLater() with bufferedLog()
- [ ] Model warm-up pacing (stagger model loads)
- [ ] Memory pressure monitoring with auto-GC
