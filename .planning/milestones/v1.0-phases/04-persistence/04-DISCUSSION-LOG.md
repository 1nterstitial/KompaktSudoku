# Phase 4: Persistence - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-24
**Phase:** 04-persistence
**Areas discussed:** Pause trigger, Resume entry point, Undo stack on resume

---

## Pause Trigger

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-save only | Save automatically on app backgrounding (onStop). No pause button. | ✓ |
| Explicit pause button | Player taps a button to trigger save. | |
| Both — auto-save + pause button | Auto-save on backgrounding AND an explicit button. | |

**User's choice:** Auto-save only
**Notes:** —

---

### Follow-up: Save frequency

| Option | Description | Selected |
|--------|-------------|----------|
| Backgrounding only | Save once when app goes to background. | ✓ |
| Every move + backgrounding | Save to DataStore after every digit entry / erase / pencil mark. | |

**User's choice:** Backgrounding only
**Notes:** Keeps it simple, avoids per-move DataStore write overhead on Helio A22.

---

## Resume Entry Point

| Option | Description | Selected |
|--------|-------------|----------|
| Dialog on GameScreen | Modal dialog on launch if saved game exists: Resume / New Game. | ✓ |
| Inline on GameScreen | Pre-populate saved state, show banner instead of modal. | |
| Stub main menu screen | Create a minimal MenuScreen with Resume + New Game buttons. | |

**User's choice:** Dialog on GameScreen (modal)
**Notes:** Phase 6 will replace this flow with the full navigation menu.

---

### Follow-up: New Game difficulty in Phase 4

| Option | Description | Selected |
|--------|-------------|----------|
| Hard-coded to Easy | Start new Easy game. Difficulty picker comes in Phase 6. | ✓ |
| Second dialog for difficulty | After 'New Game', show Easy / Medium / Hard dialog. | |

**User's choice:** Hard-coded to Easy for now
**Notes:** Keeps Phase 4 focused on persistence, not navigation.

---

## Undo Stack on Resume

| Option | Description | Selected |
|--------|-------------|----------|
| No — fresh undo state | Undo stack starts empty after resume. Simpler serialization. | ✓ |
| Yes — persist full undo stack | Save full undo history; player can undo across pause boundary. | |

**User's choice:** No — fresh undo state on resume
**Notes:** Avoids serializing GameAction types. Player can undo moves made after resuming only.

---

## Claude's Discretion

- Repository layer design and injection pattern
- Serialization data class shape (`PersistedGameState` vs annotating `GameUiState` directly)
- Lifecycle observer placement (Activity `onStop` vs `ProcessLifecycleOwner`)

## Deferred Ideas

None.
