<<<<<<< HEAD
# ⬡ SIMS1337 — Breathing Hexeract Local Agent Grid

![SIMS1337](SIMS1337_HEADER.png)


```
   ╔═══════════════════════════════════════════════════════════════════╗
   ║   _____ _____ __  __ _____ __ __________ _____                  ║
   ║  / ___//  _/ |  \/  / ___// /|__  /__  //__  /                  ║
   ║  \__ \ / /   | |\/| \__ \/ /   / /  / /   / /                   ║
   ║ ___/ // /    | |  | |__/ / /   / /  / /   / /                   ║
   ║/____/___/    |_|  |_/____/_/  /_/  /_/   /_/                    ║
   ║                                                                  ║
   ║  HEXERACT LOCAL AGENT GRID — 6D HYPERCUBE STATE ENGINE          ║
   ╚═══════════════════════════════════════════════════════════════════╝
```

> **Never delete, only add and merge. Nothing lives forever, nothing runs for free.**
> **Always advancing, always progressing.**

---

## ⬡ What is SIMS1337?

A **decentralized local desktop AI agent grid** running Small Language Models (SLMs) through Ollama,
orchestrated across a **6-Dimensional Hypercube (Hexeract) state space topology** with:

- **Viscoelastic heartbeat dynamics** — the system *breathes* using non-Newtonian fluid mechanics
- **Quorum consensus** — ⅔ supermajority (43/64 vertices) using topological homology
- **Gossip propagation** — anti-entropy state exchange across 192 edges
- **Carreau-Yasuda shear thinning** — auto-scaling under load (η drops when strain rate ↑)
- **Byzantine fault tolerance** — survives up to 21 unresponsive nodes

---

## ⬡ ASCII Topological File Tree

```
local_desktop_main/                         ← Project Root (Git)
│
├── pom.xml                                 ← Maven Build (Java 17, JavaFX)
├── README.md                               ← This file
├── BLUEPRINT.md                            ← Architecture & Data Flow
├── CHANGELOG.md                            ← Version history
│
├── src/
│   ├── main/
│   │   ├── java/com/aigen/sims/
│   │   │   ├── GodHandApp.java             ← JavaFX Main GUI (GodHand Dashboard)
│   │   │   ├── HeadlessPipeline.java        ← Headless agent pipeline runner
│   │   │   ├── AutoLoop.java               ← Autonomous loop controller
│   │   │   ├── agent/
│   │   │   │   ├── OllamaClient.java        ← Ollama HTTP API client
│   │   │   │   ├── AgentNode.java           ← SLM agent instance
│   │   │   │   ├── AgentGrid.java           ← 64-vertex hexeract grid
│   │   │   │   └── Heartbeat.java           ← Viscoelastic breath cycle
│   │   │   ├── bridge/
│   │   │   │   └── BruteMiner.java          ← Code-mining brute force engine
│   │   │   ├── deploy/
│   │   │   │   ├── DeployOrchestrator.java  ← Deploy cycle orchestrator
│   │   │   │   ├── GateKeeper.java          ← Multi-level gate approval
│   │   │   │   └── GitBackupManager.java    ← Git snapshot & backup
│   │   │   ├── gui/
│   │   │   │   ├── AegisSwingSphere.java    ← 3D Rotating Sphere GUI (Swing)
│   │   │   │   ├── GuiGardener.java         ← Self-modifying GUI proposals
│   │   │   │   └── ComponentProposal.java   ← UI component evolution
│   │   │   ├── lora/
│   │   │   │   ├── LoRAAdapter.java         ← LoRA adapter definitions
│   │   │   │   ├── LoRATuner.java           ← Performance-based LoRA tuning
│   │   │   │   └── AdapterRegistry.java     ← Adapter election & voting
│   │   │   ├── mining/
│   │   │   │   ├── CodeMiner.java           ← Repository code mining
│   │   │   │   ├── CodeMinerOrchestrator.java ← Mining pipeline controller
│   │   │   │   ├── Suggestion.java          ← Code improvement suggestions
│   │   │   │   └── SuggestionRegistry.java  ← Suggestion tracking & status
│   │   │   ├── phase1/
│   │   │   │   ├── HexCoord.java            ← Hex grid coordinate system
│   │   │   │   ├── FOWGate.java             ← Fog-of-War attention gate
│   │   │   │   └── QuorumVoting.java        ← Phase 1 quorum consensus
│   │   │   ├── routing/
│   │   │   │   ├── ModelRouter.java         ← SLM model router (complexity-based)
│   │   │   │   ├── ModelPool.java           ← Model pool manager
│   │   │   │   ├── LoRASwitcher.java        ← Dynamic LoRA adapter switching
│   │   │   │   └── TaskQueue.java           ← Prioritized task queue
│   │   │   ├── scheduler/
│   │   │   │   ├── PipelineScheduler.java   ← Pipeline scheduling engine
│   │   │   │   └── EventBus.java            ← Internal event messaging
│   │   │   ├── tasks/
│   │   │   │   ├── Task.java                ← Task definitions
│   │   │   │   ├── Complexity.java          ← Task complexity classification
│   │   │   │   └── TaskStatus.java          ← Task status enum
│   │   │   ├── voting/
│   │   │   │   ├── WeightedQuorumVote.java  ← Weighted quorum consensus
│   │   │   │   └── HexCoord.java            ← Hex coordinate for voting
│   │   │   └── web/
│   │   │       └── WebDashboard.java        ← Embedded web server (Javalin)
│   │   └── resources/fxml/
│   │       ├── GodHand.fxml                 ← GodHand dashboard layout
│   │       ├── PlayerGrid.fxml              ← 2D player grid view
│   │       ├── PlayerGrid3D.fxml            ← 3D player grid view
│   │       ├── RealPlayerGrid.fxml          ← Live player grid
│   │       ├── TabbedGodHand.fxml           ← Tabbed GodHand layout
│   │       └── UnifiedBackend.fxml          ← Backend management view
│   └── test/java/com/aigen/sims/
│       ├── bridge/BruteMinerTest.java
│       ├── deploy/DeployTest.java
│       ├── gui/GuiGardenerTest.java
│       ├── lora/LoRATest.java
│       ├── mining/CodeMinerTest.java
│       ├── phase1/TestQuorumVoting.java
│       ├── routing/
│       │   ├── LoRASwitcherTest.java
│       │   ├── ModelPoolTest.java
│       │   ├── ModelRouterTest.java
│       │   └── TaskQueueTest.java
│       └── voting/WeightedQuorumVoteTest.java
│
├── web/                                     ← Web Frontend & Server
│   ├── server.js                            ← Express.js backend (port 8080)
│   ├── package.json                         ← Node.js dependencies
│   ├── index.html                           ← RISC Manifold 3D web GUI
│   └── hexeract-gui/
│       └── index.html                       ← ⬡ BREATHING HEXERACT GUI ⬡
│
├── scripts/                                 ← Utility scripts
│   ├── agents/                              ← Agent messenger & crew loop
│   ├── toc_tok/                             ← TOC/TOK editor
│   ├── build_llamacpp.py                    ← llama.cpp build helper
│   ├── chat_server.py                       ← Python chat server
│   ├── download_gguf.py                     ← GGUF model downloader
│   ├── lstm_refractor.py                    ← LSTM refactoring utility
│   └── train_lora.py                        ← LoRA training script
│
├── lora/                                    ← LoRA / Hessian learning
│   ├── hessian_learning.py                  ← Hessian-based LoRA learning
│   └── test_hessian_learning.py             ← Hessian tests
│
├── code_registry/                           ← NEVER-MAKE-CODE-TWICE DB
│   ├── code_registry.db                     ← SQLite database (110 pages)
│   └── scan_and_register.py                 ← Scanner/registrar script
│
├── build/                                   ← Compiled Java classes
├── target/                                  ← Maven build output
└── suggestions/                             ← Code improvement proposals
```

---

## ⬡ Hexeract State Space — 6 Dimensions

```
  ┌──────────────────────────────────────────────────────────────┐
  │  AXIS   │  METRIC              │  DESCRIPTION               │
  ├─────────┼──────────────────────┼────────────────────────────┤
  │  x₁     │  Entity Class        │  MODEL, SERVER, PID, REPO  │
  │  x₂     │  Temporal (t)        │  Past ← NOW → Future       │
  │  x₃     │  Comp. Load (η)      │  CPU/thread viscosity       │
  │  x₄     │  Code Delta (ΔC)     │  Repo state drift           │
  │  x₅     │  Consensus (Wᵥ)      │  Quorum rank & reputation   │
  │  x₆     │  Topology Shard      │  Network routing cluster    │
  └──────────────────────────────────────────────────────────────┘

  Combinatorics:
    Vertices     N₀ = 2⁶       = 64
    Edges        N₁ = 2⁵·C(6,1) = 192
    Square Faces N₂ = 2⁴·C(6,2) = 240
    Cubic Cells  N₃ = 2³·C(6,3) = 160
    Tesseracts   N₄ = 2²·C(6,4) = 60
    5-Cube Facets N₅ = 2¹·C(6,5) = 12
```

---

## ⬡ Viscoelastic Heartbeat (Breathing)

```
  ψ(x,t) = A · sin(ωt - k·x) · e^(-η/ρ · t)

  ┌─────────────────────────────────────────────────────┐
  │  INHALE (∂ψ/∂t > 0)                                │
  │  ► Expansion & gossip dispersal                     │
  │  ► Viscosity drops to η_min                         │
  │  ► 192 edges dilate for state streaming             │
  │  ► PIDs broadcast across peripheral k-faces         │
  ├─────────────────────────────────────────────────────┤
  │  EXHALE (∂ψ/∂t < 0)                                │
  │  ► Compression & quorum settlement                  │
  │  ► Dynamic pressure rises to τ_yield                │
  │  ► Consensus weights calculated                     │
  │  ► Code state solidified & committed                │
  └─────────────────────────────────────────────────────┘

  Carreau-Yasuda Shear Thinning:
    η(γ̇) = η∞ + (η₀ - η∞) · (1 + (λγ̇)²)^((n-1)/2)
    n < 1 → viscosity DROPS under high-throughput gossip surges
```

---

## ⬡ Quorum Consensus (Topological Homology)

```
  Threshold: |Q| ≥ ⌈(2·64 + 1)/3⌉ = 43 vertices

  Byzantine Fault Tolerance: f < 22 unresponsive nodes
  
  Betti Number β₁ = 0 → Every closed gossip cycle is the
  boundary of a higher-dimensional face. No partition deadlock.
  
  Commit Rule: ‖q‖₁ ≥ 43 AND δ⁰q* = 0 on active sub-complex Q
```

---

## ⬡ Running the System

### Breathing Hexeract Web GUI
```bash
cd web && npm install && node server.js
# Open http://localhost:8080
```

### Java Desktop GUI (AegisSwingSphere)
```bash
# Compile
javac -d build src/main/java/com/aigen/sims/gui/AegisSwingSphere.java

# Launch
java -cp build com.aigen.sims.gui.AegisSwingSphere
```

### Java GodHand Dashboard (Maven/JavaFX)
```bash
mvn compile
mvn javafx:run
```

### Ollama Models (37 loaded)
```
qwen2.5:3b          qwen2.5-coder:3b    phi3:mini
gemma2:2b            codellama:7b        deepseek-r1:1.5b
llama3.2:1b          tinyllama:1.1b      smollm2:360m
qwen2.5:0.5b         qwen3:4b            qwen3:latest
+ 25 more including abliterated variants, vision models, custom Modelfiles
```

---

## ⬡ Tech Stack

| Layer | Technology |
|---|---|
| Desktop GUI | Java 21, Swing, JavaFX 17 |
| Web GUI | HTML5 Canvas, Three.js, CSS glassmorphism |
| Web Server | Node.js v24.14.1, Express.js |
| SLM Runtime | Ollama (37 models, mmap to SSD) |
| Build | Maven 3.9.16 |
| Database | SQLite (code_registry.db — 110 pages) |
| Consensus | Hexeract quorum (⅔ threshold) |
| Version Control | Git, GitHub (chrisalunlloyd2-sudo) |

---

## ⬡ License & Philosophy

> Nothing lives forever, nothing runs for free.
> Always advancing, always progressing.
> Never delete, only add and merge.
## White Paper

Full technical white paper: [SIMS1337_White_Paper.md](SIMS1337_White_Paper.md)
=======
# SIMS1337

> hessian_learning.py — CURVATURE-AWARE LoRA LEARNING (Hessian/Fisher) ==================================================================== "Hessian learning" for local SLMs on constrained hardware (Termux/MatrixWinCE):

*Auto-generated 2026-08-03 02:40 from source — branch `?`, 14 Python modules, 160 other files.*

## Architecture

```
  .gitignore
  CHANGELOG.md
  CHRISSTEPS_QUICK_REF.md
  DEPENDENCIES.md
  HOW_TO_OPEN_GUI.md
  INSTALL_JAVA_MAVEN.bat
  LAUNCH_BOTH.bat
  LAUNCH_GODHAND.bat
  PHASES.md
  README.md
  RUN_GUI.bat
  RUN_PLAYERGRID.bat
  bin/
    com/
      aigen/
  docs/
    ARCHITECTURE.md
    BLUEPRINT.md
    CORE_ROUTING_VALIDATION.md
    CREW_PROCEDURE.md
    FLEXIBLE_MODEL_STRATEGY.md
    MARKOV_LOGIC_REVIEW_SYSTEM.md
    MASTER_STATUS_DASHBOARD.md
    MASTER_STATUS_UPDATED.md
    OLLAMA_DOWNLOAD_STATUS.md
    OLLAMA_MODEL_STATUS.md
    OPTIMAL_MODEL_CONFIG.md
    PHASE1_COMPLETE.md
  logs/
    change_log_20260722_162505.md
    checkpoint_20260722_171210.md
    guardian.log
    headless.log
    milestone_20260722_172002.md
    release_20260722_172546.md
    release_20260722_172812.md
    release_20260722_173243.md
  lora/
    hessian_learning.py
    test_hessian_learning.py
  modelfiles/
    codellama-16k.Modelfile
    gemma2-16k.Modelfile
    phi-8k.Modelfile
    phi3-16k.Modelfile
    tinyllama-8k.Modelfile
  reports/
    heartbeat_votes.md
    model_needs_votes.md
  root-scripts/
    heartbeat_harvester.py
  scripts/
    build_llamacpp.py
    chat_server.py
    download_gguf.py
    lstm_refractor.py
    mmf_reader.ps1
    mmf_writer.ps1
    pipe_listener.ps1
    service.bat
    setup_deps.sh
    train_lora.py
    tts_readout.py
    agents/
      agent_messenger.py
      crew_loop.py
    toc_tok/
      .gitignore
      README.md
      editor.html
      editor.py
      onboard.py
      ...
```

## Dependencies

External packages imported by this project:

`node_kv_sync`, `numpy`

## How to run

Executable entry points (have a `__main__` block):

- `python lora/hessian_learning.py`
- `python root-scripts/heartbeat_harvester.py`
- `python scripts/build_llamacpp.py`
- `python scripts/chat_server.py`
- `python scripts/download_gguf.py`
- `python scripts/lstm_refractor.py`
- `python scripts/toc_tok/editor.py`
- `python scripts/toc_tok/onboard.py`
- `python scripts/toc_tok/toc_tok.py`
- `python scripts/train_lora.py`
- `python scripts/tts_readout.py`

## Modules

### `lora/hessian_learning.py`

hessian_learning.py — CURVATURE-AWARE LoRA LEARNING (Hessian/Fisher)
====================================================================
"Hessian learning" for local SLMs on constrained hardware (Termux/MatrixWinCE):

- **class `MLP`** — Minimal numpy MLP — the adapter's effective function. Layers = [in, h1, ..., out].
  - methods: `params`, `forward`, `grads`, `_finite_diff_grads`
- `fisher_diagonal(model, X, Y, n_samples)` — Empirical Fisher diagonal: average of squared per-sample gradients.
- `fisher_to_dict(fisher, prefix)` — Serialize fisher (list of arrays) to a dict for np.savez.
- `ewc_cost(fisher, prev_params, new_params)` — EWC consolidation cost: Σ 0.5·F_i·(θ_i − θ*_i)². High = forgetting.
- `curvature_scaled_delta(delta, fisher, lam)` — Δθ_i ← Δθ_i / (1 + F_i/λ). High-curvature params move slowly.
- `adaptive_rank_allocation(fisher, layers, budget)` — Allocate LoRA rank across layers ∝ curvature mass. Budget = total rank.
- `curvature_gate(fisher, prev_params, proposed_delta, threshold)` — Gate a proposed adapter delta: if EWC cost of applying it exceeds
- `demo()`
- `main()`

### `lora/test_hessian_learning.py`

Tests for hessian_learning.py — verify the math is real, not vibes.

- `check(name, cond)`
- `locate(idx)`

### `root-scripts/heartbeat_harvester.py`

Heartbeat Harvester — correlates old project ideas to current active projects.

- `load_kv()`
- `load_kg()`
- `get_keywords()`
- `get_insights()`
- `correlate()`
- `main()`

### `scripts/agents/agent_messenger.py`

agent_messenger.py — REAL inter-agent messaging via the house GGUF server
==========================================================================
No mocks. No simulations. Agents talk through the real GGUF server
(:5000, house format) backed by a real llama.cpp model.

- **class `AgentMemory`** — Real append-only memory per agent: memory/agent_<name>.jsonl
  - methods: `append`, `recent`, `count`
- `healthz()`
- `generate(prompt, max_tokens)` — Real call to the GGUF server. Raises on any failure — never fabricates.
- `build_prompt(agent_name, role, task, memory)` — Real context: role + last memory turns + task. No hallucinated history.
- `extract_java(text)` — Pull the first ```java ... ``` block; return None if absent.

### `scripts/agents/crew_loop.py`

crew_loop.py — REAL 4-agent crew through the house GGUF server
================================================================
4 tiny models (one shared server, relayed — never parallel, doctrine).
Each agent: real memory (disk) → real prompt → real POST :5000 →
real tokens → memory append. If the server is down, we STOP. No mocks.

- `run_round(memories, round_no)`
- `main()`

### `scripts/build_llamacpp.py`

PHASE 22: Native llama.cpp build + SmolLM-135M fallback
Builds llama.cpp from source on Windows (no prebuilt bionic binary).
Downloads SmolLM-135M Q4_K_M GGUF as fallback model.

- `run(cmd, cwd)`
- `build_llamacpp()` — Build llama.cpp from source using cmake + MSVC or MinGW.
- `download_smollm()` — Download SmolLM-135M Q4_K_M GGUF (~105MB).

### `scripts/chat_server.py`

chat_server.py — minimal HTTP chat server for the fleet.
Exposes local models (Ollama :11434 or GGUF :5000) as an OpenAI-style
/v1/chat/completions endpoint so the desktop/web UI can talk to SLMs.

- **class `ChatHandler`**
  - methods: `log_message`, `_send`, `do_GET`, `do_POST`
- `main()`

### `scripts/download_gguf.py`

PHASE 13: Q4_K_M GGUF Downloader from HuggingFace
Downloads quantized GGUF models for Ollama — Q4_K_M is the sweet spot:
~4-bit quantization, 4-5 tok/s on CPU, 4-6GB RAM for 7B models.

- `check_ollama()`
- `download_gguf(model_key)`
- `main()`

### `scripts/lstm_refractor.py`

lstm_refractor.py — sequence-pattern refractor for the fleet's decision logs.
Reads call/decision logs (JSONL), converts them into fixed-length token
sequences, and (if numpy is available) trains a tiny LSTM-ish transition model
so the Markov chain can learn temporal patterns beyond first-order.

- `tokenize_entry(e)` — Map a log entry to discrete tokens for sequence learning.
- `main()`

### `scripts/toc_tok/editor.py`

editor.py — TOC-TOK GUI editor (hex map + tree panel)

- **class `Handler`**
  - methods: `log_message`, `_send`, `_body`, `do_GET`, `do_POST`
- `main()`

### `scripts/toc_tok/onboard.py`

onboard.py — SLM ONBOARDING BOARDING PASS

- `hex_distance(a, b)`
- `one_hop(q, r)`
- `read_continuity(path, limit)`
- `claim_hex(hex_str, agent, model)` — Record who is occupying a hex (prevents two agents working same cell).
- `release_hex(hex_str)`
- `verify_pass(board_id)` — Onboarding verification poll: agent confirms it received the pass.
- `sync_tree(toc_file)` — Auto-update the tree's 'last_onboarded' field after onboarding.
- `build_pass(model, hex_str, role, mission, toc_file, continuity_file, board_id)`
- `main()`

### `scripts/toc_tok/toc_tok.py`

toc_tok.py — TOC-TOK Tree (Table of Contents → Tree of Knowledge)

- `load(path)`
- `save(tree, path)` — Persist tree, then auto-sync to SOV KV (gist/KV updates from nodes).
- `add_node(tree, path, meta)`
- `find_by_hex(tree, target_hex)` — Return nodes anchored at or within 1-hop of a hex.
- `search(tree, query)`
- `get_path(tree, path)`
- `subtree(node, depth, buf)`
- `cmd_init(a)`
- `cmd_add(a)`
- `cmd_tree(a)`
- `cmd_at(a)`
- `cmd_search(a)`
- `cmd_path(a)`
- `main()`

### `scripts/train_lora.py`

train_lora.py — LoRA adapter training for local SLMs (Ollama + llama.cpp style).
Takes a JSONL dataset of {instruction, output} pairs, builds a Modelfile with
a LoRA adapter, and registers it with Ollama as <base>-lora-<tag>.

- `check_ollama()`
- `build_modelfile(base_model, adapter_path)`
- `main()`

### `scripts/tts_readout.py`

TTS readout — speaks text via Windows SAPI (no API key needed).

- `speak(text)`

## Public API index

| Module | Function | Signature |
|--------|----------|-----------|
| `agent_messenger` | `build_prompt` | `build_prompt(agent_name, role, task, memory)` |
| `agent_messenger` | `extract_java` | `extract_java(text)` |
| `agent_messenger` | `generate` | `generate(prompt, max_tokens)` |
| `agent_messenger` | `healthz` | `healthz()` |
| `build_llamacpp` | `build_llamacpp` | `build_llamacpp()` |
| `build_llamacpp` | `download_smollm` | `download_smollm()` |
| `build_llamacpp` | `run` | `run(cmd, cwd)` |
| `chat_server` | `main` | `main()` |
| `crew_loop` | `main` | `main()` |
| `crew_loop` | `run_round` | `run_round(memories, round_no)` |
| `download_gguf` | `check_ollama` | `check_ollama()` |
| `download_gguf` | `download_gguf` | `download_gguf(model_key)` |
| `download_gguf` | `main` | `main()` |
| `editor` | `main` | `main()` |
| `heartbeat_harvester` | `correlate` | `correlate()` |
| `heartbeat_harvester` | `get_insights` | `get_insights()` |
| `heartbeat_harvester` | `get_keywords` | `get_keywords()` |
| `heartbeat_harvester` | `load_kg` | `load_kg()` |
| `heartbeat_harvester` | `load_kv` | `load_kv()` |
| `heartbeat_harvester` | `main` | `main()` |
| `hessian_learning` | `adaptive_rank_allocation` | `adaptive_rank_allocation(fisher, layers, budget)` |
| `hessian_learning` | `curvature_gate` | `curvature_gate(fisher, prev_params, proposed_delta, threshold)` |
| `hessian_learning` | `curvature_scaled_delta` | `curvature_scaled_delta(delta, fisher, lam)` |
| `hessian_learning` | `demo` | `demo()` |
| `hessian_learning` | `ewc_cost` | `ewc_cost(fisher, prev_params, new_params)` |
| `hessian_learning` | `fisher_diagonal` | `fisher_diagonal(model, X, Y, n_samples)` |
| `hessian_learning` | `fisher_to_dict` | `fisher_to_dict(fisher, prefix)` |
| `hessian_learning` | `main` | `main()` |
| `lstm_refractor` | `main` | `main()` |
| `lstm_refractor` | `tokenize_entry` | `tokenize_entry(e)` |
| `onboard` | `build_pass` | `build_pass(model, hex_str, role, mission, toc_file, continuity_file, board_id)` |
| `onboard` | `claim_hex` | `claim_hex(hex_str, agent, model)` |
| `onboard` | `hex_distance` | `hex_distance(a, b)` |
| `onboard` | `main` | `main()` |
| `onboard` | `one_hop` | `one_hop(q, r)` |
| `onboard` | `read_continuity` | `read_continuity(path, limit)` |
| `onboard` | `release_hex` | `release_hex(hex_str)` |
| `onboard` | `sync_tree` | `sync_tree(toc_file)` |
| `onboard` | `verify_pass` | `verify_pass(board_id)` |
| `test_hessian_learning` | `check` | `check(name, cond)` |
| `test_hessian_learning` | `locate` | `locate(idx)` |
| `toc_tok` | `add_node` | `add_node(tree, path, meta)` |
| `toc_tok` | `cmd_add` | `cmd_add(a)` |
| `toc_tok` | `cmd_at` | `cmd_at(a)` |
| `toc_tok` | `cmd_init` | `cmd_init(a)` |
| `toc_tok` | `cmd_path` | `cmd_path(a)` |
| `toc_tok` | `cmd_search` | `cmd_search(a)` |
| `toc_tok` | `cmd_tree` | `cmd_tree(a)` |
| `toc_tok` | `find_by_hex` | `find_by_hex(tree, target_hex)` |
| `toc_tok` | `get_path` | `get_path(tree, path)` |
| `toc_tok` | `load` | `load(path)` |
| `toc_tok` | `main` | `main()` |
| `toc_tok` | `save` | `save(tree, path)` |
| `toc_tok` | `search` | `search(tree, query)` |
| `toc_tok` | `subtree` | `subtree(node, depth, buf)` |
| `train_lora` | `build_modelfile` | `build_modelfile(base_model, adapter_path)` |
| `train_lora` | `check_ollama` | `check_ollama()` |
| `train_lora` | `main` | `main()` |
| `tts_readout` | `speak` | `speak(text)` |

## Status

- Branch: `?`
- Last commit: n/a
- File types: .java ×83, .md ×37, .bat ×11, .fxml ×7, .class ×6, .modelfile ×5, .ps1 ×4, .log ×2

---
*README generated by `readme_generator.py` (Viper). Deterministic — derived from source, not LLM prose.*
>>>>>>> c4e3e28 (docs(SIMS1337): autonomous update — 1 file(s))
