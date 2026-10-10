#!/bin/bash
# run_stress.sh - drive OllamaRouterStressHarness phases against live Ollama.
# Card t_4a3756ed: serialization proof + reject/timeout + RAM guard + /v1/run.
# One phase per JVM (clean router statics). Usage: bash run_stress.sh [phase ...]
set -u
cd "$(dirname "$0")"

CP="C:/Users/viper/AppData/Local/Temp/orch_compile"
HARNESS=com.aigen.sims.OllamaRouterStressHarness

PHASES=("$@")
if [ ${#PHASES[@]} -eq 0 ]; then
  PHASES=(phase1 phase1b phase2a phase2b phase3 phase4)
fi

declare -A ENV_FOR=(
  [phase1]="OLLAMA_ORCH_PACING_MS=800 OLLAMA_ORCH_NUM_PREDICT=64"
  [phase1b]="OLLAMA_ORCH_PACING_MS=2500 OLLAMA_ORCH_NUM_PREDICT=96 OLLAMA_ORCH_MAX_QUEUE=2"
  [phase2a]="OLLAMA_ORCH_URL=http://10.255.255.1:11434/api/generate OLLAMA_ORCH_PACING_MS=100 OLLAMA_ORCH_NUM_PREDICT=16"
  [phase2b]="OLLAMA_ORCH_PACING_MS=100 OLLAMA_ORCH_NUM_PREDICT=400"
  [phase3]="OLLAMA_ORCH_MIN_FREE_MB=9999999 OLLAMA_ORCH_PACING_MS=100"
  [phase4]="OLLAMA_ORCH_HTTP_PORT=11440 OLLAMA_ORCH_PACING_MS=150 OLLAMA_ORCH_NUM_PREDICT=32"
)

OVERALL=0
for P in "${PHASES[@]}"; do
  echo "=== PHASE $P ==="
  envs="${ENV_FOR[$P]:-}"
  # shellcheck disable=SC2086
  env $envs java -cp "$CP" "$HARNESS" "$P" 2>&1
  RC=$?
  echo "=== PHASE $P exit=$RC ==="
  if [ $RC -ne 0 ]; then OVERALL=1; fi
done
echo "OVERALL=$OVERALL"
exit $OVERALL