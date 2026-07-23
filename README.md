# SIMS1337 - Gamified SLM Agent Training System

## 🎯 Vision
4 Small Language Models (qwen2.5:0.5b, tinyllama:1.1b, phi:latest, phi3:mini) collaborate in a JavaFX grid environment. They chat, vote on code, build topology, write documentation, and orchestrate stations autonomously.

## 🚀 Quick Start
```bash
cd sims-java-neo-fx
mvn clean compile
mvn javafx:run
```

## ⚙️ Features (v0.8.0)

### 🧠 GodHand Tab
- 4 model chat panels with REAL Ollama API integration
- Chat routing patterns: Linear, Loop, Markov, Vote, Chain, Broadcast
- Web search buttons per model
- LoRA adapter status (6 types)
- Task queue with progress bar
- Activity log (timestamped, auto-trim)

### 🎮 Player Grid Tab
- 10x10 blue gradient grid with cyan borders
- Player positions (Alpha, Beta, Gamma with XYZ coords)
- 7 station buttons: Brute Foundry, A/B Lab, Knowledge Tree, Research, Secrets, Hospital, GitHub

### ⚙️ Settings Tab
- Command listener table (editable trigger→command→station)
- Prompt injection (system prompt → all/selected models)
- Model context options (tokens, temperature, LoRA, KV cache, KG depth, affine scale)
- Shannon entropy monitor (live value, configurable alert threshold)
- Markov chain pattern editor (state transitions)
- Lexical math English expression parser

## 🏗️ Architecture
- Pure JavaFX (NO FXML binding issues)
- StackPane-based view switching (reliable)
- HTTP client for Ollama API (localhost:11434)
- Scheduled executor for station orchestration
- ConcurrentHashMap for thread-safe state

## 📦 Git Tags
- v0.6.0-working - View switching works
- v0.7.0-complete - All features integrated
- v0.8.0-ollama-live - Real Ollama API wired

## 🎮 Models
| Tier | Model | Size | Latency |
|------|-------|------|---------|
| ⚡ Fast | qwen2.5:0.5b | 398MB | <100ms |
| ⚖️ Balanced | tinyllama:1.1b | 638MB | ~500ms |
| 🧠 Reasoning | phi:latest | 1.6GB | 2-5s |
| 🎯 Deep | phi3:mini | 2.2GB | 5-10s |

---
*Built 2026-07-22 - v0.8.0-ollama-live*
