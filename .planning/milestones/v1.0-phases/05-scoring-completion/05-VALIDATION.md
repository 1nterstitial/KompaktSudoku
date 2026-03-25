---
phase: 5
slug: scoring-completion
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) + Robolectric (4.14.x) + Turbine (1.2.0) |
| **Config file** | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest -x lint` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest -x lint`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-W0-01 | Wave 0 | 0 | SCORE-03, HS-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelHintTest*" -x lint` | ❌ W0 | ⬜ pending |
| 05-W0-02 | Wave 0 | 0 | SCORE-04, SCORE-06 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ScoreCalculationTest*" -x lint` | ❌ W0 | ⬜ pending |
| 05-W0-03 | Wave 0 | 0 | HS-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.FakeScoreRepository*" -x lint` | ❌ W0 | ⬜ pending |
| 05-W0-04 | Wave 0 | 0 | HS-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DataStoreScoreRepositoryTest*" -x lint` | ❌ W0 | ⬜ pending |
| 05-W0-05 | Wave 0 | 0 | SCORE-05 | unit (Compose) | `./gradlew :app:testDebugUnitTest --tests "*.SummaryScreenTest*" -x lint` | ❌ W0 | ⬜ pending |
| 05-W0-06 | Wave 0 | 0 | HS-03 | unit (Compose) | `./gradlew :app:testDebugUnitTest --tests "*.LeaderboardScreenTest*" -x lint` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelHintTest.kt` — stubs for SCORE-03, HS-02
- [ ] `app/src/test/java/com/mudita/sudoku/game/ScoreCalculationTest.kt` — stubs for SCORE-04, SCORE-06
- [ ] `app/src/test/java/com/mudita/sudoku/game/FakeScoreRepository.kt` — shared test double for HS-01, HS-02 ViewModel tests
- [ ] `app/src/test/java/com/mudita/sudoku/game/DataStoreScoreRepositoryTest.kt` — stubs for HS-01
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/SummaryScreenTest.kt` — stubs for SCORE-05
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/LeaderboardScreenTest.kt` — stubs for HS-03

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Screen routing transitions (GAME→SUMMARY→LEADERBOARD) | SCORE-05, HS-03 | MainActivity Compose state requires device/emulator for full integration | Launch app, complete a puzzle, verify SUMMARY screen appears; tap "View Leaderboard", verify LEADERBOARD screen; tap "New Game", verify return to GAME |
| E-ink rendering — no ghosting from hint button addition | UI-02 | Cannot be automated — requires physical Mudita Kompakt | Visually inspect ControlsRow with 4th Hint button on device |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
