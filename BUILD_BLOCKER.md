# BUILD BLOCKER — GodHandApp.java does not compile

**For Aegis, the hourly pipeline, and anything triaging issues #122–#127.**

`ORDERS OF VIPER` — investigated by Viper 2026-08-09 at Chris's instruction.

---

## Stop trying to fix the TODOs in `GodHandApp.java`

Issues **#122, #123, #124, #125, #126, #127** are all TODO markers inside
`src/main/java/com/aigen/sims/GodHandApp.java`. The hourly pipeline keeps selecting them.
**Strategy A currently sits at ~117 failures and 0 wins.**

The cause is not the triage order, and not the `fix_engine` Java anchor.

> **`GodHandApp.java` has never compiled in this repository.**

Verified with `mvn compile`: **58 `cannot find symbol` errors.** No patch to any TODO in that file
can be verified, because the file cannot be built to verify it. Every attempt will fail, forever,
for a reason that has nothing to do with the patch.

## Why: eight classes are referenced and absent

```
SwarmWatchdog            MISSING from this repo
MCTSPipeline             MISSING
AdversarialFuzzer        MISSING
MetaLogicSupervisor      MISSING
NightlyEvolutionEngine   MISSING
ModelManager             MISSING
SQLiteMemory             MISSING
KnowledgeGraph           present, but in package com.aigen.sims.engine — GodHandApp imports
                         com.aigen.sims.KnowledgeGraph, so the import does not resolve
```

Git history check: `SwarmWatchdog`, `ModelManager` and `SQLiteMemory` were **never committed here**.
`KnowledgeGraph` was committed once (`a6f12ce`) and later moved to a sub-package.

## Where they actually are

**All eight exist in `chrisalunlloyd2-sudo/sims-java-neo-fx`** (default branch `master`, 111 java
files, public):

```
src/main/java/com/aigen/sims/SwarmWatchdog.java              874 B
src/main/java/com/aigen/sims/MCTSPipeline.java             1,132 B
src/main/java/com/aigen/sims/AdversarialFuzzer.java        1,267 B
src/main/java/com/aigen/sims/MetaLogicSupervisor.java      1,501 B
src/main/java/com/aigen/sims/NightlyEvolutionEngine.java   1,541 B
src/main/java/com/aigen/sims/ModelManager.java             4,081 B
src/main/java/com/aigen/sims/SQLiteMemory.java             2,081 B
src/main/java/com/aigen/sims/KnowledgeGraph.java           2,434 B
```

That repo also holds a **93 KB `GodHandApp.java`** versus the **59 KB** partial copy here — so this
repo's copy is an older fork that lost its dependencies, not the authoritative version.

## What would actually unblock the pipeline

1. **Do not select `GodHandApp.java` TODOs until the file compiles.** A target that cannot build is
   not a tractable task, and burning 117 cycles on it starves the 23-task pool that can.
2. Reconcile with `sims-java-neo-fx` — take its GodHandApp and the 8 classes, or drop GodHandApp
   from this repo entirely.
3. **Add a buildability precondition to triage:** before a Java issue is selected, confirm its file
   compiles. One check would have caught this on failure #1 instead of #117.

Chris's instruction on the reconciliation: **skip `AdversarialFuzzer`** — replace it with a stronger
health supervisor rather than porting the fuzzer.

## Note on the failure signature

`_cron_loop`-style handlers that catch, print, and continue will repeat an identical failure
indefinitely without ever escalating. 117 identical failures should have raised **one** alert, not
117 log lines. Same class as the 42 duplicate `[TEST FAILURE] no tests ran` proposals that ran for
44 days on the Viper box, now fixed by hashing on the failure signature rather than the timestamp.
