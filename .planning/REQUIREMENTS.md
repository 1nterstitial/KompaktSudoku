# Requirements: Mudita Kompakt Sudoku

**Defined:** 2026-03-23
**Core Value:** A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

## v1 Requirements

### Puzzle Generation

- [ ] **PUZZ-01**: App generates valid Sudoku puzzles that have exactly one solution
- [ ] **PUZZ-02**: Difficulty is classified by required solving technique: Easy uses naked singles only; Medium requires hidden pairs/pencil marks; Hard requires advanced techniques (X-wing, chains)
- [ ] **PUZZ-03**: Easy puzzles provide approximately 36–45 given cells; Medium 27–35; Hard 22–27

### Difficulty

- [ ] **DIFF-01**: User can choose Easy, Medium, or Hard difficulty from the main menu before starting a game
- [ ] **DIFF-02**: Each difficulty level generates puzzles matching the cell count and technique classification defined in PUZZ-02 and PUZZ-03

### Input & Interaction

- [ ] **INPUT-01**: User can tap a cell to select it; selected cell is visually highlighted
- [ ] **INPUT-02**: User can tap a digit (1–9) to fill the selected cell with that value
- [ ] **INPUT-03**: User can toggle between "fill" mode and "pencil mark" mode for input
- [ ] **INPUT-04**: User can add and remove pencil mark candidates in any cell
- [ ] **INPUT-05**: User can undo the last move (fill or pencil mark action)

### Game State

- [ ] **STATE-01**: User can pause a game mid-play; full grid state, pencil marks, error count, and hint count are persisted to device storage
- [ ] **STATE-02**: On app launch, if a paused game exists, user is prompted to resume it or start a new game
- [ ] **STATE-03**: User can resume a paused game and continue exactly where they left off

### Scoring & Completion

- [ ] **SCORE-01**: Errors are tracked silently during play and not surfaced until the game ends
- [ ] **SCORE-02**: App automatically detects when all 81 cells are correctly filled and triggers completion
- [ ] **SCORE-03**: User can request a hint during play; a single unfilled correct cell value is revealed; hint usage is counted
- [ ] **SCORE-04**: Each hint used deducts a fixed penalty from the final score
- [ ] **SCORE-05**: On completion, user sees a summary showing error count, hints used, and final score
- [ ] **SCORE-06**: Final score is error-based (fewer errors = higher score) with hint penalties applied

### High Scores

- [ ] **HS-01**: Per-difficulty high scores are stored persistently on device
- [ ] **HS-02**: After game completion, user is informed if they achieved a new personal best for that difficulty
- [ ] **HS-03**: User can view a leaderboard screen showing top scores per difficulty

### Navigation & UI

- [ ] **NAV-01**: App has a main menu with options to start a new game (with difficulty selection) and view the leaderboard
- [ ] **UI-01**: All UI components are built using the MMD library wrapped in ThemeMMD
- [ ] **UI-02**: No animations, ripple effects, or transitions are used anywhere in the app (E-ink compliance)
- [ ] **UI-03**: All interactive touch targets are at minimum 56dp (E-ink usability on 800×480 display)

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

*Populated during roadmap creation.*

| Requirement | Phase | Status |
|-------------|-------|--------|
| PUZZ-01 | — | Pending |
| PUZZ-02 | — | Pending |
| PUZZ-03 | — | Pending |
| DIFF-01 | — | Pending |
| DIFF-02 | — | Pending |
| INPUT-01 | — | Pending |
| INPUT-02 | — | Pending |
| INPUT-03 | — | Pending |
| INPUT-04 | — | Pending |
| INPUT-05 | — | Pending |
| STATE-01 | — | Pending |
| STATE-02 | — | Pending |
| STATE-03 | — | Pending |
| SCORE-01 | — | Pending |
| SCORE-02 | — | Pending |
| SCORE-03 | — | Pending |
| SCORE-04 | — | Pending |
| SCORE-05 | — | Pending |
| SCORE-06 | — | Pending |
| HS-01 | — | Pending |
| HS-02 | — | Pending |
| HS-03 | — | Pending |
| NAV-01 | — | Pending |
| UI-01 | — | Pending |
| UI-02 | — | Pending |
| UI-03 | — | Pending |

**Coverage:**
- v1 requirements: 26 total
- Mapped to phases: 0 (pending roadmap)
- Unmapped: 26 ⚠️

---
*Requirements defined: 2026-03-23*
*Last updated: 2026-03-23 after initial definition*
