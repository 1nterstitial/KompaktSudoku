---
phase: 1
slug: puzzle-engine
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-23
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) — standard Android/Kotlin project |
| **Config file** | `app/build.gradle.kts` (testImplementation block) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*.puzzle.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~30 seconds (pure Kotlin, no Android deps) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "*.puzzle.*"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 0 | PUZZ-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.PuzzleEngineTest"` | ❌ W0 | ⬜ pending |
| 1-01-02 | 01 | 1 | PUZZ-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.UniquenessVerifierTest"` | ❌ W0 | ⬜ pending |
| 1-01-03 | 01 | 1 | PUZZ-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DifficultyClassifierTest"` | ❌ W0 | ⬜ pending |
| 1-01-04 | 01 | 1 | PUZZ-02, PUZZ-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DifficultyValidationTest"` | ❌ W0 | ⬜ pending |
| 1-01-05 | 01 | 2 | PUZZ-01, PUZZ-02, PUZZ-03 | integration | `./gradlew :app:testDebugUnitTest --tests "*.SudokuEngineIntegrationTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/mudita/sudoku/puzzle/PuzzleEngineTest.kt` — stub test for SudokuEngine facade (PUZZ-01)
- [ ] `app/src/test/java/com/mudita/sudoku/puzzle/UniquenessVerifierTest.kt` — stubs for abort-on-second-solution verifier (PUZZ-01)
- [ ] `app/src/test/java/com/mudita/sudoku/puzzle/DifficultyClassifierTest.kt` — stubs for technique-based classifier (PUZZ-02)
- [ ] `app/src/test/java/com/mudita/sudoku/puzzle/DifficultyValidationTest.kt` — 20-puzzle batch tests per difficulty verifying cell counts and technique classification (PUZZ-02, PUZZ-03)
- [ ] `app/src/test/java/com/mudita/sudoku/puzzle/SudokuEngineIntegrationTest.kt` — end-to-end generation + uniqueness + difficulty check (PUZZ-01, PUZZ-02, PUZZ-03)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Generation completes in <2s on Mudita Kompakt | PUZZ-03 (performance) | Requires physical device profiling — no emulator equivalent | Run `SudokuEngineIntegrationTest.generationPerformanceTest` on physical device via Android Studio profiler; check all 3 difficulties complete under 2000ms |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
