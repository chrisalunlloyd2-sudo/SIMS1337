# SIMS1337 — Operations Runbook
# v0.18.0 — 2026-08-02
# "If it breaks, you should have read this first."

## QUICK REFERENCE

| What | Command |
|------|---------|
| Start | `scripts/service.bat start` or launch javaw manually |
| Stop | `taskkill /F /IM javaw.exe` |
| Status | `curl http://localhost:8899/api/status` |
| Health | `curl http://localhost:8899/api/metrics` |
| Logs | `tail -f logs/overnight-data.json` |
| Evidence | `curl http://localhost:8899/api/evidence` |
| Models | `curl http://localhost:8899/api/status \| jq .models` |
| KG | `curl http://localhost:8899/api/kg/search?q=Ollama` |
| Maslow | `curl http://localhost:8899/api/maslow` |
| Hex map | `curl http://localhost:8899/api/hexmap` (SVG) |
| Dashboard | `http://localhost:8899` (browser) |

## STARTUP SEQUENCE

1. **Pre-flight checks**
   ```bash
   # Ollama must be running
   curl http://localhost:11434/api/tags | jq '.models | length'  # should be ≥ 8

   # No stale javaw
   tasklist /FI "IMAGENAME eq javaw.exe"  # should be empty

   # Disk space
   df -h /c/Users/viper/AIGEN_SYS  # at least 2GB free
   ```

2. **Launch**
   ```bash
   cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
   scripts/service.bat start
   ```

3. **Verify**
   ```bash
   sleep 15
   curl http://localhost:8899/api/status
   # Expected: {"version":"0.18.0","models":10,"kgNodes":44,"errors":0,...}
   ```

4. **Init sequence** (automatic, ~30s):
   - Hospital, Brute Foundry, KG, Server Orchestration
   - Self-Exploration, Error Logging, Design, Real RAG
   - Fine-Tuning, Multi-Agent Topology, Web Dashboard
   - Plugin System, Perfect Prompts, Map Guidance
   - Tools System, Persistent Memory, FOW, Hex TODO
   - Gist Context, Gist Sync, Night Cycle, Agent Autonomy
   - FOW Hex Map SVG, Gist→Model Context, Hex TODO Auto-Resolve
   - Email Delivery, Consensus Debate, Night Owl Collective
   - Code Wizard, Topologist, FOW Quorum Voting
   - Code Mining, Deploy Orchestrator, LoRA Auto-Tuning
   - GUI Gardener, AutoLoop, WebSocket Live, Self-Healing
   - Cross-Repo KG, Memory Persistence, Agent Comms
   - Self-Modifying Code, Evolution Engine, World Interface
   - Self-Documentation, Process Guardian, Analytics Engine
   - Plugin Hot-Reload, Maslow Hierarchy, Euler DB
   - Audio Pipeline, Cloudflare Clock, Evidence Logging
   - Distributed Scaling, Circuit Breaker, Metrics
   - Model Lifecycle, Governance, Web Search
   - TocTok Tree, World Desktop, NyxGate, Pipeline Scheduler

## SHUTDOWN

```bash
# Graceful (if GUI is accessible): File → Exit
# Force:
taskkill /F /IM javaw.exe
# Verify:
tasklist /FI "IMAGENAME eq javaw.exe"  # should be empty
```

## HEALTH CHECKS

### Critical (must pass)
```bash
# 1. Process alive
tasklist /FI "IMAGENAME eq javaw.exe" | grep javaw

# 2. Dashboard responding
curl -s --max-time 5 http://localhost:8899/api/status | jq .errors
# Expected: 0

# 3. Ollama reachable
curl -s http://localhost:11434/api/tags | jq '.models | length'
# Expected: ≥ 8
```

### Warning (should pass)
```bash
# 4. Memory under threshold
curl -s http://localhost:8899/api/metrics | jq '.memory.used_mb'
# Expected: < 600 (75% of 768MB)

# 5. Circuit breaker closed
curl -s http://localhost:8899/api/metrics | jq '.breaker'
# Expected: "CLOSED"

# 6. KG nodes present
curl -s http://localhost:8899/api/status | jq .kgNodes
# Expected: ≥ 30
```

### Info (nice to have)
```bash
# 7. Maslow needs populated
curl -s http://localhost:8899/api/maslow | jq '.needs | length'
# Expected: 10

# 8. Evidence log growing
curl -s http://localhost:8899/api/evidence | jq '.entries | length'
# Expected: > 0 after first vote cycle

# 9. Cloudflare time synced
curl -s http://localhost:8899/api/clock | jq .offset_ms
# Expected: < 5000
```

## COMMON ISSUES

### "Ollama not responding"
1. Check: `curl http://localhost:11434/api/tags`
2. Fix: Start Ollama (`ollama serve` or system tray)
3. Circuit breaker will auto-reset after 1 minute

### "javaw.exe using too much memory"
1. Check: `curl http://localhost:8899/api/metrics | jq .memory`
2. If > 600MB: OOM guard should be active
3. Force restart: `taskkill /F /IM javaw.exe && scripts/service.bat start`
4. Logs rotate at 10MB automatically

### "GUI frozen / not responding"
1. Check: `curl http://localhost:8899/api/status` (API may still work)
2. Cause: JavaFX thread flooded — staggered scheduling should prevent this
3. Fix: Wait 30s for bufferedLog flush; if still frozen, restart

### "Models not generating"
1. Check: `curl http://localhost:8899/api/metrics | jq .breaker`
2. If "OPEN": circuit breaker tripped — wait 1 minute for auto-reset
3. Check Ollama: `ollama list`
4. Pull missing models: `ollama pull <model>`

### "KG empty or stale"
1. Check: `curl http://localhost:8899/api/status | jq .kgNodes`
2. Cross-repo scan runs on startup (bounded to 20 repos)
3. Semantic embeddings pre-computed in background
4. Restart to trigger fresh scan

### "Evidence log not growing"
1. Check: `curl http://localhost:8899/api/evidence | jq '.entries | length'`
2. Evidence is logged on votes — wait for next vote cycle (every 10min)
3. Check file: `cat logs/evidence.jsonl | wc -l`

## RECOVERY PROCEDURES

### Full restart
```bash
taskkill /F /IM javaw.exe
sleep 3
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
scripts/service.bat start
sleep 15
curl http://localhost:8899/api/status
```

### Ollama restart
```bash
taskkill /F /IM ollama.exe
sleep 2
ollama serve &
sleep 5
curl http://localhost:11434/api/tags
# Circuit breaker auto-resets after 1 minute
```

### Data recovery
- `logs/overnight-data.json` — persistent state, rotates at 10MB
- `logs/evidence.jsonl` — immutable audit log, append-only
- `logs/euler.db` — spherical vision DB, JSON lines
- `db/kg_graph.db` — SQLite knowledge graph
- All survive process restart

### Emergency rollback
```bash
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx
git log --oneline -5          # find last known good commit
git checkout <commit> -- src/  # restore source
# Recompile and restart
```

## MONITORING

### Cron jobs (via Hermes)
- `Aegis_Every3Hours` — full system health check
- Dashboard at `reports/daily_view.html`

### Manual watch
```bash
# Watch memory every 30s
watch -n 30 'curl -s http://localhost:8899/api/metrics | jq .memory'

# Watch errors
watch -n 60 'curl -s http://localhost:8899/api/status | jq .errors'

# Tail evidence log
tail -f logs/evidence.jsonl
```

## PERFORMANCE TUNING

| Parameter | Default | When to increase |
|-----------|---------|-----------------|
| `-Xmx768m` | 768MB | If OOM guard triggers frequently |
| `MAX_GODCHAT_LINES` | 500 | If chat history truncates too soon |
| `MAX_OVERNIGHT_MB` | 10 | If overnight data rotates too often |
| `CIRCUIT_FAIL_THRESHOLD` | 5 | If Ollama is flaky (increase to 10) |
| `CIRCUIT_RESET_MS` | 60000 | If recovery is too slow (decrease to 30s) |
| `OLLAMA_GAP_MS` | 300000 | If models are underutilized (decrease to 120s) |
