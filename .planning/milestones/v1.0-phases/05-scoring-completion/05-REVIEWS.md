---
phase: 5
reviewers: [codex]
reviewed_at: 2026-03-24T00:00:00Z
plans_reviewed: [05-01-PLAN.md, 05-02-PLAN.md, 05-03-PLAN.md]
---

# Cross-AI Plan Review — Phase 5

> Note: Gemini not installed. Claude skipped (current runtime — independence rule).
> Single reviewer: Codex (OpenAI).

---

## Codex Review

### Plan 01 Review

**Summary**

Plan 01 is a solid foundation plan. It captures the core data-model changes, locks the score formula into a dedicated utility, and separates score persistence from in-progress game persistence in a way that fits the project architecture. The plan is generally well-scoped for Wave 1, but it leaves a few integration contracts underspecified, especially around serialization compatibility and repository behavior for invalid or absent values.

**Strengths**

- Separates score persistence from game-state persistence, which reduces coupling and avoids mixing lifecycle concerns.
- Encodes the locked scoring formula in one place, which lowers risk of duplication or drift.
- Includes backward-compatibility testing for saved game state without `hintCount`, which is important for existing installs.
- Introduces `ScoreRepository` early, enabling clean ViewModel injection in later waves.
- Includes test doubles and DataStore tests, which improves downstream implementation speed and confidence.
- `CompletionResult` is a sensible boundary object for passing completion data to UI/navigation.

**Concerns**

- `MEDIUM`: `GameEvent.Completed` and `CompletionResult` are both being introduced here, but their responsibility boundary is not fully stated. There is some risk of duplicated completion payloads or unclear ownership.
- `MEDIUM`: The plan specifies converters for `PersistedGameState` but does not explicitly say how unknown/missing JSON keys are handled in production code, only that there will be a test.
- `LOW`: `equals/hashCode` mention on `GameUiState` may be unnecessary depending on whether it is already a data class. This may indicate extra implementation work with little value.
- `LOW`: `NoOpScoreRepository` could mask wiring mistakes if used silently outside tests/previews. That is convenient, but it can hide integration failures.
- `LOW`: DataStore tests are mentioned, but not repository behavior for corrupted or negative stored values. Even if impossible through normal app flow, defensive behavior should be defined.

**Suggestions**

- Define clearly whether `GameEvent.Completed` carries a `CompletionResult` or whether the two are separate concepts used in different layers.
- Explicitly specify repository behavior for missing, corrupted, or negative scores: ideally return `null` for missing and clamp/reject invalid persisted values.
- Confirm whether `GameUiState` is already a `data class`; if so, remove the `equals/hashCode` wording to avoid noise.
- Add one repository test for "no stored score" and one for "stored value invalid/corrupt" handling.
- State that `hintCount` defaults to `0` during deserialization when absent, not just in constructor defaults.

**Risk Assessment**

**LOW-MEDIUM**. The plan is well-structured and appropriately scoped, with main risks coming from contract ambiguity rather than design failure. If the completion payload boundary is clarified, risk drops to low.

---

### Plan 02 Review

**Summary**

Plan 02 covers the core gameplay logic for hints, completion, and personal-best updates, and it is the most critical plan in this phase. It addresses the main game-loop requirements directly and keeps UI changes modest. The main weaknesses are around edge-case semantics: hint eligibility, random selection determinism, event ordering, and whether clearing game state immediately after completion could affect summary/resume behavior.

**Strengths**

- Correctly places hint and completion logic in the ViewModel, preserving MVVM separation.
- Explicitly avoids incrementing `errorCount` for hints, which aligns with the requirements.
- Includes completion after a hint fills the last cell, which is an easy edge case to miss.
- Refreshing leaderboard data after completion gives the UI a consistent source of truth.
- Keeps `ControlsRow` changes lightweight and aligned with the locked button decision.
- The test count is substantial for this wave, which is appropriate because most behavioral risk lives here.

**Concerns**

- `HIGH`: `requestHint()` says it selects from "unfilled non-correct cells," but the locked decision says "random unfilled non-correct cell." The wording is slightly ambiguous for cells with wrong user-entered values. If the game model allows filled-but-incorrect cells, this plan appears to exclude them from hint targets, which may or may not match product intent.
- `HIGH`: `clearGame()` inside `handleCompletion()` before or around event emission is risky. If completion UI depends on state still present in memory, ordering bugs can cause missing summary data or broken resume cleanup.
- `MEDIUM`: "does NOT touch undoStack" may be wrong depending on expected UX. If a hint changes the board and cannot be undone, that should be an explicit product decision, not an implementation detail.
- `MEDIUM`: Random hint selection can make tests flaky unless randomness is injected or seeded.
- `MEDIUM`: `leaderboardScores` refreshed on init and after completion is fine, but if persistence reads are asynchronous there is some risk of stale UI during screen transitions unless completion uses the freshly computed score and best-score result directly.
- `LOW`: `canRequestHint` computed locally in `GameScreen` is acceptable, but it duplicates business rules in UI if the eligibility logic becomes more complex later.

**Suggestions**

- Clarify hint target eligibility precisely:
  - If only empty cells are eligible, say "empty incorrect-by-definition cells."
  - If wrong filled cells should also be eligible, update the algorithm accordingly.
- Revisit completion ordering. Safer sequence is usually:
  1. compute completion result,
  2. persist high score if needed,
  3. emit completion event with payload,
  4. clear resumable game state after completion data is safely handed off.
- Decide explicitly whether hinted moves are undoable. If not, document that as a locked behavior and test it.
- Inject a `Random` provider or selection strategy so tests can be deterministic.
- Add tests for: no eligible hint cells; last-cell completion via hint; equal-to-best-score case; worse-than-best-score case; better-than-best-score case; repeated completion event prevention.

**Risk Assessment**

**MEDIUM-HIGH**. This plan likely achieves the phase goals, but it carries the most behavioral risk. The biggest concerns are hint semantics and completion-state ordering; both can cause user-visible bugs if implemented as written.

---

### Plan 03 Review

**Summary**

Plan 03 is pragmatic and mostly aligned with the UI and routing needs of this phase. It keeps navigation intentionally simple ahead of a later NavHost migration, respects the E-ink constraints, and covers the required summary and leaderboard surfaces. The main gaps are around MMD consistency, persistence-driven empty states, and whether the routing approach introduces state fragility in `MainActivity`.

**Strengths**

- Scope is restrained: simple screen switching now, NavHost later.
- Summary content matches the locked decision and the phase success criteria.
- Avoids animation-based UI patterns, which is correct for E-ink.
- Uses fixed-size, full-width action buttons and simple layout primitives suited to the device.
- Includes UI tests for both summary and leaderboard, which is appropriate for this screen-heavy wave.
- Explicitly handles the screen transition ordering pitfall by setting `completionResult` before switching screens.

**Concerns**

- `HIGH`: The plan mentions text content (e.g., "Puzzle Complete" bold heading) but does not explicitly require `TextMMD`/`ButtonMMD` everywhere. Given the project constraint (mandatory MMD-only components), that omission is important.
- `MEDIUM`: `LeaderboardScreen` shows one best score per difficulty, while the roadmap success criterion says "top scores for each difficulty level." D-07 says 3 rows per difficulty with placeholders, but D-08 only defines one DataStore key per difficulty. Potential requirements/design mismatch.
- `MEDIUM`: `MainActivity` local screen state can become fragile across configuration/process recreation unless `currentScreen` and `completionResult` are saved/restored.
- `LOW`: `Surface`, `BorderStroke`, `HorizontalDivider` usage — confirm these come from MMD-compatible theme expectations.
- `LOW`: UI tests focus on rendering; tap-target and accessibility assertions would also matter on 800×480 E-ink.

**Suggestions**

- Explicitly require `TextMMD` for all text and `ButtonMMD` for all actions in `SummaryScreen` and `LeaderboardScreen`.
- Resolve the leaderboard naming contradiction before implementation: either change requirement text to "best score per difficulty," or change persistence design to store top-N per difficulty.
- Add state restoration for `currentScreen` and `completionResult` in `MainActivity`, or explicitly accept that process death returns users to the game screen.
- Add UI tests for: personal-best banner visible/hidden; null-score placeholder rendering; button enabled behavior; minimum touch target sizing.
- Confirm all surfaces maintain strong contrast for E-ink rendering.

**Risk Assessment**

**MEDIUM**. The implementation itself is straightforward, but there is a significant product-model inconsistency around "leaderboard" versus "single best score per difficulty." If unresolved, the plan can ship something that passes code review but misses the stated phase goal.

---

## Consensus Summary

Single reviewer — no cross-reviewer consensus available. Key findings presented as single-source synthesis.

### Agreed Strengths

- **Clean architecture**: Data/persistence layer (Plan 01) → ViewModel logic (Plan 02) → UI/routing (Plan 03) is the correct dependency order. No circular dependencies.
- **E-ink compliance**: All three plans respect the no-animation, no-ripple, high-contrast constraints.
- **Test coverage**: All three waves include automated unit and Compose UI tests alongside implementation.
- **Score formula encapsulation**: Standalone `calculateScore()` function in Plan 01 is the right choice for isolation and testability.
- **Completion payload**: `CompletionResult` boundary object cleanly decouples ViewModel computation from UI display.

### Agreed Concerns

1. **[HIGH] Leaderboard vs. best-score naming mismatch** — HS-03 says "leaderboard showing top scores per difficulty" but storage (D-08) and the UI design (D-07) only support one score per difficulty. The word "leaderboard" implies multiple scores or rankings; the implementation delivers a best-score summary. This is a requirements contradiction to resolve before execution.

2. **[HIGH] Hint eligibility ambiguity** — Plan 02's hint algorithm targets `board[i] != solution[i]` which includes filled-but-wrong cells as well as empty cells. D-03 says "unfilled, non-correct cell." Whether wrong-but-filled cells should be eligible for hints is unspecified. Both interpretations are valid; pick one explicitly.

3. **[MEDIUM] Completion state ordering** — Codex flags that `clearGame()` before event emission could cause ordering issues if the summary screen needs any in-flight state. The RESEARCH.md already addresses this (score save → clear → emit), but the plan should explicitly confirm that `completionResult` is never null when `SummaryScreen` renders.

4. **[MEDIUM] Non-undoable hints as implicit decision** — Hints not pushing to undo stack is listed as an implementation detail. It should be a locked product decision with an explicit test asserting undoability === false for hints.

5. **[MEDIUM] Random hint selection in tests** — `candidates.random()` makes hint-cell tests non-deterministic unless randomness is injected. This can produce occasional flaky tests.

### Divergent Views

N/A — single reviewer. No divergent views to synthesize.

---

## Action Items for Replanning

Priority order:

1. **MUST resolve**: Leaderboard naming — decide "best score per difficulty" vs. "top-N per difficulty" and align REQUIREMENTS.md, CONTEXT.md, and plans
2. **MUST resolve**: Hint eligibility — confirm whether wrong-but-filled cells are hint targets (current code: yes, via `board[i] != solution[i]`)
3. **SHOULD address**: Make hints explicitly non-undoable as a locked decision (not implementation detail)
4. **SHOULD address**: Inject `Random` into `requestHint()` for test determinism
5. **CONSIDER**: State restoration for `currentScreen`/`completionResult` in `MainActivity` (or accept process-death behavior explicitly)
