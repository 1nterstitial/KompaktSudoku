---
phase: 03
slug: core-game-ui
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) + Robolectric (4.14.1) + Compose UI Test |
| **Config file** | None — Robolectric config via `@RunWith(RobolectricTestRunner::class)` |
| **Quick run command** | `./gradlew :app:test --tests "com.mudita.sudoku.ui.*" -x lint` |
| **Full suite command** | `./gradlew :app:test` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:test --tests "com.mudita.sudoku.ui.*" -x lint`
- **After every plan wave:** Run `./gradlew :app:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | UI-01, UI-02 | Compose UI smoke | `./gradlew :app:test --tests "com.mudita.sudoku.ui.GameScreenTest"` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | D-02 | Compose UI semantics | `./gradlew :app:test --tests "com.mudita.sudoku.ui.GameGridTest"` | ❌ W0 | ⬜ pending |
| 03-01-03 | 01 | 1 | UI-03 | Compose UI bounds | `./gradlew :app:test --tests "com.mudita.sudoku.ui.TouchTargetTest"` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 1 | D-08 | Compose UI semantics | `./gradlew :app:test --tests "com.mudita.sudoku.ui.ControlsRowTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/mudita/sudoku/ui/GameScreenTest.kt` — stubs for UI-01, UI-02 smoke
- [ ] `app/src/test/java/com/mudita/sudoku/ui/GameGridTest.kt` — stubs for D-02 selected cell
- [ ] `app/src/test/java/com/mudita/sudoku/ui/ControlsRowTest.kt` — stubs for D-08 toggle state
- [ ] `app/src/test/java/com/mudita/sudoku/ui/TouchTargetTest.kt` — stubs for UI-03

All four use Robolectric + `createComposeRule()` pattern established in `GameViewModelTest.kt` (`@RunWith(RobolectricTestRunner::class)`).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| No animations/ripple at runtime | UI-02 | Compose test verifies absence of Animated\* composables at code level; visual ripple can only be confirmed on device/emulator | Run app on Mudita Kompakt or emulator; tap cells, buttons, toggles — confirm no ripple or animation artifacts |
| ThemeMMD eInkColorScheme applied | UI-01 | Real MMD AAR requires credentials; stub in MmdComponents.kt wraps MaterialTheme (not eInkColorScheme) | Resolve MMD credentials, replace stub, deploy APK — visually confirm monochromatic E-ink color scheme |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
