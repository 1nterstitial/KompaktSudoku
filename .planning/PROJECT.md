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

- [ ] Player can select a difficulty level (Easy, Medium, Hard) from the main menu
- [ ] Player can request a hint during play (each hint incurs a score penalty)
- [ ] Player can request a hint during play (each hint incurs a score penalty)
- [ ] Player can pause a game and resume it at a later time (game state persists on device)
- [ ] On completion, player sees a score summary (error count, hints used, final score)
- [ ] High scores are stored persistently per difficulty level (leaderboard)
- [ ] All UI is built with the Mudita Mindful Design (MMD) library and ThemeMMD

## Current State

Phase 02 complete — GameViewModel and all game-state domain models implemented. Core game logic (cell selection, fill mode, pencil marks, undo, error tracking, completion detection) is fully tested with 38 unit tests. Building toward Phase 03: Core Game UI.

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
| Error-based scoring (not time-based) | Aligns with mindful, low-pressure design ethos of Mudita | — Pending |
| Silent error tracking (not real-time highlights) | Reduces distraction; player discovers mistakes at completion | — Pending |
| Hint penalty instead of leaderboard disqualification | Keeps game flowing; penalty is gentler than hard disqualification | — Pending |
| Pause/resume via local persistence | E-ink device users may put down device for hours; state must survive | — Pending |

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
*Last updated: 2026-03-24 after Phase 02 completion — game-state domain complete*
