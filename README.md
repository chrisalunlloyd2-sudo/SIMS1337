# ⚙️ SIMS1337 — GodHand Agent Orchestration Dashboard

<p align="center">
  <img src="https://img.shields.io/badge/version-v0.18.0-00d9ff?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Java-17%2B-ed8b00?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/JavaFX-pure%20programmatic-5382a1?style=for-the-badge" alt="JavaFX">
  <img src="https://img.shields.io/badge/Ollama-8%20models-ffffff?style=for-the-badge&logo=ollama" alt="Ollama">
  <img src="https://img.shields.io/badge/systems-22%2F22%20verified-00ff88?style=for-the-badge" alt="Systems">
  <img src="https://img.shields.io/badge/gists-8%20live-6e5494?style=for-the-badge&logo=github" alt="Gists">
  <img src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge" alt="License">
</p>

<p align="center">
  <b>A self-growing, self-voting, self-deploying SLM agent grid.</b><br>
  8 models dream up game mechanics at midnight, vote on them at 6pm,<br>
  build the winners at 8pm, and email the brief at 10pm.<br>
  <i>Every night. Forever. No human needed.</i>
</p>

---

## 📡 DATAFLOW — How the Models Talk

```
                              ┌─────────────────────────────────────────────────────┐
                              │                 GODHAND DASHBOARD                    │
                              │              http://localhost:8899                   │
                              └──────┬──────────────────────────────────┬───────────┘
                                     │                                  │
              ┌──────────────────────┼──────────────────────────────────┼──────────────────────┐
              │                      │                                  │                      │
              ▼                      ▼                                  ▼                      ▼
     ┌────────────────┐    ┌────────────────┐    ┌────────────────┐    ┌────────────────┐
     │  qwen2.5:0.5b  │    │  tinyllama:1.1b│    │  llama3.2:1b   │    │ deepseek-r1:1.5b│
     │  Fast Responder│    │ Balanced Writer│    │   Tool User    │    │  Deep Thinker   │
     │  grid,ability, │    │ ability,grid,  │    │ tool,ability,  │    │ logic,backend,  │
     │  tool          │    │ node           │    │ grid           │    │ tool            │
     └───────┬────────┘    └───────┬────────┘    └───────┬────────┘    └───────┬────────┘
             │                     │                     │                     │
             │    ┌────────────────┼─────────────────────┼─────────────────────┤
             │    │                │                     │                     │
             ▼    ▼                ▼                     ▼                     ▼
     ┌────────────────┐    ┌────────────────┐    ┌────────────────┐    ┌────────────────┐
     │   phi:latest   │    │   phi3:mini    │    │   gemma2:2b    │    │  codellama:7b  │
     │   Reasoning    │    │  Deep Reason   │    │   Balanced     │    │  Code Gen      │
     │ logic,tool,    │    │ logic,backend, │    │ node,grid,     │    │ tool,backend,  │
     │ ability        │    │ node           │    │ backend        │    │ node           │
     └───────┬────────┘    └───────┬────────┘    └───────┬────────┘    └───────┬────────┘
             │                     │                     │                     │
             └─────────────────────┼─────────────────────┼─────────────────────┘
                                   │                     │
                                   ▼                     ▼
                          ┌─────────────────────────────────────────────┐
                          │           MULTI-AGENT TOPOLOGY               │
                          │                                             │
                          │   Agent Alpha (Orchestrator) ⬡(0,0)         │
                          │        │                  │                 │
                          │        ▼                  ▼                 │
                          │   Agent Beta (Builder)   Agent Gamma (Analyst)│
                          │   ⬡(3,-2)               ⬡(-3,2)             │
                          │        │                  │                 │
                          │        └────────┬─────────┘                 │
                          │                 ▼                           │
                          │         Cross-Correlation                   │
                          │         (Dream Phase)                       │
                          └─────────────────────────────────────────────┘
```

---

## 🌙 NIGHT CYCLE — Autonomous Operation

```
                         ┌──────────────────────────────────────────┐
                         │         NIGHT CYCLE (every 24h)          │
                         └──────────────────────────────────────────┘

    00:00 💤 DREAM                    18:00 🗳️ VOTE
    ┌─────────────────────┐          ┌─────────────────────────────┐
    │ 8 models cross-      │          │ Each model votes by role:   │
    │ correlate memories   │          │                             │
    │                      │          │ deepseek → logic,backend    │
    │ 8 game mechanics     │          │ codellama → tool,backend    │
    │ generated:           │          │ qwen → grid,ability         │
    │  • Logic Systems     │          │ phi3 → logic,node           │
    │  • Node Types        │          │ llama3.2 → tool,ability     │
    │  • Tools             │          │ tinyllama → ability,grid    │
    │  • Backend Systems   │          │ gemma2 → node,grid          │
    │  • Agent Abilities   │          │ phi → logic,tool            │
    │  • Grid Mechanics    │          │                             │
    │                      │          │ 5+ yes = APPROVED           │
    │ 4 → proposals        │          │ 5+ no  = REJECTED           │
    └─────────┬────────────┘          └──────────┬──────────────────┘
              │                                  │
              │                                  ▼
              │                       ┌─────────────────────────────┐
              │                       │ 20:00 🚀 DEPLOY              │
              │                       │                             │
              │                       │ implementApprovedProposals() │
              │                       │                             │
              │                       │ tool → addTool()             │
              │                       │ node → addStation()          │
              │                       │ backend → addStation()       │
              │                       │ logic → addTool()            │
              │                       │ ability → addTool()         │
              │                       │ grid → addTool()             │
              │                       │                             │
              │                       │ Push manifest to gist        │
              │                       └──────────┬──────────────────┘
              │                                  │
              └──────────────────────────────────┤
                                                 ▼
                                      ┌─────────────────────────────┐
                                      │ 22:00 📧 EMAIL              │
                                      │                             │
                                      │ Brief → chrisalunlloyd2@     │
                                      │ gmail.com                   │
                                      └─────────────────────────────┘
```

---

## 🧬 NEUROMORPHIC LINEAGE — The Full Evolutionary Chain

```
Boolean Logic (1854)
    │
    ▼
Turing Computation (1936)
    │
    ▼
McCulloch-Pitts Neuron (1943) ─── First formal neuron model
    │
    ▼
Hebbian Learning (1949) ─── "Cells that fire together wire together"
    │
    ▼
Perceptron (1957) ─── First trainable neuron
    │
    ▼
Analog Neural Machines (1960s-70s) ─── Physical circuits mimicking neurons
    │
    ▼
Atari Deterministic Engines (1980s) ─── Fixed-step loops, perfect reproducibility
    │
    ▼
Procedural Generation ─── Rule-driven world creation
    │
    ▼
Automated 3D Design (1990-94) ─── Constraint-based geometry
    │
    ▼
Voodoo/Glide Bare-Metal Pipelines (1995-99) ─── Direct-to-silicon execution
    │
    ▼
CERN Grid AI (1990s) ─── Distributed job scheduling, particle classification
    │
    ▼
Agent-Based Systems (1990s) ─── Autonomous rule-driven agents, message passing
    │
    ▼
LSTM (1995) ─── Memory cell = biological dendrite
    │
    ▼
GRU (2014) ─── Simplified gating, more biologically plausible
    │
    ▼
Attention Mechanism (2014) ─── Synaptic weighting, dynamic routing
    │
    ▼
Transformers (2017) ─── Massive parallel synaptic routing
    │
    ▼
MoE — Mixture of Experts ─── Neurons as specialized cortical columns
    │
    ▼
SSMs — State-Space Models ─── Continuous-time neural dynamics
    │
    ▼
Retrieval-Augmented Systems ─── External memory, hippocampal simulation
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  SIMS1337 — Neuromorphic Local Agent Grids (2026)            │
│                                                              │
│  BM25 = lexical hippocampus                                  │
│  SOP DB = procedural memory                                  │
│  Logic predictor = prefrontal cortex                         │
│  Router = brainstem                                          │
│  R/P/L/E/F/C nodes = cortical microcircuits                  │
│  Stateless agents = spiking neurons                          │
│  Dependency graph = motor cortex                             │
│  Performatives = neurotransmitter signals                   │
│                                                              │
│  PRINCIPLE: A cognitive engine is a distributed,            │
│            stateless, message-passing organism.              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗺️ HEX GRID — 4D Map (Q,R,Z + Time Pulse)

```
                         ⬡(-4,4) Hospital
                      ⬡(-3,3)    ⬡(-4,3)
                   ⬡(-2,3)    ⬡(-3,2) Agent Gamma
                ⬡(-1,3)    ⬡(-2,2)    ⬡(-3,1)
             ⬡(0,3)     ⬡(-1,2)    ⬡(-2,1)    ⬡(-3,0)
          ⬡(1,3)     ⬡(0,2)     ⬡(-1,1)    ⬡(-2,0)    ⬡(-3,-1)
       ⬡(2,3)     ⬡(1,2)     ⬡(0,1)     ⬡(-1,0)    ⬡(-2,-1)    ⬡(-3,-2)
     ⬡(3,3)     ⬡(2,2)     ⬡(1,1)     ⬡(0,0) HUB  ⬡(-1,-1)    ⬡(-2,-2)    ⬡(-3,-3)
       ⬡(3,2)     ⬡(2,1)     ⬡(1,0)     ⬡(0,-1)    ⬡(-1,-2)    ⬡(-2,-3)
          ⬡(3,1)     ⬡(2,0)     ⬡(1,-1)    ⬡(0,-2)    ⬡(-1,-3)
             ⬡(3,0)     ⬡(2,-1)    ⬡(1,-2)    ⬡(0,-3)
                ⬡(3,-1)    ⬡(2,-2)    ⬡(1,-3)
                   ⬡(3,-2) Agent Beta  ⬡(2,-3)
                      ⬡(3,-3)    ⬡(4,-3)
                         ⬡(4,-4) Brute Foundry

    61 hexes | Axial Q,R coordinates | Z-axis elevation (scroll wheel)
    4D time pulse: sin-wave opacity + scale breathing (50ms animation)
    FOW: 1-hop visibility — hexes outside agent range dim to 15%
    Hover glow (green stroke) | Left-click: move agent | Right-click: pipeline
```

---

## 🔧 TOOLS — 10 Base + Dynamic Growth

| Tool | Description | Category |
|------|-------------|----------|
| `terminal` | Execute shell commands | base |
| `file_read` | Read files from disk | base |
| `file_write` | Write files to disk | base |
| `web_search` | Search the internet | base |
| `web_fetch` | Fetch URL content | base |
| `git` | Git operations (commit, push, pull) | base |
| `ollama` | Query other models | base |
| `memory` | Read/write persistent memory | base |
| `vote` | Cast votes on proposals | base |
| `pipeline` | Chain multiple models together | base |
| *dynamic* | *Added nightly by approved proposals* | dream |

**New tools are added automatically at 20:00 deploy when proposals pass 5+ yes votes.**

---

## 🏗️ STATIONS — 7 Base + Dynamic Growth

| Station | Description | Handler |
|---------|-------------|---------|
| Brute Foundry | Autonomous code generation and review | `bruteFoundryAdmission()` |
| A/B Lab | Model comparison and evaluation | log |
| Knowledge Tree | KG nodes + RAG pipeline | `knowledgeGraphInit()` |
| Research | Self-exploration and analysis | `selfExplorationInit()` |
| Secrets | Secure credential storage | log |
| Hospital | Agent diagnostics and memory repair | `hospitalAdmission()` |
| GitHub | Git sync and backup | `pushToGitHub()` |
| *dynamic* | *Added nightly by approved proposals* | auto-generated |

---

## 🚀 QUICK START

### Prerequisites

| Requirement | Version | Install |
|-------------|---------|---------|
| Java JDK | 17+ | `choco install openjdk17` |
| Ollama | Latest | `choco install ollama` |
| Git | Any | `choco install git` |
| GIST_TOKEN | env var | GitHub fine-grained token with `gist` scope |

### Install & Run

```bash
# 1. Clone
git clone https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx.git
cd sims-java-neo-fx

# 2. Pull models
ollama pull qwen2.5:0.5b
ollama pull tinyllama:1.1b
ollama pull llama3.2:1b
ollama pull deepseek-r1:1.5b
ollama pull phi:latest
ollama pull phi3:mini
ollama pull gemma2:2b
ollama pull codellama:7b

# 3. Set GIST_TOKEN
export GIST_TOKEN="github_pat_..."

# 4. Compile & Run
export JAVA_HOME="C:/Program Files/Java/jdk-17"
SRC="src/main/java"
OUT="target/classes"
M2="$HOME/.m2/repository"
JFX="$M2/org/openjfx"
MP="$JFX/javafx-base/17.0.6/javafx-base-17.0.6-win.jar"
MP="$MP;$JFX/javafx-controls/17.0.6/javafx-controls-17.0.6-win.jar"
MP="$MP;$JFX/javafx-graphics/17.0.6/javafx-graphics-17.0.6-win.jar"
MP="$MP;$JFX/javafx-fxml/17.0.6/javafx-fxml-17.0.6-win.jar"
CP="$M2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
CP="$CP;$M2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
CP="$CP;$M2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
CP="$CP;$M2/org/apache/httpcomponents/client5/httpclient5/5.2.1/httpclient5-5.2.1.jar"
CP="$CP;$M2/org/apache/httpcomponents/core5/httpcore5/5.2/httpcore5-5.2.jar"
CP="$CP;$M2/org/apache/httpcomponents/core5/httpcore5-h2/5.2/httpcore5-h2-5.2.jar"
CP="$CP;$M2/org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar"
CP="$CP;$M2/org/java-websocket/Java-WebSocket/1.5.3/Java-WebSocket-1.5.3.jar"
mkdir -p "$OUT"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d "$OUT" -cp "$CP" --module-path "$MP" --add-modules javafx.controls,javafx.fxml "$SRC/com/aigen/sims/GodHandApp.java"
"$JAVA_HOME/bin/java" --module-path "$MP" --add-modules javafx.controls,javafx.fxml -cp "$CP:$OUT" com.aigen.sims.GodHandApp
```

### Verify

```bash
# Dashboard
curl http://localhost:8899/api/status
# → {"version":"0.18.0","models":8,"kgNodes":23,"errors":0,...}

# Web UI
open http://localhost:8899
```

---

## 🎮 HOW TO PLAY

### The Grid
- **61 hexagons** in a radius-4 axial grid
- **3 agents**: Alpha (0,0), Beta (3,-2), Gamma (-3,2)
- **Left-click** a hex to move the selected agent there
- **Right-click** a hex to start a pipeline from that station
- **Scroll wheel** changes Z-axis elevation (0-4)
- **Hover** any hex to see its TODOs and coordinates
- **FOW**: hexes outside 1-hop of any agent are dimmed

### The Models
- **8 Ollama models** running locally
- Each has a **role** and **specialty** for voting
- Models auto-rotate, auto-commit, and auto-heal
- **God Chat** shows all model conversations color-coded

### The Night Cycle
- **00:00** — Dream Phase: models cross-correlate, generate 8 game mechanics, 4 become proposals
- **18:00** — Vote Phase: each model votes by role (specialty match = 90% approval)
- **20:00** — Deploy Phase: approved proposals become real tools/stations, pushed to gist
- **22:00** — Email Phase: brief sent

### The Voting System
- **8 seeded proposals** (terrain, skill trees, economy, FOW, dream journal, consensus, weather, breeding)
- **+4 new proposals every night** from dream phase
- **5+ yes votes** = approved → deployed at 20:00
- **5+ no votes** = rejected
- Each model votes based on its specialty (not random)

### Growing the Game
The system grows itself. Every night:
1. Models dream up new tools, stations, abilities, and grid mechanics
2. They vote on them by role
3. Approved proposals are **actually built** — `addTool()` or `addStation()`
4. The tool/station count increases permanently
5. Everything is pushed to GitHub and gists

---

## 📊 22 BACKEND SYSTEMS

| # | System | Status |
|---|--------|--------|
| 1 | Hospital | ✅ Active |
| 2 | Brute Foundry | ✅ Active |
| 3 | Knowledge Graph (23 nodes, 19 edges) | ✅ Active |
| 4 | Server Orchestration | ✅ Active |
| 5 | Self-Exploration | ✅ Active |
| 6 | Error Logging | ✅ Active |
| 7 | Design | ✅ Active |
| 8 | Real RAG (64-dim vectors) | ✅ Active |
| 9 | Fine-Tuning (4 datasets) | ✅ Active |
| 10 | Multi-Agent Topology (7 nodes, 14 edges) | ✅ Active |
| 11 | Web Dashboard (:8899) | ✅ Active |
| 12 | Plugin System (5 plugins) | ✅ Active |
| 13 | Perfect Prompts (8 templates, 89% avg) | ✅ Active |
| 14 | Map Guidance (61 hex weights) | ✅ Active |
| 15 | Perfect Patterns (8 routes) | ✅ Active |
| 16 | Tools System (10+dynamic) | ✅ Active |
| 17 | Persistent Memory (3 agents) | ✅ Active |
| 18 | FOW — Fog of War (1-hop) | ✅ Active |
| 19 | Hex TODO System (16 items) | ✅ Active |
| 20 | Gist Context (11 fragments) | ✅ Active |
| 21 | Gist Sync (30min) | ✅ Active |
| 22 | Night Cycle (Armed) | ✅ Active |

---

## 📦 GIST ECOSYSTEM — 8 Live

| Gist | ID | Content |
|------|----|---------|
| neuromorphic-lineage | `87a6e878` | Full evolutionary chain, 5 layers, 21 systems, 5 principles |
| memories-db | `14e94c9d` | SQLite schema, seed data for 3 agents, homeostasis pruning |
| project-places | `09a19470` | Hex coordinates for all repos, agents, stations |
| databases | `d0733fb0` | KG schema, seed nodes/edges, export schedule |
| hex-fow | `a23215d0` | Hex geometry, FOW deployment, Go middleman |
| topological-memory | `93ef40fd` | H0/H1/H2 persistent homology, simplicial complex |
| hyper-buffer | `f918a05e` | O(1) bitwise pruning engine |
| matrix-wince | `c91b5b29` | APK compiler pipeline |

---

## 🧪 TESTING

```bash
# API health
curl http://localhost:8899/api/status

# Dashboard
curl http://localhost:8899/

# Ollama models
curl http://localhost:11434/api/tags

# Java processes
tasklist /FI "IMAGENAME eq java.exe"

# Git status
cd C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx && git log --oneline -5
```

---

## 📡 HOURLY HEARTBEAT

A cron job (`701829ee2a9a`) runs every hour:
1. Checks Java GUI is running — restarts if not
2. Checks Ollama is running
3. Checks dashboard health (version, models, errors)
4. Checks all 8 gists are accessible
5. Commits and pushes any uncommitted changes
6. Checks hex TODO state for stale items
7. Investigates and fixes any errors
8. Reports summary

---

## 🏛️ ARCHITECTURE

```
SIMS1337 v0.18.0 — GodHandApp.java (~2700 lines, 160KB)
│
├── View Management (StackPane switching, NO FXML)
├── 4D Hex Map (61 hexes, Q/R/Z + time pulse)
├── FOW Middleware (1-hop visibility)
├── Hex TODO System (16 items, 15 cells)
├── Ollama API Integration (8 models)
├── Model Chat System (color-coded God Chat)
├── Agent Movement (hex click handling)
├── Station Pipelines (7 base + dynamic)
├── Dynamic Tool Registry (10 base + addTool())
├── Dynamic Station Registry (7 base + addStation())
├── Voting System (8 proposals + dream-generated)
├── Role-Based Voting (each model votes by specialty)
├── Dream Engine (8 mechanics/night, 4→proposals)
├── Deploy Implementation (approved → real tools/stations)
├── Night Cycle (00:00 dream → 18:00 vote → 20:00 deploy → 22:00 email)
├── Knowledge Graph (23 nodes, 19 edges)
├── Real RAG Pipeline (64-dim vectors, 8 docs)
├── Gist Sync (30min state push)
├── Gist Context (11 lineage fragments in all models)
├── Persistent Memory (3 agents, 12+ memories each)
├── Entropy Monitor (Laplace smoothing + hex spread)
├── Web Dashboard (HTTP server on :8899)
├── Plugin System (5 plugins)
├── Map Guidance (61 hex weights)
├── Perfect Prompts (8 templates)
├── Perfect Patterns (8 routes)
├── Hourly Heartbeat Cron
└── GitHub Integration
```

---

## 🔑 ENVIRONMENT VARIABLES

| Variable | Required | Description |
|----------|----------|-------------|
| `JAVA_HOME` | Yes | JDK 17+ path |
| `GIST_TOKEN` | Yes | GitHub fine-grained token with `gist` scope |
| `OLLAMA_HOST` | No | Default: `http://localhost:11434` |

---

## ⚠️ CRITICAL RULES

1. **NO FXML** — FXML `@FXML` binding silently fails with view switching. Pure programmatic JavaFX only.
2. **Checkpoint before every change** — `git commit + tag + changelog + blueprint + README`
3. **Scientific method** — One variable at a time, tracers for evidence
4. **3-strike rule** — If same fix fails 3×, restore from git and try different architecture
5. **Never delete from GitHub** — Always add and advance only
6. **No code duplication** — Databases always uploaded to gist

---

## 📜 VERSION HISTORY

| Version | Milestone |
|---------|-----------|
| v0.3.0 | Blue grid + nav buttons |
| v0.6.0 | View switching WORKS (StackPane, no FXML) |
| v0.7.0 | Models, chats, stations, entropy, Markov, commands |
| v0.8.0 | Real Ollama API — 4 SLM agents chatting live |
| v0.9.0 | Agent movement + pipelines + GitHub push |
| v0.10.0 | Shared God Chat + editable routing + lexical search |
| v0.11.0 | Web APIs + Model Manager + Voting + Topology + Night Cycle |
| v0.12.0 | 6 models + Evaluation + LoRA + Prompt Engineering + Stats |
| v0.13.0 | Gameplay tab + Headless Pipeline in GUI |
| v0.14.0 | All backend systems (12) |
| v0.15.0 | Next-gen: Real RAG, Fine-Tuning, Multi-Agent Topology, Web Dashboard, Plugins |
| v0.16.0 | SLMs ready: Perfect Prompts, Map Guidance, Patterns, Tools, Memory (17 systems) |
| v0.17.0 | 4D Hex Map: 61 translucent hexes, Q/R/Z + time pulse, FOW-ready |
| v0.18.0 | FOW + Hex TODOs + Gist Context + Gist Sync + Night Cycle + Dream Engine + Role-Based Voting + Dynamic Tool/Station Registry + Deploy Implementation (22 systems) |

---

## 💙 CREDITS

Built by the Architect (chrisalunlloyd2) and Hermes Agent.
Pure JavaFX. No FXML. Everything works.
The system grows itself. Every night. Forever.

<p align="center">
  <i>"A cognitive engine is a distributed, stateless, message-passing organism."</i>
</p>
