# Phase 3: Core Game UI - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-24
**Phase:** 03-core-game-ui
**Areas discussed:** Grid & cell visuals, Number pad layout, Screen composition

---

## Grid & Cell Visuals

### Box/cell borders

| Option | Description | Selected |
|--------|-------------|----------|
| Thick+thin borders | 3×3 box boundaries thick (2–3dp), cell borders thin (1dp) | ✓ |
| Uniform borders + background fill | Same border weight; boxes differentiated by alternating fill | |

**User's choice:** Thick+thin borders — accepted the visual preview mockup.

### Selected cell highlight

| Option | Description | Selected |
|--------|-------------|----------|
| Filled black background | Solid black fill, white digit | ✓ |
| Bold border | Thicker border only, no fill change | |
| Inverted + row/col highlight | Inverted cell + tinted same row/col | |

**User's choice:** Filled black background.

### Given vs player-entered cells

| Option | Description | Selected |
|--------|-------------|----------|
| Bold vs regular weight | Givens bold, player entries regular | ✓ |
| Gray background on givens | Givens get light gray fill | |
| You decide | Claude chooses MMD typography approach | |

**User's choice:** Bold typeface for givens, regular for player entries.

---

## Number Pad Layout

### Button arrangement

| Option | Description | Selected |
|--------|-------------|----------|
| Row of 9 buttons | 1–9 in single horizontal row | ✓ |
| 3×3 grid | Phone numpad layout | |

**User's choice:** Row of 9 — accepted the visual preview.

### Erase button

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — separate Erase button | Dedicated × button to clear cell | ✓ |
| No — tap digit again to erase | Tap placed digit to remove | |
| You decide | Claude picks per conventions | |

**User's choice:** Dedicated Erase button.

---

## Screen Composition

### Vertical layout

| Option | Description | Selected |
|--------|-------------|----------|
| Grid top, controls mid, pad bottom | Difficulty label → Grid → Fill/Pencil+Undo → Number pad | ✓ |
| Grid only, pad overlays on selection | Full-screen grid, pad instant-appears on cell select | |
| Grid left, pad right (landscape split) | 60/40 landscape split | |

**User's choice:** Standard vertical stack — accepted the mockup layout.

### Mode toggle

| Option | Description | Selected |
|--------|-------------|----------|
| Toggle button row | Two ButtonMMD: Fill / Pencil, active visually indicated | ✓ |
| Single tap-to-toggle button | One button cycling modes | |
| You decide | Claude picks clearest MMD approach | |

**User's choice:** Two ButtonMMD buttons side-by-side.

---

## Claude's Discretion

- Pencil mark display inside cells (mini grid vs compact list)
- Erase button exact placement within pad row
- Loading state presentation (isLoading = true)
- Error cell visual treatment (subtle, per silent-error design)

## Deferred Ideas

None surfaced during discussion.
