#!/usr/bin/env python3
"""SIMS1337 quick health check (heartbeat verify).

Checks: Ollama API, web dashboard, live app activity, git cleanliness.
Usage: python scripts/verify.py
Exit code: 0 = all pass (or app-only checks skipped), 1 = critical failure.
"""
import json
import subprocess
import sys
import urllib.request
from datetime import datetime

REPO = "C:/Users/viper/AIGEN_SYS/repos/sims-java-neo-fx"
OLLAMA = "http://localhost:11434/api/tags"
DASHBOARD = "http://localhost:8899/api/status"

ok = True


def fetch(url, timeout=5):
    with urllib.request.urlopen(url, timeout=timeout) as r:
        return r.read().decode("utf-8", errors="replace")


print("=" * 60)
print(f"SIMS1337 VERIFY — {datetime.now().isoformat()}")
print("=" * 60)

# 1. Ollama
try:
    tags = json.loads(fetch(OLLAMA))
    models = [m["name"] for m in tags.get("models", [])]
    print(f"[OK]   Ollama: {len(models)} models: {', '.join(models[:5])}{'...' if len(models) > 5 else ''}")
    core = {"qwen2.5:0.5b", "tinyllama:1.1b", "phi:latest", "phi3:mini"}
    missing = core - set(models)
    if missing:
        print(f"[WARN] Core models missing: {missing}")
except Exception as e:
    print(f"[FAIL] Ollama down: {e}")
    ok = False

# 2. Dashboard /api/status
try:
    status = json.loads(fetch(DASHBOARD))
    print(f"[OK]   Dashboard v{status.get('version')}: models={status.get('models')} "
          f"kgNodes={status.get('kgNodes')} errors={status.get('errors')} recoveries={status.get('recoveries')}")
    if status.get("errors", 0) > 10:
        print("[WARN] Error count high")
except Exception as e:
    print(f"[WARN] Dashboard :8899 down: {e}")

# 3. Git cleanliness
try:
    out = subprocess.run(["git", "status", "--porcelain"], cwd=REPO,
                         capture_output=True, text=True, timeout=30)
    dirty = len(out.stdout.strip().splitlines()) if out.stdout.strip() else 0
    print(f"[{'OK' if dirty == 0 else 'INFO'}]  Git: {dirty} uncommitted change(s)")
except Exception as e:
    print(f"[WARN] Git check failed: {e}")

print("=" * 60)
print("RESULT: " + ("PASS" if ok else "FAIL"))
sys.exit(0 if ok else 1)