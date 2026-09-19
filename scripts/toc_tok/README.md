# 🗺️ TOC-TOK Tree + Onboarding — seamless SLM orientation

## TOC-TOK (Table of Contents → Tree of Knowledge)
A hex-anchored knowledge tree. Projects/phases/tasks/knowledge are nodes;
every node anchors to a hex coordinate. Models navigate by tree path AND by
space (FOW). "Tree + map = orientation."

```
root (0,0)
└── projects/
    ├── SIMS1337 (2,0) ── phases/phase4-agents (2,1)
    │                    └── knowledge/markov-voting (2,2)
    ├── api-orchestrator (-2,1)
    └── MatrixWinCE (3,0)
└── tasks/deploy-tonight (2,1)
└── knowledge/quantization-guide (-4,0)
```

## Onboarding (onboard.py)
Generates a boarding pass for any SLM entering the matrix:
position + FOW visibility + knowledge + anchored tasks + continuity.
```bash
python3 onboard.py --model qwen2.5:0.5b --hex 2,1 --role phase4-agents \
    --mission "Verify FOW-aware" --continuity chain_decisions.jsonl
```

## Commands
```bash
python3 toc_tok.py tree          # full tree
python3 toc_tok.py at 2,1        # what lives at/near hex
python3 toc_tok.py search markov # keyword search
python3 toc_tok.py add /path --hex 2,1 --type project --tags "a,b"
```

## Continuity loop
1. Models decide via Markov chain → chain_decisions.jsonl
2. lstm_refractor.py learns temporal priors
3. Next spawn's boarding pass includes last decisions
4. SLMs never spawn amnesiac — they inherit context

## batch_termux integration
`python-cascade/onboarding/injector.py` prepends the boarding pass to every
model invocation in the batch terminals (see batch_termux README).
