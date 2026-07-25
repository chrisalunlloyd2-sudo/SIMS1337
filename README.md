# SIMS1337 - GodHand Agent Orchestration Dashboard

**Version:** v0.13.0  
**Author:** Architect (chrisalunlloyd2)  
**Repository:** [SIMS1337](https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx)  
**License:** MIT  

---

## 🎯 What is SIMS1337?

SIMS1337 is a **Small Language Model (SLM) agent orchestration platform** built in pure JavaFX. It provides a unified control center for running, evaluating, and orchestrating multiple local AI models through Ollama. Think of it as a **better OpenClaw** — a desktop application where SLM agents chat, vote, build topology, run pipelines, and operate autonomously.

### Core Philosophy

- **Full control over small language models** — automate code completion, essay writing, task execution
- **Model evaluation** — test what each model can do, keep stats, assign LoRA adapters
- **Autonomous operation** — night cycle: 18:00 votes → 20:00 deploy → 22:00 email brief
- **Everything is a changeable GUI component** — no hidden config, no FXML, pure programmatic JavaFX

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version | How to Install |
|-------------|---------|----------------|
| Java JDK | 17+ | `choco install openjdk17` or [Adoptium](https://adoptium.net/) |
| Maven | 3.9+ | `choco install maven` or [Maven](https://maven.apache.org/) |
| Ollama | Latest | `choco install ollama` or [Ollama](https://ollama.com/) |
| Git | Any | `choco install git` |

### Install & Run

```bash
# 1. Clone the repository
git clone https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx.git
cd sims-java-neo-fx

# 2. Pull required models (at minimum)
ollama pull qwen2.5:0.5b
ollama pull tinyllama:1.1b

# 3. Build & Run
mvn clean compile javafx:run
```

### Verified Models

| Model | Size | Speed | Best For |
|-------|------|-------|----------|
| qwen2.5:0.5b | 398MB | <100ms | Fast chat, quick tasks |
| tinyllama:1.1b | 638MB | ~500ms | Balanced, essays |
| phi:latest | 1.6GB | ~2-5s | Reasoning, analysis |
| phi3:mini | 2.2GB | ~5-10s | Deep thinking |
| llama3.2:1b | 1.3GB | ~1-3s | Tools, 131K context |
| deepseek-r1:1.5b | 1.1GB | ~2-5s | THINKING capability |
| gemma2:2b | 1.6GB | ~2-5s | Google's latest |
| codellama:7b | 3.8GB | ~5-15s | Code generation |

---

## 🎮 Interface

### 4 Tabs

```
[🧠 GodHand] [🎮 Player Grid] [🎯 Gameplay] [⚙️ Settings]
```

### 🧠 GodHand Tab
- **Shared God Chat** — All model conversations in one color-coded stream
- **6 Model Panels** — Each with: Route pattern, Next route, Loop, Web search, API button
- **LoRA Adapters** — 6 types (CHAT, CODE, PATHFIND, MOTIVES, CAREER, ANALYSIS)
- **Task Queue** — Progress bar + list view
- **Activity Log** — Timestamped, auto-trims to 500 lines

### 🎮 Player Grid Tab
- **10×10 Clickable Grid** — Left-click moves agents, right-click starts pipelines
- **3 Agents** — Alpha (green), Beta (blue), Gamma (orange) with XYZ coordinates
- **7 Stations** — Brute Foundry, A/B Lab, Knowledge Tree, Research, Secrets, Hospital, GitHub

### 🎯 Gameplay Tab
- **Headless Pipeline** — One-click: Code Gen, Essay, Task, Full Pipeline, Vote
- **Agent Inventory** — Shield, Sword, Resources, Key Fragments, Blueprint
- **Agent Skills** — Code Gen L4, Analysis L3, Writing L3, Voting L5, Building L2
- **Active Quests** — Main, Side, Daily, Epic with progress bars
- **Achievements** — First Chat, Pipeline Master, Night Owl, Model Collector

### ⚙️ Settings Tab
- **Web APIs** — Per-model HTTP endpoints (editable table)
- **Model Manager** — Pull/list/switch Ollama models
- **Voting System** — Proposals, approve/reject, consensus tracking
- **Topology Builder** — Node/edge graph for world structure
- **Night Cycle** — Autonomous: 18:00 votes → 20:00 deploy → 22:00 email
- **Model Evaluation** — Capability matrix, LoRA config, prompt templates, stats
- **Routing Table** — From → Pattern → Next → Loop (editable)
- **Command Listener** — Trigger → Command → Station table
- **Prompt Injection** — System prompt text area + inject to all
- **Context Options** — Tokens, Temperature, LoRA, KV Cache, KG Depth, Affine Scale
- **Entropy Monitor** — Live Shannon entropy with alert threshold
- **Markov Chains** — Editable state transition table
- **Lexical Math** — Expression parser with variable substitution
- **GitHub Push** — One-click git add/commit/push

---

## 🏗️ Architecture

```
SIMS1337 (Pure JavaFX, NO FXML)
│
├── GodHandApp.java (~1000 lines, 75KB)
│   ├── View Management (StackPane switching)
│   ├── Ollama API Integration (HTTP client)
│   ├── Model Chat System (6 models)
│   ├── Shared God Chat (color-coded stream)
│   ├── Agent Movement (grid click handling)
│   ├── Station Pipelines (7-station chain)
│   ├── Model Evaluation (capability matrix)
│   ├── LoRA Adapter Config
│   ├── Prompt Engineering Templates
│   ├── Voting System
│   ├── Topology Builder
│   ├── Night Cycle Automation
│   ├── Headless Pipeline (in-GUI)
│   ├── Gameplay (inventory, skills, quests, achievements)
│   ├── Entropy Monitor
│   ├── Markov Chain Patterns
│   ├── Lexical Math English
│   └── GitHub Integration
│
├── HeadlessOllamaPipeline.java
│   ├── Code Generation (3-model chain)
│   ├── Essay Writing (3-model chain)
│   ├── Task Completion (3-model chain)
│   ├── Full Pipeline (6-model chain)
│   └── Voting (4-model consensus)
│
└── pom.xml (Maven + JavaFX plugin)
```

---

## 🔧 Development

### Build Commands

```bash
# Compile only
mvn compile

# Compile + run
mvn clean compile javafx:run

# Package as JAR
mvn package

# Run headless pipeline
java -cp target/classes com.aigen.sims.HeadlessOllamaPipeline code "your task"
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_HOME` | `C:\Program Files\Java\jdk-17` | JDK installation path |
| `OLLAMA_HOST` | `http://localhost:11434` | Ollama API endpoint |

### Key Design Decisions

1. **NO FXML** — FXML `@FXML` binding silently fails with view switching. Pure programmatic JavaFX with `StackPane.getChildren().setAll()` is the only reliable approach.
2. **Checkpoint before every change** — `git commit + tag + changelog + blueprint + README`
3. **Scientific method** — One variable at a time, tracers for evidence
4. **3-strike rule** — If same fix fails 3×, restore from git and try different architecture

---

## 📊 Version History

| Version | Milestone |
|---------|-----------|
| v0.3.0 | Blue grid + nav buttons |
| v0.6.0 | View switching WORKS (StackPane, no FXML) |
| v0.7.0 | Models, chats, stations, entropy, Markov, commands |
| v0.8.0 | Real Ollama API — 4 SLM agents chatting live |
| v0.9.0 | Agent movement + pipelines + GitHub push |
| v0.10.0 | Shared God Chat + editable routing + lexical search + loops |
| v0.11.0 | Web APIs + Model Manager + Voting + Topology + Night Cycle |
| v0.12.0 | 6 models + Evaluation + LoRA + Prompt Engineering + Stats |
| v0.13.0 | Gameplay tab + Headless Pipeline in GUI |

---

## 🤖 Model Capability Matrix

| Model | Code | Essay | Logic | Creative | Speed | Reliability |
|-------|------|-------|-------|----------|-------|-------------|
| qwen2.5:0.5b | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐ | ⚡⚡⚡⚡⚡ | ⭐⭐⭐⭐ |
| tinyllama:1.1b | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⚡⚡⚡⚡ | ⭐⭐⭐ |
| phi:latest | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⚡⚡ | ⭐⭐⭐ |
| phi3:mini | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⚡ | ⭐⭐⭐⭐ |
| llama3.2:1b | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⚡⚡⚡ | ⭐⭐⭐⭐ |
| deepseek-r1:1.5b | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⚡⚡ | ⭐⭐⭐⭐ |

---

## 🌙 Night Cycle

The autonomous night cycle runs on a schedule:

1. **18:00** — All models vote on pending proposals
2. **20:00** — Approved changes are deployed
3. **22:00** — Email brief sent to chrisalunlloyd2@gmail.com

Toggle ON/OFF in Settings → Night Cycle.

---

## 🧪 Testing

```bash
# Test Ollama connectivity
curl http://localhost:11434/api/tags

# Test a single model
curl -X POST http://localhost:11434/api/generate -d '{"model":"qwen2.5:0.5b","prompt":"Hello","stream":false}'

# Run headless pipeline test
java -cp target/classes com.aigen.sims.HeadlessOllamaPipeline code "fibonacci sequence"

# Check Java processes
tasklist /FI "IMAGENAME eq java.exe"
```

---

## 📡 Hourly Heartbeat

A cron job runs every hour that:
1. Checks system health (Ollama, Java, Git)
2. Runs model voting on backend improvements
3. Builds what passed
4. Backs up to GitHub
5. Logs errors
6. Improves design

---

## 🚀 Future Roadmap

- [ ] RAG integration (Knowledge Graph + semantic search)
- [ ] Server orchestration (load balancing, health monitoring)
- [ ] Self-exploration (agents analyze own outputs)
- [ ] More Ollama models (mistral, mixtral, command-r)
- [ ] Web dashboard
- [ ] Multi-user support
- [ ] Plugin system

---

## 💙 Credits

Built by the Architect and Hermes Agent.  
A better OpenClaw. Pure JavaFX. No FXML. Everything works.
