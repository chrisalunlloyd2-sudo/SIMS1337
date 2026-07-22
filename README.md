# 🎮 Sims Java Neo FX

**Gamified SLM Agent Training System**

[![Status](https://img.shields.io/badge/status-phase%201%20discovery-blue)](https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx)
[![Java](https://img.shields.io/badge/java-17+-red.svg)](https://openjdk.java.net/)
[![JavaFX](https://img.shields.io/badge/javafx-17+-orange.svg)](https://openjfx.io/)
[![Models](https://img.shields.io/badge/models-qwen%20%7C%20danube-green.svg)](https://ollama.ai)

---

## 🚀 Vision

A **pure JavaFX desktop application** that gamifies the training of Small Language Model (SLM) agents for automation tasks.

**Key Features:**
- 🧠 **LoRA-Adapted SLM Agents** - Circular token trees, fast weight switching
- 💾 **Memory-Mapped Models** - Minimal RAM footprint via mmap + quantization
- 📊 **Knowledge Graph Pyramid** - Structured knowledge + KV stores + scratchpads
- 🎮 **Gamified GUI** - Pure JavaFX, no web UI, native desktop experience
- 📈 **Real-Time Telemetry** - Performance monitoring, system balance optimization

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────┐
│              JavaFX Desktop GUI                 │
│         (Pure Java, No Web Dependencies)        │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│           LoRA Adapter Manager                  │
│    (Circular Token Trees, Warm Models)          │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│         SLM Agents (Ollama Backend)             │
│   Qwen 0.5B/1.8B/7B | Danube 500M/1.8B         │
│      (Quantized Q4_K_M, mmap to disk)           │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│      Knowledge Graph + KV Store Pyramid         │
│   (KG Nodes | Scratchpads | Chain-of-Thought)   │
└─────────────────────────────────────────────────┘
```

---

## 📦 Installation

### Prerequisites

```bash
# Java 17+
winget install Oracle.JavaRuntimeEnvironment

# Ollama (already installed ✅)
# https://ollama.ai

# Python 3.11+ (already installed ✅)
# uv package manager (already installed ✅)
```

### Clone Repository

```bash
git clone https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx.git
cd sims-java-neo-fx
```

### Download Models

```bash
# Essential models
ollama pull qwen2.5:0.5b      # ✅ Already downloaded
ollama pull qwen2.5:1.8b      # ⏳ Downloading
ollama pull danube3:500m      # ⏳ Downloading

# Recommended
ollama pull qwen2.5:7b
ollama pull danube3:1.8b
```

### Build (Coming Soon)

```bash
# JavaFX build
mvn clean javafx:run

# Or with Gradle
gradle run
```

---

## 🎯 Usage

### Start Chat Server

```bash
python scripts/chat_server.py
```

### Launch GUI

```bash
mvn javafx:run
```

### Train LoRA Adapter

```bash
python scripts/train_lora.py --model qwen2.5:1.8b --adapter coding_v1
```

---

## 📁 Project Structure

```
sims-java-neo-fx/
├── src/main/java/          # Java source code
├── resources/              # Assets, configs, models
├── scripts/                # Python utilities
├── docs/                   # Documentation
│   ├── BLUEPRINT.md       # Architecture & plans
│   ├── CHANGELOG.md       # Version history
│   └── SETUP_GUIDE.md     # Installation guide
├── config/                 # YAML configurations
└── adapters/               # LoRA adapter weights
```

---

## 🗓️ Development Phases

| Phase | Name | Status | ETA |
|-------|------|--------|-----|
| 1 | Discovery & Planning | 🟢 In Progress | Day 1 |
| 2 | Repo Consolidation | ⏳ Pending | Day 2 |
| 3 | Core Build | ⏳ Pending | Day 3-4 |
| 4 | Memory Systems | ⏳ Pending | Day 5-6 |
| 5 | Telemetry + Balance | ⏳ Pending | Day 7 |
| 6 | Testing + Polish | ⏳ Pending | Day 8-9 |

---

## 🧠 Key Concepts

### LoRA Adapters (Circular Token Trees)

Instead of loading multiple full models, we keep one base model warm and swap LoRA adapters:

```
Base Model (qwen2.5:1.8b) → Resident in RAM
    ↓
LoRA Adapter 1 (Coding) → 50MB
LoRA Adapter 2 (Writing) → 50MB
LoRA Adapter 3 (Math) → 50MB
LoRA Adapter 4 (Chat) → 50MB

Switching: Instant (only weights change, model stays warm)
```

### Knowledge Graph Pyramid

```
        ┌─────────────┐
        │  KG Nodes   │  (Structured knowledge)
        └─────────────┘
              ↓
        ┌─────────────┐
        │ Scratchpads │  (Working memory)
        └─────────────┘
              ↓
        ┌─────────────┐
        │ Chain-of-   │  (Reasoning traces)
        │   Thought   │
        └─────────────┘
```

### Memory Mapping (mmap)

Models are mmap'd to disk, so they don't consume RAM until accessed:

```python
# Instead of loading full model to RAM:
model = load_model("qwen2.5:1.8b")  # Uses 2GB RAM

# We memory-map:
model = mmap_model("qwen2.5:1.8b.gguf")  # Uses ~100MB RAM
# Pages loaded on-demand from disk
```

---

## 📊 Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| Model Load Time | <5s | - |
| LoRA Switch | <100ms | - |
| RAM Usage (1.8B) | <2GB | - |
| Context Length | 4096 tokens | - |
| Inference Speed | 50 tokens/s | - |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Update documentation
5. Submit a pull request

---

## 📝 Documentation

- **[BLUEPRINT.md](docs/BLUEPRINT.md)** - Full architecture and plans
- **[CHANGELOG.md](CHANGELOG.md)** - Version history
- **[SETUP_GUIDE.md](docs/SETUP_GUIDE.md)** - Installation steps

---

## 🙏 Acknowledgments

- **Ollama** - Local LLM runtime
- **Qwen** - Model family (Alibaba)
- **Danube** - Efficient SLMs (H2O.ai)
- **JavaFX** - Desktop GUI framework

---

## 📄 License

MIT License - See LICENSE file

---

**Built with 💙 by the Architect + Aegis**

*Gamifying SLM training, one adapter at a time.* 🎮🧠🚀
