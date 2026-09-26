#!/bin/bash
# Heartbeat merge runner: merges origin/main into local, resolving known conflicts
set -x
cd /c/Users/viper/AIGEN_SYS/repos/sims-java-neo-fx || exit 1

git merge origin/main --no-edit -m "heartbeat merge: sync upstream main (vault import + autonomous agent work) into local snapshot" 2>&1
MERGE_EXIT=$?
echo "MERGE_EXIT=$MERGE_EXIT"

if [ $MERGE_EXIT -ne 0 ]; then
  echo "=== CONFLICTS ==="
  git diff --name-only --diff-filter=U
  # Resolve known conflicts
  for f in $(git diff --name-only --diff-filter=U); do
    case "$f" in
      .gitignore)   git checkout --ours -- "$f"; echo "RESOLVED(ours) $f" ;;
      CHANGELOG.md) git checkout --theirs -- "$f"; echo "RESOLVED(theirs) $f" ;;
      guardian.bat|logs/guardian.log) git checkout --ours -- "$f"; echo "RESOLVED(ours) $f" ;;
      *) git checkout --theirs -- "$f" 2>/dev/null || git checkout --ours -- "$f"; echo "RESOLVED(default-theirs) $f" ;;
    esac
    git add -- "$f"
  done
  echo "=== RESOLUTION DONE, committing merge ==="
  git commit --no-edit -m "heartbeat merge: sync upstream main (vault import + autonomous agent work) into local snapshot [conflicts resolved: .gitignore=ours, CHANGELOG=theirs, guardian=ours]"
  echo "COMMIT_EXIT=$?"
fi

echo "=== FINAL LOG ==="
git log --oneline -3
echo "=== AHEAD/BEHIND ==="
git rev-list --left-right --count origin/main...HEAD
echo "MERGE_RUNNER_DONE"