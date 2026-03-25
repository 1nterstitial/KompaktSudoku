# Milestones

## v1.0 MVP (Shipped: 2026-03-25)

**Phases completed:** 6 phases, 21 plans, 38 tasks
**Timeline:** 2026-03-23 → 2026-03-25 (2 days)
**Codebase:** ~6,650 lines of Kotlin, 138 commits

**Key accomplishments:**

- Sudoklify-backed puzzle generator with 3-gate acceptance loop (uniqueness + givenCount + technique-tier) — all 28 puzzle engine tests green, PUZZ-01/02/03 verified
- Full game loop in GameViewModel: cell selection, fill/pencil mark, LIFO undo stack, silent error tracking, and completion detection — all 9 requirements TDD-verified
- E-ink–native game screen validated on physical Mudita Kompakt hardware — no ghosting after 30+ interactions, no ripple effects, all touch targets ≥56dp confirmed
- Pause/resume via DataStore: full game state (board, pencil marks, error count, hint count) survives force-close; ResumeDialog presented on next launch
- Hint mechanics with injectable Random, error-based score formula (max(0, 100 − errors×10 − hints×5)), SummaryScreen + LeaderboardScreen with personal best detection, all DataStore-persisted
- Full 5-screen navigation: Menu → Difficulty → Game → Summary → Leaderboard with reactive StateFlow-driven Resume button; 22 Robolectric tests across all navigation screens

**Archive:** `.planning/milestones/v1.0-ROADMAP.md` | `.planning/milestones/v1.0-REQUIREMENTS.md`

---
