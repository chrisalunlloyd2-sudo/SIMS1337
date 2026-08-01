# SIMS1337 — COMPLETION PHASES & STEPS
# All phases chain real local SLM models via Ollama.
# "Nothing lives forever, nothing runs for free."

## PHASE 1: MODEL INFRASTRUCTURE ✅ (DONE)
- [x] 9 models installed: qwen2.5:0.5b, tinyllama:1.1b, llama3.2:1b, deepseek-r1:1.5b, phi:latest, gemma2:2b, phi3:mini, codellama:7b, mistral:7b
- [x] nomic-embed-text pulled (274MB, embeddings/RAG)
- [x] Quantization audit: 3 models Q4_K_M+, 5 models Q4_0 (Ollama registry limitation)
- [x] Context windows fixed via API: tinyllama 2K→8K, phi 2K→8K, all others 16K-32K
- [x] mistral:7b wired into voting specialties (all 4 categories)
- [ ] Q4_K_M upgrade path: download GGUFs from HuggingFace, create via `ollama create`

## PHASE 2: CORE ENGINE ✅ (DONE)
- [x] GodHandApp JavaFX GUI with dashboard at :8899
- [x] Real Ollama voting (8 models vote YES/NO via API)
- [x] Real Ollama debate (8 models argue FOR/AGAINST)
- [x] Overnight data logging to logs/overnight-data.json
- [x] Night cycle: dream→debate→vote→deploy→email (30min)
- [x] Rate limiter: 2s gap, 30s backoff after 5 fails
- [x] Finite dreaming: 10 rounds × 6 models × 2s gap = ~2min
- [x] Guardian disabled (was spawn loop root cause)
- [x] Quorum voting capped at 48 cycles, 30min interval
- [x] Clipboard crash fix: -Dprism.order=sw

## PHASE 3: TERMINAL CLI ✅ (DONE)
- [x] sims bash CLI: 27 one-word commands
- [x] sims.bat Windows CMD wrapper
- [x] sims.ps1 PowerShell version
- [x] Chain-linking: sims chain <m1> <m2> <q>
- [x] All commands hit real Ollama API

## PHASE 4: AGENT AUTONOMY 🔄 (IN PROGRESS)
- [x] 3 agents on hex grid: Alpha(0,0), Beta(2,-1), Gamma(-2,1)
- [x] Agent autonomy round-robin every 90s
- [x] SLM model assignment per agent
- [ ] Real inter-agent messaging via Ollama (currently simulated)
- [ ] Agent task completion tracking
- [ ] Agent memory persistence to gists

## PHASE 5: HEX MAP & FOW 🔄
- [x] 61-hex grid, 4D Q/R/Z+time pulse
- [x] FOW 1-hop visibility
- [x] 16 hex TODOs
- [ ] Live SVG hex map in dashboard
- [ ] Agent movement visualization
- [ ] FOW reveal animation

## PHASE 6: KNOWLEDGE GRAPH 🔄
- [x] Cross-repo KG: 48 repos indexed
- [x] 71 KG nodes
- [x] RAG queries against KG
- [ ] nomic-embed-text integration for semantic search
- [ ] KG visualization in dashboard
- [ ] Auto-edge discovery between repos

## PHASE 7: SELF-MODIFICATION 🔄
- [x] Self-modify engine initialized
- [x] Plugin hot-reload
- [x] Auto-commit on changes
- [ ] Compile-gate safety (compile before deploy)
- [ ] Rollback on failed compile
- [ ] Diff review before auto-merge

## PHASE 8: GIST SYNC 🔄
- [x] 8 gists defined (neuromorphic-lineage, memories-db, etc.)
- [ ] GIST_TOKEN configuration
- [ ] Auto-sync agent memories to gists
- [ ] Pull gist updates on startup

## PHASE 9: NIGHT OWL COLLECTIVE 🔄
- [x] 8 personas defined
- [x] 5min reasoning per persona
- [x] Synthesis via deepseek-r1
- [ ] Real Ollama reasoning (currently simulated)
- [ ] Collective memory across sessions

## PHASE 10: CODE WIZARD 🔄
- [x] 10min auto-review/refactor
- [ ] Real code analysis via codellama
- [ ] Auto-PR generation
- [ ] Test generation from code changes

## PHASE 11: TOPOLOGIST 🔄
- [x] 3min bottleneck detection
- [x] Auto-connect suggestions
- [ ] Real topology analysis via deepseek-r1
- [ ] Visual topology graph in dashboard

## PHASE 12: DEPLOYMENT & MONITORING 🔄
- [x] Nightly deploy of approved proposals
- [x] Email summary (night cycle phase 5)
- [ ] Real email sending (SMTP config)
- [ ] Health dashboard with alerts
- [ ] Auto-restart on crash (without spawn loop)

## PHASE 13: Q4_K_M UPGRADE PATH 📋
- [ ] Download Q4_K_M GGUFs from HuggingFace for codellama, gemma2, phi3, phi, tinyllama
- [ ] Create Modelfiles pointing to GGUF files
- [ ] `ollama create <name>:q4_K_M -f Modelfile`
- [ ] Update SIMS1337 model roster to use Q4_K_M variants
- [ ] Benchmark quality improvement

## PHASE 14: PRODUCTION HARDENING 📋
- [ ] Windows service wrapper (NSSM)
- [ ] Auto-start on boot
- [ ] Log rotation (144MB in 2 days = ~2GB/month)
- [ ] Memory monitoring (was 195MB after 2 days — good)
- [ ] Crash recovery without manual intervention
