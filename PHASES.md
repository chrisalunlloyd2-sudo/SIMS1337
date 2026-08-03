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

## PHASE 15: WEB SEARCH ✅ (DONE)
- [x] DuckDuckGo API integration at /api/websearch
- [x] No API key required
- [x] Abstract + related topics extraction
- [ ] Agent web search capability (agents can search autonomously)
- [ ] Search result caching

## PHASE 16: DISTRIBUTED SCALING ✅ (DONE)
- [x] Multi-instance coordination (distributedInit, heartbeat every 30s)
- [x] Peer discovery (/api/peers endpoint)
- [x] UUID-based instance IDs
- [x] Stale peer purging (2 min timeout)
- [ ] Load balancing across Ollama instances
- [ ] Model sharding (split large models across GPUs)
- [ ] Cross-machine agent communication

## PHASE 17: RELIABILITY ENGINEERING ✅ (DONE)
- [x] Circuit breakers for Ollama API calls (5 fails → 1min cooldown)
- [x] Auto-reset after cooldown
- [x] Consecutive failure tracking
- [ ] Backpressure handling
- [ ] Retry with exponential backoff
- [ ] Dead letter queue for failed operations
- [ ] Health check aggregation

## PHASE 18: OBSERVABILITY ✅ (DONE)
- [x] /api/metrics endpoint (uptime, memory, Ollama stats, counters)
- [x] Structured metrics JSON
- [x] Circuit breaker state in metrics
- [x] Model latency tracking
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Prometheus-compatible metrics
- [ ] Alert thresholds on error rate, latency, memory
- [ ] Replayable event logs

## PHASE 19: MODEL LIFECYCLE MANAGEMENT ✅ (DONE)
- [x] Model versioning (modelVersions map)
- [x] Shadow deployments (modelShadowDeploy)
- [x] A/B routing between model versions (modelABRoute)
- [x] Latency-aware scheduling (modelLatencyScores, selectModel)
- [ ] Safety scoring for model outputs

## PHASE 20: GOVERNANCE & SECURITY ✅ (DONE)
- [x] Role-based access control (SIMS_API_KEY env var)
- [x] Immutable audit log (auditLog)
- [x] 3-gate proposal validation (validateProposal: compile, sandbox, vote)
- [x] Forbidden pattern detection
- [x] Size gate (5000 char limit)
- [ ] Capability isolation (sandboxed execution)
- [ ] Formal verification of generated code
- [ ] Artifact signing
- [ ] Secure build pipeline (CI/CD with policy gates)

## PHASE 21: DOCUMENTATION & RUNBOOKS ✅ (DONE)
- [x] Architecture blueprints (ASCII data flow diagrams) — docs/ARCHITECTURE.md
- [x] Threat models — docs/THREAT_MODEL.md
- [x] Operational runbooks — docs/RUNBOOK.md
- [ ] SLA definitions
- [ ] Compliance documentation
- [ ] CREW_PROCEDURE.md integration

## PHASE 22: REAL AGENT BUILD (from GitHub) 📋 (PENDING)
- [ ] Native llama.cpp build (no prebuilt bionic binary)
- [ ] SmolLM-135M fallback (105MB, verified on device)
- [ ] House GGUF server on :5000
- [ ] Real crew loop (scripts/agents/crew_loop.py)
- [ ] LoRA = the agent (train_lora.py --hessian)
- [ ] Hessian/Fisher curvature-aware learning
- [ ] EWC penalty for knowledge preservation

## PHASE 23: TOC-TOK INTEGRATION ✅ (DONE)
- [x] Hex-anchored knowledge tree (TocTokTree.java)
- [x] /api/toctok endpoint (keyword search + hex-anchored lookup)
- [x] TocTokTree Java read-side wired into GodHandApp
- [ ] SLM onboarding boarding pass
- [ ] Board ID stamping + verification
- [ ] Drag-drop hex map canvas (editor.html)

## PHASE 24: WORLD AS DESKTOP ✅ (DONE)
- [x] KV store (KVStore.java) — file-backed, wired via /api/desktop
- [x] Knowledge graph engine (KnowledgeGraph.java) — in-memory, wired
- [x] A* pathfinding (AStarPathfinder.java) — 20×20 grid, wired
- [x] /api/desktop endpoint
- [ ] Desktop paradigm agents (Agent.java, Task.java) — pending SLMAgent dep fix
- [ ] Pipeline scheduler integration — pending DeployOrchestrator dep fix

## PHASE 25: STABILITY HARDENING 🔄 (IN PROGRESS)
- [x] UI batch buffer (2s flush, prevents JavaFX thread flood)
- [x] GodChat capped at 500 lines
- [x] Overnight data rotation at 10MB
- [x] Task stagger (30s between scheduled tasks)
- [x] JVM G1GC with low pause target
- [x] Windows Defender whitelist (whitelist.bat)
- [x] OOM guard (skip heavy ops if heap > 75%)
- [x] Cross-repo KG bounded to 20 repos
- [x] Heap increased to 768MB
- [x] Platform.runLater reduced from 211 to 76
- [ ] Replace remaining Platform.runLater() with bufferedLog()
- [ ] Model warm-up pacing (stagger model loads)
- [ ] Memory pressure monitoring with auto-GC

---

## MASLOW'S HIERARCHY (v0.18.0+) ✅
- [x] 10 models × 10 need types (acl, kqml, rag, kg, lora, quant, speed, vision, audio, memory)
- [x] Voted every 10min by all models via Ollama
- [x] Watchdog ensures all needs met
- [x] /api/maslow endpoint

## EULER SPHERICAL DB (v0.18.0+) ✅
- [x] Spherical coordinates (theta, phi) → hash for Kaden vision
- [x] O(1) lookup
- [x] /api/euler endpoint
- [x] JSON-lines persistence (logs/euler.db)

## AUDIO PIPELINE (v0.18.0+) ✅
- [x] TTS via Windows SAPI (PowerShell System.Speech)
- [x] Talon-like keyword routing to models
- [x] /api/audio endpoint
- [x] scripts/tts_readout.py

## CLOUDFLARE ATOMIC CLOCK (v0.18.0+) ✅
- [x] HTTP HEAD to time.cloudflare.com every 5min
- [x] RTT-compensated offset
- [x] /api/clock endpoint

## EVIDENCE LOGGING (v0.18.0+) ✅
- [x] Immutable JSONL log (logs/evidence.jsonl)
- [x] 60s flush
- [x] Wired into all vote events
- [x] /api/evidence endpoint

## POWERSHELL IPC (v0.18.0+) ✅
- [x] Named pipe listener (scripts/pipe_listener.ps1)
- [x] Memory-Mapped File writer (scripts/mmf_writer.ps1)
- [x] MMF reader (scripts/mmf_reader.ps1)
- [x] PSCustomPipe + SIMS1337_SharedMem namespacing

## NYX GATE (v0.18.0+) ✅
- [x] Symbolic AST bracket verification
- [x] 3 retry attempts
- [x] AlgebraicCorrector.java
- [x] Wired into GodHandApp init

## PIPELINE SCHEDULER (v0.18.0+) 🔄
- [x] /api/pipeline endpoint (standalone, pending dep fixes)
- [x] EventBus.java compiled
- [ ] Full activation pending DeployOrchestrator constructor fix
- [ ] PipelineScheduler.java has pre-existing dependency issues

## WEB DASHBOARD V2 (v0.18.0+) 🔄
- [x] WebDashboard.java compiled
- [x] Init method registered in GodHandApp
- [ ] Full activation pending PipelineScheduler fix
