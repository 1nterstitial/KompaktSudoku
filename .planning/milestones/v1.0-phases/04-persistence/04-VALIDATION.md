---
phase: 4
slug: persistence
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Robolectric 4.14.1 + Mockk 1.13.17 + Turbine 1.2.0 |
| **Config file** | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*Persistence*" --tests "*DataStore*" --tests "*Repository*" -x lint` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest -x lint` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "*Persistence*" --tests "*DataStore*" --tests "*Repository*" -x lint`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest -x lint`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | STATE-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*PersistedGameStateTest*"` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | STATE-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*DataStoreGameRepositoryTest*"` | ❌ W0 | ⬜ pending |
| 4-02-01 | 02 | 2 | STATE-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*"` | ❌ W0 | ⬜ pending |
| 4-02-02 | 02 | 2 | STATE-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*"` | ❌ W0 | ⬜ pending |
| 4-03-01 | 03 | 3 | STATE-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*"` | ❌ W0 | ⬜ pending |
| 4-03-02 | 03 | 3 | STATE-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*"` | ❌ W0 | ⬜ pending |
| 4-04-01 | 04 | 3 | STATE-02 | manual | N/A — lifecycle hook | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt` — shared test double used by all persistence tests
- [ ] `app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt` — DTO serialization round-trip tests for STATE-01
- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelPersistenceTest.kt` — ViewModel integration tests for STATE-01, STATE-02, STATE-03

*Existing test infrastructure (JUnit 4, Mockk, Turbine) covers all phase requirements — no new framework install needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `onStop()` saves on force-close | STATE-01 | Lifecycle hook; no unit test can simulate process kill | 1. Start game. 2. Force-stop app via Android Settings. 3. Reopen — verify resume dialog appears with restored state. |
| Resume dialog appears on launch | STATE-02 | UI dialog triggered on app launch; requires running app | 1. Pause a game. 2. Close app. 3. Reopen — confirm dialog with "Resume" / "New Game" appears. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
