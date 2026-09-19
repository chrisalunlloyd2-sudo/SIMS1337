# SIMS1337 — Threat Model
# v0.18.0 — 2026-08-02
# "Trust nothing, verify everything."

## ASSETS

| Asset | Value | Exposure |
|-------|-------|----------|
| Ollama models (10) | High — core intelligence | localhost:11434 |
| KG graph (44 nodes) | High — institutional knowledge | in-memory + disk |
| Evidence log | High — immutable audit trail | logs/evidence.jsonl |
| Overnight data | Medium — persistent state | logs/overnight-data.json |
| Gist token | High — GitHub write access | env var GIST_TOKEN |
| API key | High — dashboard auth | env var SIMS_API_KEY |
| Source code | High — self-modifying agent | repo on disk |
| Euler DB | Medium — vision data | logs/euler.db |

## THREAT SURFACE

### 1. Ollama Endpoint (localhost:11434)
- **Threat**: Unauthenticated local API — any process can call models
- **Impact**: Prompt injection, model abuse, token theft
- **Mitigation**: Ollama bound to 127.0.0.1 only; circuit breaker (5 fails → 1min cooldown)
- **Residual**: Local malware could abuse — Windows Defender whitelist reduces attack surface

### 2. HTTP Dashboard (:8899)
- **Threat**: Unauthenticated endpoints expose system state
- **Impact**: Information disclosure (model list, KG nodes, metrics)
- **Mitigation**: SIMS_API_KEY env var for write operations; read-only endpoints are low-risk
- **Residual**: Read endpoints are open — acceptable for local dev; add auth if exposed to network

### 3. Self-Modifying Code
- **Threat**: Agent modifies its own source code
- **Impact**: Code injection, logic corruption, infinite self-modify loops
- **Mitigation**: Compile-gate (javac check before apply), rollback on failure, 3-gate proposal validation
- **Residual**: Sophisticated prompt injection could bypass gates — NyxGate bracket verification adds defense-in-depth

### 4. Gist Token Exposure
- **Threat**: GIST_TOKEN in environment variable
- **Impact**: Unauthorized gist creation/modification
- **Mitigation**: Token scoped to gist only; not logged; env var not exposed via API
- **Residual**: Process memory dump could leak token

### 5. Cross-Repo KG Scan
- **Threat**: Scanning 45 repos for knowledge graph
- **Impact**: OOM, disk I/O flood, sensitive file exposure
- **Mitigation**: Bounded to 20 repos; skips if heap > 75%; OOM guard
- **Residual**: Large repos could still cause memory pressure

### 6. PowerShell IPC (Named Pipes + MMF)
- **Threat**: Shared memory and named pipes for Java↔PowerShell comms
- **Impact**: Process injection, data tampering
- **Mitigation**: Named pipe uses PSCustomPipe (not default); MMF uses SIMS1337_SharedMem (namespaced)
- **Residual**: Any process with same user context can read/write — Windows integrity levels help

### 7. Evidence Log Tampering
- **Threat**: Modification of immutable audit log
- **Impact**: Loss of scientific evidence, audit trail corruption
- **Mitigation**: JSONL append-only; 60s flush; separate from overnight data
- **Residual**: File deletion possible — no cryptographic integrity (future: hash chain)

## RISK MATRIX

| Threat | Likelihood | Impact | Risk |
|--------|-----------|--------|------|
| Ollama prompt injection | Medium | High | HIGH |
| Dashboard info disclosure | High | Low | LOW |
| Self-modify code injection | Low | Critical | MEDIUM |
| Gist token leak | Low | High | LOW |
| KG scan OOM | Medium | Medium | MEDIUM |
| PowerShell IPC tampering | Low | Medium | LOW |
| Evidence log deletion | Low | Medium | LOW |

## DEFENSE LAYERS

```
┌─────────────────────────────────────────┐
│ LAYER 1: Network                         │
│  Ollama → 127.0.0.1 only                 │
│  Dashboard → localhost only              │
│  Cloudflare → outbound HTTPS only        │
├─────────────────────────────────────────┤
│ LAYER 2: Authentication                  │
│  SIMS_API_KEY → write endpoints          │
│  GIST_TOKEN → scoped to gist             │
├─────────────────────────────────────────┤
│ LAYER 3: Application                     │
│  Circuit breaker → 5 fails, 1min cooldown│
│  Compile-gate → javac before apply       │
│  NyxGate → bracket verification          │
│  OOM guard → skip if heap > 75%         │
│  Rate limiter → 1 Ollama call/5min       │
├─────────────────────────────────────────┤
│ LAYER 4: Data                            │
│  Evidence log → append-only JSONL        │
│  Audit log → immutable (RBAC)           │
│  Log rotation → 10MB overnight cap       │
│  GodChat → 500 line cap                  │
├─────────────────────────────────────────┤
│ LAYER 5: System                          │
│  Windows Defender → whitelist script     │
│  JVM flags → G1GC, no explicit GC       │
│  Heap bounds → 256MB–768MB              │
│  Process guardian → singleton guard      │
└─────────────────────────────────────────┘
```

## INCIDENT RESPONSE

1. **Ollama abuse detected** → Circuit breaker opens automatically (5 fails)
2. **OOM** → OOM guard skips heavy ops; heap capped at 768MB
3. **Self-modify corruption** → Compile-gate rejects; rollback to last known good
4. **Evidence log missing** → Recreated on next evidenceLog() call
5. **Process crash** → Service wrapper restarts; overnight data persists
