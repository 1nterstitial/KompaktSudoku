# Requirements: Mudita Kompakt Sudoku

**Defined:** 2026-03-23
**Core Value:** A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

## v1 Requirements

### Puzzle Generation

- [x] **PUZZ-01**: App generates valid Sudoku puzzles that have exactly one solution
- [x] **PUZZ-02**: Difficulty is classified by required solving technique: Easy uses naked singles only; Hard requires advanced techniques (X-wing, chains). Medium is differentiated from Easy by given-cell count only (27–35 vs 36–45 givens) — Sudoklify's preset schemas produce no hidden-pairs-tier puzzles empirically (bimodal distribution: NAKED_SINGLES_ONLY or ADVANCED, nothing in between). The DifficultyClassifier correctly implements hidden-pairs detection infrastructure; the generator constraint is a library limitation documented in DifficultyConfig.kt.
- [x] **PUZZ-03**: Easy puzzles provide approximately 36–45 given cells; Medium 27–35; Hard 22–27

### Difficulty

- [x] **DIFF-01**: User can choose Easy, Medium, or Hard difficulty from the main menu before starting a game
- [x] **DIFF-02**: Each difficulty level generates puzzles matching the cell count and technique classification defined in PUZZ-02 and PUZZ-03

### Input & Interaction

- [x] **INPUT-01**: User can tap a cell to select it; selected cell is visually highlighted
- [x] **INPUT-02**: User can tap a digit (1–9) to fill the selected cell with that value
- [x] **INPUT-03**: User can toggle between "fill" mode and "pencil mark" mode for input
- [x] **INPUT-04**: User can add and remove pencil mark candidates in any cell
- [x] **INPUT-05**: User can undo the last move (fill or pencil mark action)

### Game State

- [x] **STATE-01**: User can pause a game mid-play; full grid state, pencil marks, error count, and hint count are persisted to device storage
- [x] **STATE-02**: On app launch, if a paused game exists, user is prompted to resume it or start a new game
- [x] **STATE-03**: User can resume a paused game and continue exactly where they left off

### Scoring & Completion

- [x] **SCORE-01**: Errors are tracked silently during play and not surfaced until the game ends
- [x] **SCORE-02**: App automatically detects when all 81 cells are correctly filled and triggers completion
- [x] **SCORE-03**: User can request a hint during play; a single unfilled correct cell value is revealed; hint usage is counted
- [x] **SCORE-04**: Each hint used deducts a fixed penalty from the final score
- [x] **SCORE-05**: On completion, user sees a summary showing error count, hints used, and final score
- [x] **SCORE-06**: Final score is error-based (fewer errors = higher score) with hint penalties applied

### High Scores

- [x] **HS-01**: Per-difficulty high scores are stored persistently on device
- [x] **HS-02**: After game completion, user is informed if they achieved a new personal best for that difficulty
- [x] **HS-03**: User can view a leaderboard screen showing top scores per difficulty

### Navigation & UI

- [x] **NAV-01**: App has a main menu with options to start a new game (with difficulty selection) and view the leaderboard
- [x] **UI-01**: All UI components are built using the MMD library wrapped in ThemeMMD
- [x] **UI-02**: No animations, ripple effects, or transitions are used anywhere in the app (E-ink compliance)
- [x] **UI-03**: All interactive touch targets are at minimum 56dp (E-ink usability on 800×480 display)

## v2 Requirements

### Hints

- **HINT-V2-01**: Strategy-revealing hints explain the solving technique for a cell rather than just revealing the answer

### Statistics

- **STAT-V2-01**: User can view per-difficulty statistics across sessions (games played, average errors, best score)

### Puzzle

- **PUZZ-V2-01**: User can manually enter a custom puzzle (import/input their own)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Real-time error highlighting | Deliberately silent — errors revealed at completion; aligns with mindful ethos |
| Timer / time-based scoring | Creates anxiety; contradicts Mudita device philosophy |
| Daily challenges / streaks | Obligation-inducing gamification loops; against mindful premise |
| Global / online leaderboards | No Google Services on MuditaOS K; local only |
| Multiplayer | Single-player offline experience by design |
| Achievements / badges / XP | Visual noise and extrinsic motivation loops; contrary to mindful design |
| Color themes / custom skins | E-ink is monochromatic; theming is irrelevant |
| Sound effects / haptics | E-ink audience values quiet focus |
| Cloud save / cross-device sync | No backend; local-only scope |
| Ad monetization | Violates distraction-free premise; likely GMS-incompatible |
| Auto-fill pencil marks | Reduces cognitive challenge; manual annotation is part of the game |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PUZZ-01 | Phase 1 | Complete |
| PUZZ-02 | Phase 1 | Complete (Sudoklify HIDDEN_PAIRS limitation — count-only medium differentiation) |
| PUZZ-03 | Phase 1 | Complete |
| DIFF-01 | Phase 2 | Complete |
| DIFF-02 | Phase 2 | Complete |
| INPUT-01 | Phase 2 | Complete |
| INPUT-02 | Phase 2 | Complete |
| INPUT-03 | Phase 2 | Complete |
| INPUT-04 | Phase 2 | Complete |
| INPUT-05 | Phase 2 | Complete |
| STATE-01 | Phase 4 | Complete |
| STATE-02 | Phase 4 | Complete |
| STATE-03 | Phase 4 | Complete |
| SCORE-01 | Phase 2 | Complete |
| SCORE-02 | Phase 2 | Complete |
| SCORE-03 | Phase 5 | Complete |
| SCORE-04 | Phase 5 | Complete |
| SCORE-05 | Phase 5 | Complete |
| SCORE-06 | Phase 5 | Complete |
| HS-01 | Phase 5 | Complete |
| HS-02 | Phase 5 | Complete |
| HS-03 | Phase 5 | Complete |
| NAV-01 | Phase 6 | Complete |
| UI-01 | Phase 3 | Complete |
| UI-02 | Phase 3 | Complete |
| UI-03 | Phase 3 | Complete |

**Coverage:**
- v1 requirements: 26 total
- Mapped to phases: 26
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-23*
*Last updated: 2026-03-23 after roadmap creation*
