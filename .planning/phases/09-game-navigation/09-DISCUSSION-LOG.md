# Phase 9: Game Navigation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-26
**Phase:** 09-game-navigation
**Areas discussed:** Dialog state ownership, Dialog visual design, Quit Game clear path, Back during loading

---

## Dialog State Ownership

| Option | Description | Selected |
|--------|-------------|----------|
| Local composable state | `var showExitDialog by remember { mutableStateOf(false) }` in GameScreen.kt. Simple, no ViewModel changes, purely a UI concern. | ✓ |
| GameUiState field | Add `showExitDialog: Boolean` to `GameUiState`. ViewModel-owned, fully unit-testable. Requires updating equals/hashCode and adding a ViewModel action. | |

**User's choice:** Local composable state
**Notes:** Simpler, no ViewModel complexity for a UI-only concern.

---

## Dialog Visual Design

### Overlay approach

| Option | Description | Selected |
|--------|-------------|----------|
| Centered Box overlay | White Box with 1dp black border + RectangleShape centered over game. Two ButtonMMD buttons stacked vertically. Semi-opaque scrim behind it. | ✓ |
| Full-screen overlay | Full-screen Box covers game entirely with buttons centered. No scrim, no border box. | |
| Inline — replace controls | Controls area replaced with dialog buttons when Back is pressed. | |

**User's choice:** Centered Box overlay

### Dialog text

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — short message | A single TextMMD line above the buttons gives context. | ✓ |
| No — buttons only | Button labels are self-explanatory. | |

**User's choice:** Yes — include a short message

### Dialog copy

| Option | Description | Selected |
|--------|-------------|----------|
| "Leave this game?" / "Return to Menu" / "Quit Game" | Standard, familiar labels. | |
| "Pause game?" / "Save and Exit" / "Quit without Saving" | More explicit but longer. | |
| You decide | Claude picks copy. | |
| Custom (user entered) | "Leave game?" / "Save and Exit" / "Forfeit" | ✓ |

**User's choice:** Custom — "Leave game?" / "Save and Exit" / "Forfeit"
**Notes:** User typed this directly as their preferred copy.

---

## Quit Game Clear Path

### What gets cleared

| Option | Description | Selected |
|--------|-------------|----------|
| Clear DataStore save + reset in-memory state | `repository.clearGame()` AND `_uiState.value = GameUiState()`. No Resume button on return to menu. | ✓ |
| Clear DataStore save only | `repository.clearGame()` only. In-memory state stays but DataStore is clear. | |

**User's choice:** Clear DataStore save + reset in-memory state

### Implementation approach

| Option | Description | Selected |
|--------|-------------|----------|
| New ViewModel function: `quitGame()` | Explicit, named, testable. | ✓ |
| Reuse `startNewGame()` | Already clears save but also starts a new Easy puzzle — wrong behavior. | |
| Inline in the callback | Bypasses ViewModel encapsulation. | |

**User's choice:** New `quitGame()` ViewModel function

---

## Back During Loading

| Option | Description | Selected |
|--------|-------------|----------|
| Disabled during loading | `BackHandler` only active when `!uiState.isLoading`. System back exits app. | ✓ |
| Show dialog during loading too | BackHandler always active; "Save and Exit" is a no-op during loading due to saveNow() guards. | |
| You decide | Claude picks safest approach. | |

**User's choice:** Disabled during loading

---

## Claude's Discretion

- Exact scrim alpha value
- Padding and sizing of the centered dialog box
- Whether to add Compose UI tests for the dialog dismissal flow

## Deferred Ideas

None — discussion stayed within phase scope.
