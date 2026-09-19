#!/usr/bin/env python3
"""
train_lora.py — LoRA adapter training for local SLMs (Ollama + llama.cpp style).
Takes a JSONL dataset of {instruction, output} pairs, builds a Modelfile with
a LoRA adapter, and registers it with Ollama as <base>-lora-<tag>.

Uses the flexible model strategy: base model just needs to be competent;
the LoRA adapter provides specialization (CHAT, CODE, PATHFIND, ...).

Usage:
  python3 train_lora.py --base qwen2.5:0.5b --tag chat --data chat_data.jsonl
"""
import argparse, json, os, subprocess, sys, tempfile

def check_ollama():
    try:
        subprocess.run(["ollama", "--version"], capture_output=True, check=True)
        return True
    except Exception:
        return False

def build_modelfile(base_model, adapter_path):
    return f"FROM {base_model}\nADAPTER {adapter_path}\n"

def main():
    p = argparse.ArgumentParser(description="Train a LoRA adapter for a local SLM")
    p.add_argument("--base", required=True, help="base model, e.g. qwen2.5:0.5b")
    p.add_argument("--tag", required=True, help="adapter tag, e.g. chat, code, pathfind")
    p.add_argument("--data", required=True, help="JSONL dataset: {instruction, output}")
    p.add_argument("--epochs", type=int, default=3)
    p.add_argument("--dry-run", action="store_true", help="show commands without running")
    p.add_argument("--hessian", action="store_true",
                   help="emit curvature-aware LoRA policy before training "
                        "(Fisher rank allocation + EWC overwrite warning)")
    args = p.parse_args()

    # 1. Validate dataset
    rows = []
    with open(args.data) as f:
        for line in f:
            line = line.strip()
            if not line: continue
            d = json.loads(line)
            if "instruction" in d and "output" in d:
                rows.append(d)
    print(f"[train_lora] dataset: {len(rows)} examples")
    if not rows:
        sys.exit("no valid examples (need instruction+output)")
    if len(rows) < 10:
        print("[train_lora] WARNING: <10 examples — adapter quality will be poor")

    # 2. Adapt dataset for the available trainer (here: llama.cpp finetune format)
    train_file = "lora_train.jsonl"
    with open(train_file, "w") as f:
        for r in rows:
            f.write(json.dumps({"text": f"### Instruction:\n{r['instruction']}\n\n### Response:\n{r['output']}"}) + "\n")
    print(f"[train_lora] wrote {train_file}")

    # 3a. Hessian-aware policy (curvature-aware LoRA learning)
    if args.hessian:
        try:
            sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "lora"))
            from hessian_learning import adaptive_rank_allocation, ewc_cost, MLP, fisher_diagonal
            import numpy as np
            # build a tiny surrogate MLP shaped like the adapter's layers
            # (in->h->out guess; the real per-layer Fisher needs the trainer,
            #  this emits the POLICY + rank budget the trainer should follow)
            n_in = max(len(r["instruction"].split()) for r in rows[:50]) + 8
            n_out = max(len(r["output"].split()) for r in rows[:50]) + 8
            sizes = [n_in, max(32, n_in // 2), max(16, n_out // 2), n_out]
            model = MLP(sizes, seed=0)
            rng = np.random.default_rng(0)
            X = rng.standard_normal((min(len(rows), 32), n_in))
            Y = rng.standard_normal((min(len(rows), 32), n_out))
            F = fisher_diagonal(model, X, Y, n_samples=min(8, len(rows)))
            ranks = adaptive_rank_allocation(F, sizes, budget=8)
            policy = {"base": args.base, "tag": args.tag, "layers": sizes,
                      "rank_budget": 8, "rank_allocation": ranks,
                      "policy": "LEARN where flat, PRESERVE where curved"}
            with open(f"lora_hessian_policy-{args.tag}.json", "w") as pf:
                json.dump(policy, pf, indent=2)
            print(f"[train_lora] hessian policy -> lora_hessian_policy-{args.tag}.json")
            print(f"[train_lora]   rank allocation: {ranks}")
            if os.path.exists(f"lora-{args.tag}.gguf"):
                print("[train_lora]   WARNING: existing adapter will be overwritten — "
                      "EWC says: preserve high-curvature knowledge, only add.")
        except Exception as e:
            print(f"[train_lora] hessian policy skipped ({e})")

    # 3. Trainer invocation — llama.cpp finetune (finetune) or mlx-lm
    #    Adapt this to whatever trainer is installed on the target device.
    trainer = os.environ.get("LORA_TRAINER", "llama.cpp")
    lora_out = f"lora-{args.tag}.gguf"
    if trainer == "llama.cpp":
        cmd = [
            "finetune",
            "--model-base", args.base.replace(":", "-"),
            "--train-data", train_file,
            "--lora-out", lora_out,
            "--epochs", str(args.epochs),
        ]
    else:
        sys.exit(f"unknown trainer: {trainer}")

    print(f"[train_lora] training command: {' '.join(cmd)}")
    if args.dry_run:
        print("[train_lora] dry-run — not executing")
        return
    if not os.path.exists(cmd[0]) and not any(os.access(p + "/" + cmd[0], os.X_OK) for p in os.environ.get("PATH", "").split(":")):
        print(f"[train_lora] trainer '{cmd[0]}' not found — install it, then re-run. Skipping train.")

    # 4. Register with Ollama
    if check_ollama() and os.path.exists(lora_out):
        model_name = f"{args.base}-lora-{args.tag}"
        modelfile = build_modelfile(args.base, lora_out)
        mf_path = os.path.join(tempfile.gettempdir(), f"Modelfile-{args.tag}")
        with open(mf_path, "w") as f:
            f.write(modelfile)
        print(f"[train_lora] registering {model_name} with Ollama")
        subprocess.run(["ollama", "create", model_name, "-f", mf_path], check=True)
        print(f"[train_lora] DONE: {model_name}")
    else:
        print("[train_lora] adapter file not produced yet — run trainer, then register manually")

if __name__ == "__main__":
    main()
