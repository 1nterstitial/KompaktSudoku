# Roadmap: Mudita Kompakt Sudoku

**Project:** Mudita Kompakt Sudoku

## Milestones

- ✅ **v1.0 MVP** — Phases 1–6 (shipped 2026-03-25)
- 🔲 **v1.1 Bug Fixes and Improvements** — Phases 7–9 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–6) — SHIPPED 2026-03-25</summary>

- [x] Phase 1: Puzzle Engine (5/5 plans) — completed 2026-03-24
- [x] Phase 2: Game State & Domain (3/3 plans) — completed 2026-03-24
- [x] Phase 3: Core Game UI (5/5 plans) — completed 2026-03-24
- [x] Phase 4: Persistence (3/3 plans) — completed 2026-03-25
- [x] Phase 5: Scoring & Completion (3/3 plans) — completed 2026-03-25
- [x] Phase 6: Menu & Navigation (2/2 plans) — completed 2026-03-25

Full phase details: `.planning/milestones/v1.0-ROADMAP.md`

</details>

### v1.1 Bug Fixes and Improvements

- [x] **Phase 7: Grid Rendering Fixes** — Pencil marks visible and correctly sized in all cell states (completed 2026-03-27)
- [x] **Phase 8: Controls & Number Pad Fixes** — All control buttons visually correct and grouped (completed 2026-03-27)
- [ ] **Phase 9: Game Navigation** — Back-press dialog and save/resume work reliably

## Phase Details

### Phase 7: Grid Rendering Fixes
**Goal**: Pencil marks are always readable regardless of cell selection state
**Depends on**: Nothing (self-contained Canvas changes in GameGrid.kt)
**Requirements**: GRID-01, GRID-02
**Success Criteria** (what must be TRUE):
  1. Pencil mark digits in a selected (black-background) cell display in white, not black
  2. Pencil mark digits are legible at all grid sizes — no clipping when all 9 marks occupy one cell
  3. Pencil mark font size adapts to the rendered cell size, not a fixed hardcoded value
**Plans**: 1 plan
Plans:
- [x] 07-01-PLAN.md — Pencil mark cap (4-mark limit) + 2x2 dynamic layout + white-on-selected color

**UI hint**: yes

### Phase 8: Controls & Number Pad Fixes
**Goal**: All control buttons communicate their state and grouping clearly on the E-ink display
**Depends on**: Nothing (self-contained changes in ControlsRow.kt and NumberPad.kt)
**Requirements**: CTRL-01, CTRL-02, CTRL-03, CTRL-04
**Success Criteria** (what must be TRUE):
  1. Number pad digit labels (1–9) appear vertically centered within their buttons
  2. The "Get Hint" button label reads as two lines, both centered within the button bounds
  3. The inactive Fill or Pencil mode button is visually distinct from the active one (mid-gray background vs. solid black)
  4. The Fill and Pencil buttons are enclosed in a shared border frame that visually separates them from the Undo and Get Hint buttons
**Plans**: 1 plan
Plans:
- [x] 08-01-PLAN.md — Controls row border frame, inactive gray, two-line hint, condensed number pad font

**UI hint**: yes

### Phase 9: Game Navigation
**Goal**: Players can safely exit a game via Back and trust that their progress is preserved
**Depends on**: Nothing (self-contained changes in GameScreen.kt and GameViewModel.kt)
**Requirements**: NAV-01, NAV-02, NAV-03
**Success Criteria** (what must be TRUE):
  1. Pressing Back during an active game shows a confirmation overlay with "Save and Exit" and "Forfeit" options — the back press does not immediately exit
  2. Selecting "Save and Exit" saves the current game state and returns to the main menu, where the Resume button is immediately visible
  3. Selecting "Forfeit" discards the saved game state and returns to the main menu without a Resume button
  4. If the app is force-closed mid-game, the saved game state persists and the Resume button appears on the next launch
**Plans**: 1 plan
Plans:
- [ ] 09-01-PLAN.md — Back-press exit dialog with Save and Exit / Forfeit + quitGame() ViewModel function

**UI hint**: yes

## Progress Table

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Puzzle Engine | v1.0 | 5/5 | Complete | 2026-03-24 |
| 2. Game State & Domain | v1.0 | 3/3 | Complete | 2026-03-24 |
| 3. Core Game UI | v1.0 | 5/5 | Complete | 2026-03-24 |
| 4. Persistence | v1.0 | 3/3 | Complete | 2026-03-25 |
| 5. Scoring & Completion | v1.0 | 3/3 | Complete | 2026-03-25 |
| 6. Menu & Navigation | v1.0 | 2/2 | Complete | 2026-03-25 |
| 7. Grid Rendering Fixes | v1.1 | 1/1 | Complete   | 2026-03-27 |
| 8. Controls & Number Pad Fixes | v1.1 | 1/1 | Complete | 2026-03-27 |
| 9. Game Navigation | v1.1 | 0/1 | Not started | - |
