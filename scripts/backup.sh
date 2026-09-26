#!/usr/bin/env bash
# ============================================================================
# SIMS1337 BACKUP — run before & after every modification. Never skip.
#   bash scripts/backup.sh --snapshot <label>   # tag + tarball (BEFORE)
#   bash scripts/backup.sh --push "<message>"   # commit + push to main (AFTER)
#   bash scripts/backup.sh --status             # drift + last backup report
#   bash scripts/backup.sh --restore <tag>      # roll back to a snapshot
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
REPO="SIMS1337"
LOG="logs/backup_receipt.jsonl"
SNAP_DIR="logs/snapshots"
mkdir -p "$SNAP_DIR" logs reports

ts(){ date -u +%Y-%m-%dT%H:%M:%SZ; }
receipt(){ echo "{\"ts\":\"$(ts)\",\"sha\":\"${1:-}\",\"msg\":\"${2:-}\",\"agent\":\"${AGENT_NAME:-unknown}\"}" >> "$LOG"; }

case "${1:-}" in
  --snapshot)
    LABEL="${2:-snap-$(date +%s)}"
    TAG="backup/$LABEL"
    git tag -f "$TAG" >/dev/null 2>&1 || true
    tar czf "$SNAP_DIR/$LABEL-$(date +%Y%m%d-%H%M%S).tar.gz" \
      --exclude=.git --exclude=target --exclude=logs/snapshots \
      docs scripts src pom.xml README.md *.md 2>/dev/null || true
    echo "snapshot: tag=$TAG tarball=$SNAP_DIR/$LABEL-*.tar.gz"
    receipt "$TAG" "snapshot $LABEL"
    ;;
  --push)
    MSG="${2:-chore: backup}"
    git add -A
    # never commit build artifacts or snapshots
    git reset -q logs/snapshots target 2>/dev/null || true
    if git diff --cached --quiet; then echo "nothing to commit"; exit 0; fi
    git commit -m "$MSG" >/dev/null
    SHA=$(git rev-parse --short HEAD)
    git push origin main >/dev/null 2>&1 || git push origin HEAD:main >/dev/null
    echo "pushed: $SHA — $MSG"
    receipt "$SHA" "$MSG"
    ;;
  --status)
    LOCAL=$(git rev-parse --short HEAD 2>/dev/null || echo none)
    REMOTE=$(git ls-remote origin main 2>/dev/null | cut -c1-8 || echo unknown)
    echo "local:  $LOCAL"
    echo "remote: $REMOTE"
    [ "$LOCAL" = "$REMOTE" ] && echo "STATUS: SYNCED ✓" || echo "STATUS: DRIFT — push now (backup.sh --push)"
    echo "--- last 3 receipts ---"
    tail -3 "$LOG" 2>/dev/null || echo "no receipts yet"
    ;;
  --restore)
    git checkout "${2:-backup/last}" -- . 2>/dev/null || echo "tag not found: ${2:-backup/last}"
    echo "restored from ${2:-backup/last} — review then commit"
    ;;
  *)
    echo "usage: backup.sh --snapshot <label> | --push <msg> | --status | --restore <tag>"
    ;;
esac
