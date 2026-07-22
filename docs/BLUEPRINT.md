# 🎮 Sims Java Neo FX - Master Blueprint

**Project:** Gamified SLM Agent Training System  
**Vision:** Pure JavaFX GUI wrapping LoRA-adapted SLM agents  
**Timeline:** Multi-day phased build  
**Status:** Phase 0 - Discovery & Planning

---

## 🧠 CORE CONCEPT

```
┌─────────────────────────────────────────────────────────────┐
│                    JavaFX GUI (Pure Java)                   │
│  - No WebUI, no browser, native desktop app                 │
│  - Gamified interface for SLM training                      │
│  - Real-time telemetry visualization                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              LoRA Adapter Manager (Circular)                │
│  - Token trees organized in segments                        │
│  - Circular weight switching (models stay warm)             │
│  - Fast context switching without reload                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│           SLM Agents (Memory-Mapped, Quantized)             │
│  - Qwen models (0.5B, 1.8B, 7B)                             │
│  - Danube models (500M, 1.8B)                               │
│  - mmap'd to disk (low RAM footprint)                       │
│  - Q4_K_M quantization (speed optimized)                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│         Knowledge Graph + KV Store Pyramid                  │
│  - KG nodes for structured knowledge                        │
│  - Scratchpads for working memory                           │
│  - Chain-of-thought tracking                                │
│  - Pyramid database layers (purpose-separated)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 PURPOSE

1. **Train SLM Agents** for automation tasks
2. **Data Syphon** - Complete projects via gamified interaction
3. **Low Resource** - mmap + quantization = minimal RAM/CPU
4. **Fast Switching** - LoRA adapters = instant context changes
5. **Pure Java** - No web dependencies, native desktop

---

## 📁 REPO CONSOLIDATION PLAN

### Current Sims Repos (To Merge)
```
[ ] sims-java-neo-fx/          (just created, empty scaffold)
[ ] Matrix_CEKAI9000/          (existing orchestration code)
[ ] living-ascii-art/          (ASCII rendering components)
[ ] Aegis_Agents/              (Agent logic)
[ ] openrouter_manager/        (Model API management)
[ ] H2OIDE/                    (IDE components)
[ ] Nova/                      (Monitoring/telemetry)
[ ] moe_kai_skill/             (MoE architecture)
```

### Target Structure
```
sims-java-neo-fx/
├── src/main/java/com/aigen/sims/
│   ├── SimulationApp.java         # Main entry
│   ├── gui/                       # Pure JavaFX (no web)
│   │   ├── MainWindow.java
│   │   ├── AgentPanel.java
│   │   ├── TelemetryPanel.java
│   │   └── TrainingPanel.java
│   ├── agents/                    # SLM agent wrappers
│   │   ├── SLMAgent.java
│   │   ├── LoRAAdapter.java
│   │   ├── ModelManager.java
│   │   └── ChatServer.java
│   ├── memory/                    # Memory systems
│   │   ├── KnowledgeGraph.java
│   │   ├── KVStore.java
│   │   ├── Scratchpad.java
│   │   └── ChainOfThought.java
│   ├── models/                    # Model configurations
│   │   ├── QwenModels.java
│   │   ├── DanubeModels.java
│   │   └── QuantizationConfig.java
│   └── telemetry/                 # Monitoring
│       ├── MetricsCollector.java
│       └── PerformanceLogger.java
├── resources/
│   ├── models/                    # Downloaded GGUF files
│   ├── adapters/                  # LoRA adapter weights
│   ├── configs/                   # YAML configs
│   └── assets/                    # GUI assets
├── docs/
│   ├── BLUEPRINT.md              # This file (updated)
│   ├── CHANGELOG.md              # Every change logged
│   ├── SETUP_GUIDE.md            # Installation steps
│   └── API_REFERENCE.md          # Code documentation
├── scripts/
│   ├── download_models.sh        # Auto-download Ollama models
│   ├── quantize_models.py        # Q4_K_M conversion
│   └── train_lora.py             # LoRA adapter training
└── config/
    ├── models.yaml               # Model registry
    ├── adapters.yaml             # LoRA configs
    └── telemetry.yaml            # Monitoring settings
```

---

## 🗓️ PHASED PLAN

### Phase 1: Discovery (Day 1) ✅ TODAY
- [ ] Audit all Sims-related repos
- [ ] Identify reusable components
- [ ] Map dependencies
- [ ] Create consolidated file structure
- [ ] Download Ollama models (Qwen, Danube)
- [ ] Verify JavaFX environment

### Phase 2: Consolidation (Day 2)
- [ ] Merge all Sims repos into one
- [ ] Organize into modular asset folders
- [ ] Remove web UI components (pure Java only)
- [ ] Clean up GitHub structure
- [ ] Initial README + CHANGELOG

### Phase 3: Core Build (Day 3-4)
- [ ] JavaFX GUI skeleton
- [ ] Ollama integration layer
- [ ] LoRA adapter manager
- [ ] Model download + quantization
- [ ] Chat server implementation

### Phase 4: Memory Systems (Day 5-6)
- [ ] Knowledge Graph integration
- [ ] KV Store pyramid
- [ ] Scratchpad implementation
- [ ] Chain-of-thought tracking
- [ ] mmap optimization

### Phase 5: Telemetry + Balance (Day 7)
- [ ] Performance monitoring
- [ ] System overhead optimization
- [ ] Model balancing
- [ ] Chat server telemetry
- [ ] Dashboard visualization

### Phase 6: Testing + Polish (Day 8-9)
- [ ] End-to-end testing
- [ ] Performance benchmarks
- [ ] Documentation complete
- [ ] GitHub cleanup
- [ ] Release v1.0

---

## 📊 OLLAMA MODELS TO DOWNLOAD

### Priority 1 (Essential)
```bash
ollama pull qwen2.5:0.5b      # 398MB, fast, already downloaded ✅
ollama pull qwen2.5:1.8b      # ~1GB, good balance
ollama pull danube3:500m      # ~300MB, smallest
```

### Priority 2 (Recommended)
```bash
ollama pull qwen2.5:7b        # ~4GB, best quality
ollama pull danube3:1.8b      # ~1GB, alternative
ollama pull phi:latest        # Already have ✅
```

### Priority 3 (Optional)
```bash
ollama pull starcoder:3b      # Code generation
ollama pull tinyllama:1.1b    # Simple tasks
```

---

## 🔧 TECHNICAL REQUIREMENTS

### Java Environment
```
Java: 17+ (required for JavaFX)
JavaFX: 17+ SDK
Maven/Gradle: Build tool
```

### Python Environment
```
Python: 3.11+ (for model scripts)
Ollama: Installed ✅
UV: Package manager ✅
```

### Disk Space
```
Models (Q4_K_M):
- qwen2.5:0.5b  = 398MB
- qwen2.5:1.8b  = 1.1GB
- qwen2.5:7b    = 4.2GB
- danube3:500m  = 285MB
- danube3:1.8b  = 1.0GB
Total: ~7GB

LoRA Adapters: ~500MB (estimated)
KG + KV Stores: ~1GB (grows with use)
Total Project: ~10GB
```

### RAM Requirements
```
With mmap + quantization:
- 0.5B model: ~512MB RAM
- 1.8B model: ~2GB RAM
- 7B model: ~4GB RAM

With LoRA switching (models stay warm):
- Base model: Resident in RAM
- LoRA adapters: ~50-100MB each
- Total: ~3-5GB for multi-model setup
```

---

## 🧩 LoRA ADAPTER ARCHITECTURE

### Circular Token Tree Structure
```
        ┌───────────────┐
        │  Base Model   │
        │  (qwen2.5)    │
        └───────┬───────┘
                │
        ┌───────┴───────┐
        │               │
   ┌────▼────┐     ┌────▼────┐
   │ LoRA 1  │     │ LoRA 2  │
   │ Coding  │     │ Writing │
   └────┬────┘     └────┬────┘
        │               │
        └───────┬───────┘
                │
        ┌───────▼───────┐
        │               │
   ┌────▼────┐     ┌────▼────┐
   │ LoRA 3  │     │ LoRA 4  │
   │ Math    │     │ Chat    │
   └─────────┘     └─────────┘

Circular switching: 1→2→3→4→1...
Models stay warm, only weights swap
```

### Adapter Segments
```yaml
adapter: coding_v1
base_model: qwen2.5:1.8b
lora_rank: 16
lora_alpha: 32
target_modules:
  - q_proj
  - v_proj
  - k_proj
  - o_proj
training_data: code_corpus_10k
quantization: Q4_K_M
mmap: true
```

---

## 📝 DOCUMENTATION STANDARDS

### BLUEPRINT.md (This File)
- Updated: Every major decision
- Contains: Architecture, plans, requirements
- Location: `docs/BLUEPRINT.md`

### CHANGELOG.md
- Updated: Every commit
- Format:
```markdown
## [0.1.0] - 2026-07-19
### Added
- Initial project structure
- Ollama integration
- LoRA adapter framework

### Changed
- Merged 8 Sims repos into one

### Fixed
- JavaFX dependency issues
```

### README.md
- Updated: Weekly
- Contains: Quick start, features, examples
- Location: Root of repo

---

## ✅ IMMEDIATE NEXT STEPS (Today)

1. **Audit Existing Repos**
   ```bash
   cd /c/Users/viper/AIGEN_SYS/repos
   ls -la | grep -i sims
   ```

2. **Download Ollama Models**
   ```bash
   ollama pull qwen2.5:1.8b
   ollama pull danube3:500m
   ```

3. **Verify JavaFX**
   ```bash
   java --version
   # If missing: winget install Oracle.JavaRuntimeEnvironment
   ```

4. **Create Phase 1 Report**
   - List all Sims repos found
   - Inventory reusable code
   - Document dependencies
   - Update BLUEPRINT.md

---

## 💭 CLARIFICATIONS NEEDED

**Questions for Architect:**

1. **Repo Priority:** Which Sims repo has the most complete code to use as base?
2. **LoRA Training:** Train adapters ourselves or use pre-trained?
3. **KG Integration:** Use existing `kg_graph.db` or new structure?
4. **Telemetry:** What metrics matter most (latency, throughput, accuracy)?
5. **GUI Features:** Must-have vs nice-to-have for v1.0?

**I'll figure out what I can autonomously.**  
**I'll ask when blocked.**  
**I'll document everything.**

---

*Phase 1 starting now. Blueprint will update as we discover.* 🎮🧠🚀
