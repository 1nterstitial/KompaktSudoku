# Roadmap: Mudita Kompakt Sudoku

**Project:** Mudita Kompakt Sudoku
**Created:** 2026-03-23
**Granularity:** Standard
**Coverage:** 26/26 v1 requirements mapped

## Phases

- [x] **Phase 1: Puzzle Engine** - Pure Kotlin generator producing valid, unique, technique-classified puzzles (completed 2026-03-24)
- [ ] **Phase 2: Game State & Domain** - ViewModel contract, domain models, input handling, and error tracking
- [ ] **Phase 3: Core Game UI** - E-ink-native playable game screen validated on physical hardware
- [ ] **Phase 4: Persistence** - Pause/resume and high score storage via DataStore
- [ ] **Phase 5: Scoring & Completion** - Hint logic, score computation, completion summary, and leaderboard screens
- [ ] **Phase 6: Menu & Navigation** - Full navigation graph assembling all screens into a complete app flow

## Phase Details

### Phase 1: Puzzle Engine
**Goal**: The app can generate valid Sudoku puzzles at three difficulty levels, each guaranteed to have exactly one solution
**Depends on**: Nothing
**Requirements**: PUZZ-01, PUZZ-02, PUZZ-03
**Success Criteria** (what must be TRUE):
  1. Generating 20 puzzles at any difficulty level produces zero puzzles with more than one valid solution
  2. Easy puzzles consistently have 36–45 given cells and are solvable using naked singles only
  3. Medium puzzles consistently have 27–35 given cells and require hidden pairs or pencil mark reasoning to solve
  4. Hard puzzles consistently have 22–27 given cells and require advanced techniques (X-wing, chains) to solve
  5. Puzzle generation completes in under 2 seconds on a Mudita Kompakt device
**Plans**: 4 plans

Plans:
- [x] 01-01-PLAN.md — Gradle setup, domain models (Difficulty/DifficultyConfig/SudokuPuzzle), Wave 0 test stubs
- [x] 01-02-PLAN.md — SudokuValidator + UniquenessVerifier (abort-on-second-solution, PUZZ-01)
- [x] 01-03-PLAN.md — DifficultyClassifier (constraint-propagation solver with technique tracking, PUZZ-02)
- [x] 01-04-PLAN.md — SudokuGenerator (Sudoklify wrapper + retry loop) + integration batch tests (PUZZ-01/02/03)

### Phase 2: Game State & Domain
**Goal**: The game loop is fully modeled — difficulty is selectable, cells accept input, errors are tracked silently, and undo works — all backed by a stable ViewModel contract
**Depends on**: Phase 1
**Requirements**: DIFF-01, DIFF-02, INPUT-01, INPUT-02, INPUT-03, INPUT-04, INPUT-05, SCORE-01, SCORE-02
**Success Criteria** (what must be TRUE):
  1. User can choose Easy, Medium, or Hard before starting; the generated puzzle matches the selected difficulty's cell count and technique classification
  2. Tapping a cell marks it as selected; tapping a digit fills it; the selection state updates immediately with no visual delay
  3. User can switch between fill mode and pencil mark mode; digits entered in each mode are stored and displayed separately
  4. Tapping undo reverses the last fill or pencil mark action, including multi-step sequences
  5. Filling an incorrect cell increments the silent error counter without displaying any error feedback to the player; filling the last correct cell triggers completion detection
**Plans**: 3 plans

Plans:
- [ ] 02-01-PLAN.md — Domain models (GameUiState, InputMode, GameAction, GameEvent) + test infrastructure (MainDispatcherRule, FakeGenerator, GameUiState tests)
- [ ] 02-02-PLAN.md — GameViewModel core actions (startGame, selectCell, enterDigit fill, toggleInputMode) + tests for DIFF-01/02, INPUT-01/02/03
- [ ] 02-03-PLAN.md — GameViewModel pencil marks, undo, error tracking, completion detection + tests for INPUT-04/05, SCORE-01/02

### Phase 3: Core Game UI
**Goal**: The game screen renders correctly on the Mudita Kompakt E-ink display with no ghosting artifacts, appropriately sized touch targets, and full MMD library compliance
**Depends on**: Phase 2
**Requirements**: UI-01, UI-02, UI-03
**Success Criteria** (what must be TRUE):
  1. The complete game screen (grid + number pad) is wrapped in ThemeMMD and uses only MMD components; no standard Material or custom non-MMD widgets are present
  2. No animations, ripple effects, or transitions occur anywhere in the app when tested on physical hardware
  3. All interactive elements (cells, digit buttons, mode toggle, undo) have a minimum touch target of 56dp and are reliably activatable with a single tap on the physical device
  4. After 30+ successive cell interactions on the physical device, no visible ghosting artifacts remain on the display
**Plans**: TBD
**UI hint**: yes

### Phase 4: Persistence
**Goal**: A paused game survives app closure and device sleep, and high scores are stored durably per difficulty level
**Depends on**: Phase 3
**Requirements**: STATE-01, STATE-02, STATE-03
**Success Criteria** (what must be TRUE):
  1. Pausing a game and force-closing the app then reopening it presents a prompt to resume; all grid state, pencil marks, error count, and hint count are restored exactly as left
  2. Resuming a paused game and making additional moves does not corrupt the saved state on a subsequent pause
  3. Starting a new game after a paused game exists correctly discards the prior paused state and starts fresh
**Plans**: TBD

### Phase 5: Scoring & Completion
**Goal**: The closed game loop is complete — hints are available with a score penalty, the game detects completion, and the player sees a meaningful summary with their score compared to their personal best
**Depends on**: Phase 4
**Requirements**: SCORE-03, SCORE-04, SCORE-05, SCORE-06, HS-01, HS-02, HS-03
**Success Criteria** (what must be TRUE):
  1. Tapping the hint button reveals exactly one unfilled correct cell value; the hint count increments; the hint cannot be used on a cell that already contains a correct value
  2. On puzzle completion, the summary screen shows the exact error count, hints used, and computed final score; the score is never negative
  3. The final score is calculated as a function of errors and hints used, where fewer errors and fewer hints produce a higher score
  4. If the completed game's score is a personal best for that difficulty, the summary screen displays a new personal best notification
  5. The leaderboard screen shows the top scores for each difficulty level, drawn from persistent storage
**Plans**: TBD
**UI hint**: yes

### Phase 6: Menu & Navigation
**Goal**: The app has a complete navigation flow from main menu through game to summary and leaderboard, with the resume prompt correctly driven by persisted state
**Depends on**: Phase 5
**Requirements**: NAV-01
**Success Criteria** (what must be TRUE):
  1. The main menu presents options to start a new game (with difficulty selection) and to view the leaderboard
  2. When a paused game exists, the main menu also presents a resume option; when no paused game exists, the resume option is absent
  3. Completing a game navigates to the summary screen; from summary the player can return to the main menu or view the leaderboard
  4. The back stack is coherent throughout — pressing back at the main menu exits the app; pressing back mid-game returns to the menu (with the game automatically paused)
**Plans**: TBD
**UI hint**: yes

## Progress Table

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Puzzle Engine | 4/4 | Complete   | 2026-03-24 |
| 2. Game State & Domain | 0/3 | Planned | - |
| 3. Core Game UI | 0/0 | Not started | - |
| 4. Persistence | 0/0 | Not started | - |
| 5. Scoring & Completion | 0/0 | Not started | - |
| 6. Menu & Navigation | 0/0 | Not started | - |
