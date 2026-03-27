# Mudita Kompakt Sudoku

## What This Is

A fully playable Sudoku game for the Mudita Kompakt E-ink Android device, built with Kotlin, Jetpack Compose, and the Mudita Mindful Design (MMD) library. Players choose from Easy, Medium, or Hard difficulty, solve puzzles with touch input, use hints at a score penalty, and earn an error-based score at completion. High scores are stored persistently per difficulty, and in-progress games can be paused and resumed across app restarts.

## Core Value

A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

## Requirements

### Validated

- ✓ App generates valid Sudoku puzzles with exactly one solution — v1.0 (PUZZ-01, 28 tests)
- ✓ Difficulty classified by technique tier and given-cell count — v1.0 (PUZZ-02/03; Sudoklify produces bimodal NAKED_SINGLES/ADVANCED distribution; MEDIUM uses count-only differentiation by library constraint)
- ✓ Player selects Easy, Medium, or Hard from main menu — v1.0 (DIFF-01, DIFF-02)
- ✓ Player taps cell to select; taps digit to fill; selection highlights immediately — v1.0 (INPUT-01, INPUT-02)
- ✓ Player toggles fill / pencil mark mode; marks stored and displayed separately — v1.0 (INPUT-03, INPUT-04)
- ✓ Player undoes last fill or pencil mark action — v1.0 (INPUT-05)
- ✓ Errors tracked silently; not surfaced until game end — v1.0 (SCORE-01)
- ✓ App detects completion when all 81 cells correctly filled — v1.0 (SCORE-02)
- ✓ Player requests hint; one unfilled correct cell revealed; hint count incremented — v1.0 (SCORE-03)
- ✓ Each hint deducts fixed penalty from final score — v1.0 (SCORE-04)
- ✓ Completion screen shows error count, hints used, and final score — v1.0 (SCORE-05, SCORE-06)
- ✓ Per-difficulty high scores stored persistently — v1.0 (HS-01)
- ✓ New personal best detected and displayed on summary screen — v1.0 (HS-02)
- ✓ Leaderboard screen shows top score per difficulty — v1.0 (HS-03)
- ✓ Game state (board, pencil marks, errors, hints) persisted on pause — v1.0 (STATE-01)
- ✓ Resume prompt shown on launch when saved game exists — v1.0 (STATE-02)
- ✓ Player resumes paused game exactly as left — v1.0 (STATE-03)
- ✓ Main menu with New Game (→ difficulty selection) and Best Scores — v1.0 (NAV-01)
- ✓ All UI built with MMD library wrapped in ThemeMMD — v1.0 (UI-01)
- ✓ No animations, ripple effects, or transitions — v1.0 (UI-02)
- ✓ All touch targets ≥56dp — v1.0 (UI-03)

### Active

- ✓ Pencil mark digits visible in white on selected (black) cells — Validated in Phase 07: grid-rendering-fixes (GRID-01)
- ✓ Pencil mark font sized to fit 4 marks (2×2) at maximum per cell — Validated in Phase 07: grid-rendering-fixes (GRID-02)
- ✓ Number pad digits use sans-serif-condensed (Roboto Condensed) for better vertical centering — Validated in Phase 08: controls-number-pad-fixes (CTRL-01)
- ✓ "Get Hint" button renders as two centered lines — Validated in Phase 08: controls-number-pad-fixes (CTRL-02)
- ✓ Inactive Fill/Pencil button has mid-gray (#E0E0E0) background; pair enclosed in 1dp border frame — Validated in Phase 08: controls-number-pad-fixes (CTRL-03, CTRL-04)
- ✓ Exit confirmation dialog (Save / Forfeit) shown on back press during active game — Validated in Phase 09: game-navigation (NAV-01, NAV-02, NAV-03)

### Out of Scope

- Real-time error highlighting — deliberately silent; errors revealed at completion; aligns with mindful ethos
- Timer / time-based scoring — creates anxiety; contradicts Mudita device philosophy
- Daily challenges / streaks — obligation-inducing gamification loops; against mindful premise
- Global / online leaderboards — no Google Services on MuditaOS K; local only
- Multiplayer — single-player offline experience by design
- Achievements / badges / XP — visual noise and extrinsic motivation loops; contrary to mindful design
- Color themes / custom skins — E-ink is monochromatic; theming is irrelevant
- Sound effects / haptics — E-ink audience values quiet focus
- Cloud save / cross-device sync — no backend; local-only scope
- Ad monetization — violates distraction-free premise
- Auto-fill pencil marks — reduces cognitive challenge; manual annotation is part of the game
- Custom puzzle creation — generated puzzles only; no import/entry UI

## Current Milestone: v1.1 Bug Fixes and Improvements

**Goal:** Fix cosmetic UI issues identified from first real-device play session.

**Target features:**
- Pencil mark digits visible in white on selected (black) cells
- Pencil mark font sized to fit 4 marks (2×2) at maximum per cell
- Number pad text vertically centered with taller/thinner font
- "Get Hint" button text centered (two lines, vertically centered)
- Fill/Pencil inactive button subtle background + frame separating pair from number row

## Context

- **Shipped:** v1.0 MVP — 2026-03-25
- **Codebase:** ~6,700 lines Kotlin, 8 phases, 23 plans
- **Platform:** Mudita Kompakt, MuditaOS K (de-Googled AOSP Android 12, API 31)
- **Display:** 4.3" E-ink touchscreen, 800×480, monochromatic, ~216 ppi
- **Tech stack:** Kotlin 2.3.20, Jetpack Compose BOM 2026.03.00, MMD 1.0.1, Sudoklify 1.0.0-beta04, DataStore 1.2.1
- **Architecture:** MVVM + StateFlow; single-module; Screen enum routing (5 screens)
- **E-ink validation:** Physical device verified — no ghosting, no ripple, all touch targets pass

## Constraints

- **Tech Stack:** Kotlin + Jetpack Compose + MMD library — required for Mudita Kompakt compatibility
- **Display:** 800×480 E-ink — large touch targets, high contrast, no animations, instant feedback
- **Architecture:** MVVM with StateFlow — aligns with MMD's recommended pattern
- **Storage:** Local only — no network, no backend

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Error-based scoring (not time-based) | Aligns with mindful, low-pressure design ethos of Mudita | ✓ Validated v1.0 |
| Silent error tracking (not real-time highlights) | Reduces distraction; player discovers mistakes at completion | ✓ Validated v1.0 |
| Hint penalty instead of leaderboard disqualification | Keeps game flowing; gentler than hard disqualification | ✓ Validated v1.0 |
| Pause/resume via local persistence (DataStore) | E-ink device users may put down device for hours; state must survive | ✓ Validated v1.0 |
| Sudoklify for puzzle generation | Only Kotlin-native library with built-in difficulty levels; 2.4T+ variations | ✓ Good — but MEDIUM tier is count-only due to bimodal preset distribution |
| MEDIUM difficulty uses given-count range only | Sudoklify presets produce no HIDDEN_PAIRS tier empirically (bimodal: NAKED_SINGLES or ADVANCED) | ✓ Accepted — documented in DifficultyConfig.kt |
| Screen enum routing (not NavHost) | 5 screens, no deep linking; enum is sufficient and simpler | ✓ Good — refactor to NavHost only if deep linking needed |
| MMD local stubs for offline dev | GitHub Packages requires auth token; stubs allow compilation without AAR | ✓ Good — replaced with real AAR once credentials configured |
| compileOnly → implementation for MMD | Real AAR resolved via scoped GitHub Packages repo with credential gating | ✓ Good |
| Resume button on MenuScreen (not GameScreen dialog) | Cleaner UX — user sees resume option before any game logic runs | ✓ Validated v1.0 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-27 — Phase 09 complete (NAV-01, NAV-02, NAV-03) — milestone v1.1 complete*
