# SIMS1337 — Dependencies (single source of truth)
> Every dependency, one place. `scripts/setup_deps.sh` installs all of them.

## One command (Termux / Linux)
```bash
bash scripts/setup_deps.sh          # install everything
bash scripts/setup_deps.sh --check  # report what's missing (safe)
```

## Java (GodHand GUI + agents)
| Tool | Install | Used for |
|------|---------|----------|
| JDK 17 | Termux: `pkg install openjdk-17` · Ubuntu: `apt install openjdk-17-jdk` · Windows: INSTALL_JAVA_MAVEN.bat | compile |
| Maven 3.9+ | Termux: `pkg install maven` · Ubuntu: `apt install maven` | deps + build |

> **Java 17 exactly** — pom.xml targets `maven.compiler.source/target=17`,
> JavaFX 17.0.6. Do NOT use 8/11/21 without editing pom.xml.

Maven pulls the rest automatically (JavaFX, Jackson, HttpClient5,
Java-WebSocket, slf4j/logback, JUnit 5, Mockito, AssertJ).

## Python (tooling scripts — numpy only, rest is stdlib)
| Script | Deps |
|--------|------|
| scripts/toc_tok/*.py | stdlib |
| scripts/train_lora.py, lora/hessian_learning.py | numpy |
| scripts/chat_server.py, lstm_refractor.py, download_gguf.py | stdlib |
| root-scripts/heartbeat_harvester.py | stdlib |

```bash
pip3 install numpy
```

## Runtime models
| Piece | Path | Notes |
|-------|------|-------|
| GGUF models | `modelfiles/*.gguf` | download: `python3 scripts/download_gguf.py` |
| GGUF server | `:5000` (house format) | MatrixWinCE gguf_server_v2.py, strict mode |
| Ollama (optional) | `ollama serve` | alternative model source |

## Cross-repo expectations
| Repo | Needed for | Optional? |
|------|-----------|-----------|
| MatrixWinCE | GGUF server + scheduler (same house format :5000) | no for full pipeline |
| batch_termux | onboarding injector (boarding pass) | yes |

## Verification
```bash
bash scripts/setup_deps.sh --check    # all ✓ ?
mvn -q compile                        # Java compiles
python3 -m pytest lora/test_hessian_learning.py   # or: python3 lora/test_hessian_learning.py
```
