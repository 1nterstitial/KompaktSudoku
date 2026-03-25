# Mudita Kompakt Sudoku

## What This Is

A Sudoku game for the Mudita Kompakt E-ink Android device, built with Kotlin and the Mudita Mindful Design (MMD) library. Players choose from three difficulty levels, solve puzzles with touch input, and earn an error-based score at completion. High scores are stored persistently per difficulty, and in-progress games can be paused and resumed later.

## Core Value

A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

## Requirements

### Validated

- [x] Player can tap a cell to select it and tap a number to fill it in — *Validated in Phase 02: game-state-domain*
- [x] Errors are tracked silently during play (not surfaced until game end) — *Validated in Phase 02: game-state-domain*
- [x] Puzzles vary by cells revealed AND solving complexity per difficulty level — *Validated in Phase 01 + 02: puzzle engine + difficulty selection in ViewModel*

### Active

- [x] Player can select a difficulty level (Easy, Medium, Hard) from the main menu — *Validated in Phase 06: menu-navigation*
- [x] Player can request a hint during play (each hint incurs a score penalty) — *Validated in Phase 05: scoring-completion*
- [x] Player can pause a game and resume it at a later time (game state persists on device) — *Validated in Phase 04: persistence*
- [x] On completion, player sees a score summary (error count, hints used, final score) — *Validated in Phase 05: scoring-completion*
- [x] High scores are stored persistently per difficulty level (leaderboard) — *Validated in Phase 05: scoring-completion*
- [ ] All UI is built with the Mudita Mindful Design (MMD) library and ThemeMMD

## Current State

Phase 06 complete — Full 5-screen navigation flow implemented. App launches to MenuScreen with conditional Resume button (reactive StateFlow). DifficultyScreen added. All screens wired via Screen enum in MainActivity (MENU→DIFFICULTY→GAME→SUMMARY→LEADERBOARD). BackHandler save-on-back in GameScreen. All 6 phases of v1.0 are complete.

### Out of Scope

- Multiplayer or online features — single-player offline experience only
- Color UI elements — E-ink display is monochromatic, high contrast only
- Animated transitions — E-ink ghosting prevention; use instant state updates
- Custom puzzle creation — generated puzzles only for v1
- Account system — local device storage only

## Context

- **Platform**: Mudita Kompakt running MuditaOS K (de-Googled AOSP Android)
- **Display**: 4.3" E-ink touchscreen, 800×480 px, monochromatic, ~216 ppi
- **E-ink constraints**: No ripple effects, no animations, full-width layouts preferred, minimal partial refreshes
- **MMD library**: Kotlin + Jetpack Compose components optimized for E-ink (ThemeMMD, ButtonMMD, TextMMD, etc.) — repo: https://github.com/mudita/MMD
- **Scoring**: Error-based — fewer errors = higher score; each hint used deducts a fixed penalty from the final score
- **Game state persistence**: Paused games saved to local device storage (DataStore or SharedPreferences)

## Constraints

- **Tech Stack**: Kotlin + Jetpack Compose + MMD library — required for Mudita Kompakt compatibility
- **Display**: 800×480 E-ink — large touch targets, high contrast, no animations, instant feedback
- **Architecture**: MVVM with StateFlow — aligns with MMD's recommended pattern
- **Storage**: Local only — no network, no backend

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Error-based scoring (not time-based) | Aligns with mindful, low-pressure design ethos of Mudita | Validated Phase 05 |
| Silent error tracking (not real-time highlights) | Reduces distraction; player discovers mistakes at completion | Validated Phase 05 |
| Hint penalty instead of leaderboard disqualification | Keeps game flowing; penalty is gentler than hard disqualification | Validated Phase 05 |
| Pause/resume via local persistence | E-ink device users may put down device for hours; state must survive | Validated Phase 04 |

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
*Last updated: 2026-03-25 after Phase 05 completion — scoring, hints, and completion screens complete*
