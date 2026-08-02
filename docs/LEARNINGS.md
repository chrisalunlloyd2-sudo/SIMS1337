# LEARNINGS — 4D HEX GAME Round Log
Hussein-style: every round records Goal / Steps / What Broke / Learning / Next Steps.
The crew reads this before acting; each round feeds the next.

---

## Round 9 — hourly-task t1: Task priority field (Aegis-doctor as banker)

### Goal
Implement priority field (1-10) with getter/setter on Task.java; close t1 which was
marked done but never actually delivered to GitHub.

### Steps Taken
1. Banker audit: compared game log (t1/t2/t3 all ✅) vs GitHub branch (only t3 present).
2. Found t1 diff lost, t2 silently swept into t3 commit (working-tree carry-over).
3. Inspected Task.java: found junk line `// hourly-task: tok850 tok870...`
   (placeholder-token garbage appended by broken pipeline fallback).
4. Stripped junk, added `priority = 5` field + both constructors + validated getter/setter.
5. Verified brace/paren balance, committed `ca43c66`, pushed to hourly-task branch.

### What Broke
- Pipeline marked tasks done even when commit/push FAILED (git identity was unset
  on early runs) → silent lost work, system said ✅.
- Placeholder mode (no native llama-cli) emits fake tokens; pipeline appended them
  as comments into real source files.

### Learning
1. NEVER mark a task done unless the commit is verified ON THE BRANCH (git log check).
2. NEVER trust fallback/placeholder output as code — it's token garbage.
3. Banker audit = compare declared state vs actual state (game log vs GitHub API).
4. A "done" that isn't delivered is not done.

### Next Steps
- Pipeline must verify commit+push before marking done (FIXED this round).
- Pipeline must never append placeholder output to source files (FIXED: revert on failure).
- Add compile dry-check (javac) to banker audit rotation.
- Record this round in the game log as a learning move.

---

## Round 10 — Doctor-Banker & Pipeline Hardening (in progress)

### Goal
Fold the learning doctrine into the hourly pipeline itself:
after every task, append a LEARNINGS.md entry and push it with the same commit.

### Steps Taken
1. Patch hourly_task.py: after verified commit, append Hussein-style learning entry
   to docs/LEARNINGS.md and include it in the commit.
2. Add `git log --oneline -5` + javac dry-check to doctor_banker rotation (banker audit).

### What Broke
(none yet — this round)

### Learning
- Learning is only valuable if it lands in the same commit stream the crew reads.

### Next Steps
- Ship LEARNINGS.md; wire gist backup; confirm doctor rotation includes audit.

---

## Round — hourly-task t1 (Add a priority field to Task.java with getter/sett)

### Goal
Add a priority field to Task.java with getter/setter

### Steps Taken
1. Picked task from pool.
2. SLM generated diff.
3. Applied to src/main/java/com/aigen/sims/tasks/Task.java.
4. Committed + pushed (verified on branch).

### What Broke
None — clean delivery.

### Learning
- Delivery is verified on the branch, not assumed.
- Placeholder token output is never code.

### Next Steps
- Continue next task from pool; keep each round to ONE commit.
