# Requirements: Mudita Kompakt Sudoku

**Milestone:** v1.1 Bug Fixes and Improvements
**Defined:** 2026-03-26
**Core Value:** A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

## v1.1 Requirements

### Grid Rendering

- [ ] **GRID-01**: Pencil mark digits display in white when their cell is selected (black background)
- [ ] **GRID-02**: Pencil mark font size scales dynamically from cell size to fit a 2×2 (4-mark) arrangement at maximum

### Controls & Number Pad

- [ ] **CTRL-01**: Number pad digit text is vertically centered on its button with a condensed/taller font (`sans-serif-condensed`)
- [ ] **CTRL-02**: "Get Hint" button text is vertically centered within the button (two lines, centered)
- [ ] **CTRL-03**: Inactive Fill/Pencil mode button displays a solid mid-gray background (`#E0E0E0`); active button retains solid black
- [ ] **CTRL-04**: Fill and Pencil buttons are visually grouped with a 1dp black border frame separating them from Undo/Get Hint

### Game Navigation

- [ ] **NAV-01**: Pressing Back during an active game shows a confirmation overlay with "Return to Menu" (saves game) and "Quit Game" (discards save) options
- [ ] **NAV-02**: After selecting "Return to Menu", the Resume button appears on the main menu
- [ ] **NAV-03**: After an unexpected app kill, the saved game state is preserved and the Resume button appears on next launch

## Future Requirements

_(None identified — v1.1 is scoped to bug fixes and cosmetic improvements from first real-device play session)_

## Out of Scope

- Timer / time-based scoring — creates anxiety; contradicts Mudita device philosophy
- Daily challenges / streaks — obligation-inducing gamification
- Online leaderboards — no Google Services on MuditaOS K
- Auto-fill pencil marks — reduces cognitive challenge
- Undo across sessions — per decision D-05; undo stack not persisted
- Accessibility / screen reader support — Canvas-based grid; deferred to future milestone

## Traceability

| REQ-ID | Phase | Plan |
|--------|-------|------|
| GRID-01 | Phase 7 | Pending |
| GRID-02 | Phase 7 | Pending |
| CTRL-01 | Phase 8 | Pending |
| CTRL-02 | Phase 8 | Pending |
| CTRL-03 | Phase 8 | Pending |
| CTRL-04 | Phase 8 | Pending |
| NAV-01 | Phase 9 | Pending |
| NAV-02 | Phase 9 | Pending |
| NAV-03 | Phase 9 | Pending |

---
*Last updated: 2026-03-25 — traceability mapped to Phases 7–9*
