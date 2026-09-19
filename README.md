# SIMS1337

> hessian_learning.py — CURVATURE-AWARE LoRA LEARNING (Hessian/Fisher) ==================================================================== "Hessian learning" for local SLMs on constrained hardware (Termux/MatrixWinCE):

*Auto-generated 2026-08-14 12:43 from source — branch `HEAD`, 15 Python modules, 184 other files.*

## Architecture

```
  .gitignore
  BLUEPRINT.md
  BUILD_BLOCKER.md
  CHANGELOG.md
  CHRISSTEPS_QUICK_REF.md
  DEPENDENCIES.md
  HOW_TO_OPEN_GUI.md
  INSTALL_JAVA_MAVEN.bat
  LAUNCH_BOTH.bat
  LAUNCH_GODHAND.bat
  LAUNCH_GUI.ps1
  PHASES.md
  bin/
    com/
      aigen/
  code_registry/
    code_registry.db
    scan_and_register.py
  docs/
    ARCHITECTURE.md
    BACKUP_DOCTRINE.md
    BLUEPRINT.md
    CORE_ROUTING_VALIDATION.md
    CREW_PROCEDURE.md
    FLEXIBLE_MODEL_STRATEGY.md
    LEARNINGS.md
    MARKOV_LOGIC_REVIEW_SYSTEM.md
    MASTER_STATUS_DASHBOARD.md
    MASTER_STATUS_UPDATED.md
    OLLAMA_DOWNLOAD_STATUS.md
    OLLAMA_MODEL_STATUS.md
    screenshots/
      real_gui.jpg
      swarm_hub.jpg
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
    backup.sh
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
      ...
```

## Dependencies

External packages imported by this project:

`node_kv_sync`, `numpy`

## How to run

Executable entry points (have a `__main__` block):

- `python code_registry/scan_and_register.py`
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

### `code_registry/scan_and_register.py`

- `init_db()`
- `get_language(filepath)`
- `scan_and_register(conn)`

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
- `main()` — Main (function).

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
| `scan_and_register` | `get_language` | `get_language(filepath)` |
| `scan_and_register` | `init_db` | `init_db()` |
| `scan_and_register` | `scan_and_register` | `scan_and_register(conn)` |
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

## Status

- Branch: `HEAD`
- Last commit: 2026-08-14 11:12:26 -0600
- File types: .java ×86, .md ×45, .bat ×11, .fxml ×7, .class ×6, .ps1 ×5, .modelfile ×5, .html ×4

### Recent commits
```
31f6215 [Moe autonomous] SIMS1337 2026-08-14 11:12
cd55867 [Moe autonomous] SIMS1337 2026-08-14 08:56
26d8d42 [Moe autonomous] SIMS1337 2026-08-14 04:10
90c1b15 [Moe autonomous] SIMS1337 2026-08-14 01:26
370b37d [Moe autonomous] SIMS1337 2026-08-13 23:24
49f1b73 [Moe autonomous] SIMS1337 2026-08-13 21:56
ad830ff [Moe autonomous] SIMS1337 2026-08-13 20:20
f4322a3 [Moe autonomous] SIMS1337 2026-08-13 19:37
```

---
*README generated by `readme_generator.py` (Viper). Deterministic — derived from source, not LLM prose.*