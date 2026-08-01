#!/usr/bin/env bash
# ============================================================================
# SIMS1337 DEPENDENCY PROVISIONER — run once, never wonder again
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"
CHECK_ONLY=0
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=1
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok(){ echo -e "${GREEN}✓ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $1${NC}"; }
MISSING=0

echo "=============================================================="
echo "  SIMS1337 Dependency Provisioner | $([ $CHECK_ONLY -eq 1 ] && echo CHECK || echo INSTALL)"
echo "=============================================================="

# [1/4] Java 17 + Maven
echo; echo "[1/4] Java + Maven"
if command -v javac >/dev/null 2>&1; then
  JV=$(javac -version 2>&1 | grep -oE "[0-9]+" | head -1)
  if [ "${JV:-0}" -ge 17 ]; then ok "javac $JV"; else warn "javac $JV — need 17+"; MISSING=1; fi
else warn "javac missing (pkg install openjdk-17)"; MISSING=1; fi
if command -v mvn >/dev/null 2>&1; then ok "maven $(mvn -v 2>/dev/null | head -1 | grep -oE '[0-9.]+' | head -1)"; else warn "maven missing (pkg install maven)"; MISSING=1; fi

# [2/4] Python + numpy
echo; echo "[2/4] Python"
if command -v python3 >/dev/null 2>&1; then ok "python3"; else warn "python3 missing"; MISSING=1; fi
if python3 -c "import numpy" >/dev/null 2>&1; then ok "numpy"; else
  if [ $CHECK_ONLY -eq 1 ]; then warn "numpy missing"; MISSING=1; else
    pip3 install numpy >/dev/null 2>&1 && ok "numpy installed" || warn "pip numpy failed"; MISSING=1
  fi
fi

# [3/4] Model
echo; echo "[3/4] Models"
if ls modelfiles/*.gguf >/dev/null 2>&1; then ok "model(s) in modelfiles/"; else
  if [ $CHECK_ONLY -eq 1 ]; then warn "no models in modelfiles/"; MISSING=1; else
    echo "  fetching model via scripts/download_gguf.py..."
    python3 scripts/download_gguf.py >/dev/null 2>&1 && ok "model downloaded" || warn "model fetch failed (run scripts/download_gguf.py manually)"; MISSING=1
  fi
fi

# [4/4] Maven deps offline cache
echo; echo "[4/4] Maven dependency cache"
if [ -d "$HOME/.m2/repository" ] && [ "$(find "$HOME/.m2/repository" -name '*.jar' 2>/dev/null | wc -l)" -gt 10 ]; then
  ok "m2 cache populated"
else
  if [ $CHECK_ONLY -eq 1 ]; then warn "m2 cache empty"; MISSING=1; else
    echo "  running mvn -q dependency:go-offline (first run downloads ~100MB)..."
    mvn -q dependency:go-offline >/dev/null 2>&1 && ok "m2 cache primed" || warn "go-offline failed — check network"
  fi
fi

echo
if [ $MISSING -eq 1 ]; then echo "⚠ Missing pieces — re-run WITHOUT --check to install."; exit 1; fi
echo "✅ ALL PRESENT — build with: mvn -q compile"
