# SIMS Java Neo FX - Gamified SLM Agent Training System

## 🎯 Vision
A JavaFX-based GUI for orchestrating 4-tier Small Language Model (SLM) agents in a gamified environment where models collaborate, vote on code additions, build topology, and autonomously improve the system.

## 🏗️ Architecture

### Core Components
- **GodHand GUI**: JavaFX dashboard for model orchestration
- **Player Grid 3D**: 10x10x5 grid where agents navigate and interact
- **Model Pool**: 4-tier SLM system (qwen2.5:0.5b, tinyllama:1.1b, phi:latest, phi3:mini)
- **LoRA Adapters**: 6 specialized adapters (CHAT, CODE, PATHFIND, MOTIVES, CAREER, ANALYSIS)
- **Station Orchestrator**: 7 facilities (Brute Foundry, A/B Lab, Knowledge Tree, Research Lab, Secrets Locker, Hospital, GitHub Station)

### Model Tiers
| Tier | Model | Size | Latency | Use Case |
|------|-------|------|---------|----------|
| ⚡ Fast | qwen2.5:0.5b | 398MB | <100ms | Real-time tasks |
| ⚖️ Balanced | tinyllama:1.1b | 638MB | ~500ms | Game logic |
| 🧠 Reasoning | phi:latest | 1.6GB | 2-5s | Social AI |
| 🎯 Deep | phi3:mini | 2.2GB | 5-10s | Complex decisions |

## 🚀 Quick Start

### Prerequisites
- Java 17+ (JDK)
- Maven 3.9+
- Ollama server running locally

### Build & Run
```bash
cd sims-java-neo-fx
mvn clean compile
mvn javafx:run
```

### GUI Controls
- **🧠 GodHand**: Dashboard view (model status, LoRA adapters, task queue)
- **🎮 Player Grid**: 10x10 blue gradient grid with cyan borders
- **Navigation**: Click buttons in top blue bar to switch views

## 📊 Features

### Phase 1 (Complete)
- ✅ GodHand dashboard with 4 model tiers
- ✅ LoRA adapter status display (6 types)
- ✅ Task queue with progress bar
- ✅ Activity log console
- ✅ Navigation bar with view switching
- ✅ Player Grid 3D (10x10 blue gradient)

### Phase 2 (In Progress)
- ⏳ Model routing patterns table
- ⏳ Web search integration buttons
- ⏳ Command listener (preset → terminal trigger)
- ⏳ Prompt injection menu

### Phase 3 (Planned)
- Model context options (tokens, LoRA, KV, KG, affine)
- Station orchestration (Brute Force, Hospital, GitHub)
- Shannon entropy monitoring
- Markov chain patterns
- Lexical math English programming

## 🔧 Configuration

### Ollama Models
```bash
ollama pull qwen2.5:0.5b
ollama pull tinyllama:1.1b
ollama pull phi:latest
ollama pull phi3:mini
```

### Environment Variables
```bash
JAVA_HOME=/path/to/jdk-17
MAVEN_HOME=/path/to/maven
OLLAMA_HOST=localhost:11434
```

## 📝 Development Workflow

### Version Control
- Checkpoints saved to Git with semantic versioning (v0.1.0, v0.2.0, etc.)
- Change logs in `logs/` directory with timestamps
- Blueprints and documentation in `docs/` directory

### Testing
- JUnit 5 for unit tests
- TestFX for GUI testing
- Target: >90% code coverage

## 🐛 Known Issues

### View Switching (Being Fixed)
**Problem**: FXML reload creates new controller instance, losing scene reference
**Status**: Investigating VBox child swapping vs. scene root replacement
**Workaround**: Use pure GodHand (no nav bar) for stability

## 📈 Roadmap

### Week 1-2: Core Infrastructure
- [x] GodHand GUI foundation
- [x] Navigation bar
- [ ] Stable view switching
- [ ] Documentation system

### Week 3-4: Model Integration
- [ ] Ollama API integration
- [ ] Model routing patterns
- [ ] LoRA adapter switching

### Week 5-6: Station Orchestration
- [ ] Brute Foundry (autonomous coding)
- [ ] Hospital (agent recovery)
- [ ] GitHub Station (repo management)

### Week 7-8: Advanced Features
- [ ] Shannon entropy monitoring
- [ ] Markov chain patterns
- [ ] Lexical math English

## 📄 License
MIT License - See LICENSE file

## 🙏 Acknowledgments
- AIGEN_SYS autonomous foundry system
- OpenClaw mission control inspiration
- 4-tier SLM architecture design
