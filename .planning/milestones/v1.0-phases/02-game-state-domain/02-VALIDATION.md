---
phase: 2
slug: game-state-domain
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockk 1.13.x + Turbine 1.2.0 + Robolectric 4.14.x |
| **Config file** | `app/build.gradle.kts` (test dependencies already declared) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 0 | DIFF-01, DIFF-02 | unit stub | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-01-02 | 01 | 1 | DIFF-01, DIFF-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-02-01 | 02 | 0 | INPUT-01, INPUT-02 | unit stub | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-02-02 | 02 | 1 | INPUT-01, INPUT-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-03-01 | 03 | 0 | INPUT-03 | unit stub | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-03-02 | 03 | 1 | INPUT-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-04-01 | 04 | 0 | INPUT-04, INPUT-05 | unit stub | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-04-02 | 04 | 1 | INPUT-04, INPUT-05 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-05-01 | 05 | 0 | SCORE-01, SCORE-02 | unit stub | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 2-05-02 | 05 | 1 | SCORE-01, SCORE-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/example/sudoku/GameViewModelTest.kt` — stubs for DIFF-01, DIFF-02, INPUT-01 through INPUT-05, SCORE-01, SCORE-02
- [ ] `app/src/test/java/com/example/sudoku/GameUiStateTest.kt` — stubs for state model correctness (givenMask, pencilMarks, IntArray equality)

*Existing test infrastructure (JUnit 4, Mockk, Turbine, Robolectric) already declared in build.gradle.kts — no new installs required.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Puzzle difficulty matches Sudoklify classification | DIFF-02 | Sudoklify difficulty enum is opaque; unit test can verify the mapping but not the calibration of the algorithm itself | Generate 10 puzzles per difficulty; count given cells; verify Easy ~36-45, Medium ~27-35, Hard ~22-26 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
