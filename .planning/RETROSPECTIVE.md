# Retrospective

## Milestone: v1.0 — MVP

**Shipped:** 2026-03-25
**Phases:** 6 | **Plans:** 21 | **Tasks:** 38
**Timeline:** 2026-03-23 → 2026-03-25 (2 days)

### What Was Built

- Puzzle engine: Sudoklify-backed generator with 3-gate acceptance loop (uniqueness, given-count, technique-tier) producing valid, difficulty-classified puzzles
- Game domain: GameViewModel with full game loop — selection, fill, pencil marks, LIFO undo, silent error tracking, completion detection
- E-ink UI: Canvas-based game grid, MMD-compliant number pad and controls, validated on physical Mudita Kompakt hardware
- Persistence: DataStore-backed pause/resume preserving all game state; per-difficulty high score storage
- Completion flow: Hint mechanics, score formula, SummaryScreen + LeaderboardScreen with personal best detection
- Navigation: Full 5-screen flow (Menu → Difficulty → Game → Summary → Leaderboard) with reactive StateFlow-driven Resume button

### What Worked

- **TDD discipline held throughout**: Wave 0 @Ignore stubs established contracts before implementation; near-zero test regressions across 21 plans
- **Worktree execution**: Each plan ran in an isolated git worktree — no mid-plan context bleed between phases; atomic commits per task
- **JAR bytecode inspection for Sudoklify**: When official docs and plan examples disagreed with reality, inspecting the AAR/JAR directly gave authoritative API paths
- **Physical device verification early (Phase 3)**: Validating E-ink rendering and touch targets before persistence/scoring meant no late-stage display regressions
- **Screen enum routing**: Simple and sufficient for 5 screens — avoided NavHost overhead without any UX cost
- **Completion state ordering**: Setting `completionResult` before `currentScreen` in onCompleted callback — discovered as a necessary pattern (Pitfall 2), documented for reuse

### What Was Inefficient

- **ROADMAP.md progress table not kept in sync**: Plans were marked `[x]` in phase lists but the progress table rows still showed "In Progress" after completion. Auditor had to infer state from VERIFICATION.md and SUMMARY.md rather than the table.
- **Noisy STATE.md performance table**: Performance rows accumulated in inconsistent formats (some `Phase 02 P02 | 4 | 1 tasks | 2 files`, some `| 02 | 01 | 6 min | 3 | 17 | 2026-03-24 |`) — requires cleanup each milestone
- **MMD credential setup as a plan**: Plan 03-04 (scoped GitHub Packages repo) was necessary but added planning overhead for what is essentially a one-time dev setup. Could be a setup doc instead.
- **VALIDATION.md files never updated post-execution**: All 6 remain `nyquist_compliant: false` / `draft` — created at planning time, never reconciled with actual test results. Future milestones should include a Nyquist closure sweep before milestone completion.

### Patterns Established

- **3-gate puzzle generator**: Uniqueness check → given-count range → technique-tier classification; each gate independently testable
- **Injectable dependencies for testing**: Generator as suspend lambda into GameViewModel; Random as constructor param in GameViewModel; ioDispatcher injected — all enable deterministic tests without subclassing
- **BasicAlertDialog + Surface for E-ink dialogs**: Avoids ripple from AlertDialog's built-in button slots; monochromatic border via `BorderStroke(1.dp, Color.Black)`
- **assertDoesNotExist() for conditionally absent nodes**: When a composable is removed by an `if` block (not just hidden), `assertDoesNotExist()` is correct; `assertIsNotDisplayed()` will fail
- **Completion state ordering**: Set data state (completionResult) before routing state (currentScreen) to prevent null dereference on first recomposition
- **wasAdded flag on pencil mark action**: Encodes toggle direction at write time — O(1) undo without re-reading set state

### Key Lessons

- **Sudoklify preset distribution is bimodal**: MEDIUM preset produces ~40% NAKED_SINGLES and ~60% ADVANCED — no HIDDEN_PAIRS tier exists. Verify empirically before relying on library difficulty claims.
- **Sudoklify API diverges from docs**: Actual package paths (`components.toSeed`, `presets.loadPresetSchemas`) differ from README examples. Always verify from JAR bytecode for beta libraries.
- **DataStore file-per-instance rule**: `gameDataStore` and `scoreDataStore` must be separate extension properties on Context — sharing one DataStore file causes data corruption.
- **MMD ButtonMMD is in plural package**: `com.mudita.mmd.components.buttons.ButtonMMD` (not `button`) — verified from AAR class inspection.

### Cost Observations

- Sessions: ~5 (one per phase roughly)
- Notable: Physical device validation (Plan 03-05) added a full plan but was the right call — E-ink ghosting and touch target issues cannot be caught by Robolectric

---

## Cross-Milestone Trends

| Milestone | Phases | Plans | Timeline | LOC |
|-----------|--------|-------|----------|-----|
| v1.0 MVP | 6 | 21 | 2 days | ~6,650 Kotlin |
