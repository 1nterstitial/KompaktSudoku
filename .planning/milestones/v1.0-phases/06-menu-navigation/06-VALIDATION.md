---
phase: 6
slug: menu-navigation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Robolectric + Compose UI Test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*.MenuScreenTest" --tests "*.DifficultyScreenTest"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "*.MenuScreenTest" --tests "*.DifficultyScreenTest"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 6-W0-01 | W0 | 0 | NAV-01 | ui | `./gradlew :app:testDebugUnitTest --tests "*.MenuScreenTest"` | ❌ W0 | ⬜ pending |
| 6-W0-02 | W0 | 0 | NAV-01 | ui | `./gradlew :app:testDebugUnitTest --tests "*.DifficultyScreenTest"` | ❌ W0 | ⬜ pending |
| 6-01-01 | 01 | 1 | NAV-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.MenuScreenTest"` | ❌ W0 | ⬜ pending |
| 6-01-02 | 01 | 1 | NAV-01 | ui | `./gradlew :app:testDebugUnitTest --tests "*.DifficultyScreenTest"` | ❌ W0 | ⬜ pending |
| 6-02-01 | 02 | 2 | NAV-01 | ui | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |
| 6-03-01 | 03 | 3 | NAV-01 | ui | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/MenuScreenTest.kt` — stubs for NAV-01 menu display and resume-conditional behavior
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/DifficultyScreenTest.kt` — stubs for NAV-01 difficulty selection

*Existing infrastructure (Robolectric + Compose UI Test) covers the framework; only test files are missing.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Back press at main menu exits app | NAV-01 | `BackHandler` finishes Activity — hard to assert in Robolectric without instrumented test | Launch app, navigate to menu, press back, verify app closes |
| Back press mid-game pauses and returns to menu | NAV-01 | Requires Activity lifecycle interaction | Start a game, press back, verify menu shown and game auto-paused |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
