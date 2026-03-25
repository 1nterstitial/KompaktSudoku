# Feature Landscape

**Domain:** Mobile Sudoku game (E-ink, mindful/minimalist)
**Researched:** 2026-03-23
**Overall confidence:** HIGH (core Sudoku features well-documented; E-ink specifics MEDIUM)

---

## Table Stakes

Features users expect in any Sudoku app. Missing = product feels broken or incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Puzzle generation | Core product — no puzzles, no game | High | Generate-then-verify approach: fill valid grid via backtracking, then remove cells. Difficulty controlled by solving technique required, NOT just cell count. |
| Three difficulty levels | Universal Sudoku convention (Easy / Medium / Hard) | Low | Easy: 36-45 givens, naked singles only. Medium: 27-35, requires pencil marks/pairs. Hard: 22-27, requires X-wing/advanced chains. Exact range less important than technique classification. |
| Cell selection + number entry | Primary input interaction | Medium | Tap cell to select, tap digit to fill. Must feel immediate on E-ink — no animation feedback delay. Large tap targets required (800x480 px means ~89x53px cells minimum). |
| Undo | Universal expectation; misplaced taps happen constantly on touch screens | Low | Single-level undo acceptable; unlimited undo preferred. Critical on E-ink where accidental taps are common due to delayed visual feedback. |
| Pencil marks / candidate notes | Expected by any intermediate-to-advanced solver | Medium | Small digit annotations in cell corners. Toggle between "fill" and "note" input modes. Without this, Medium+ puzzles are unsolvable for most players. |
| Incorrect entry detection | Players expect to know when they've made an error, even if only at the end | Low | This project uses silent tracking (revealed at game end). Still table stakes — the question is WHEN to surface it, not WHETHER to track it. |
| Game completion detection | Automatic puzzle-solved recognition | Low | Validate all 81 cells filled correctly; trigger completion screen. |
| Completion summary | Players need closure and feedback on performance | Low | Show error count, hints used, final score at minimum. This project already specifies this. |
| Pause and resume | Players stop mid-puzzle constantly; state loss = frustration | Medium | Persist full grid state, notes, and error count. On E-ink devices especially — users put devices down for hours. |
| Puzzle uniqueness | Each puzzle must have exactly one valid solution | High | Standard constraint for Sudoku generation. Violation destroys player trust. Verify at generation time. |

---

## Differentiators

Features that set a product apart within the Sudoku genre. Not universally expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Error-based scoring (not time-based) | Aligns with mindful ethos — quality over speed | Low | Fewer errors = higher score. Fixed penalty per hint used. This is a deliberate design statement: E-ink users aren't in a hurry. |
| Silent error tracking (revealed at end) | Encourages reflection over reactive correction; more cognitively engaging | Low | Contrast: most apps use real-time auto-check. Silent tracking forces players to own their reasoning process. Supports the mindful angle. |
| Per-difficulty high score table | Personal progression per level; motivates replay at harder difficulties | Low | Local device only. Three separate leaderboards (Easy / Medium / Hard). Simple and satisfying. |
| Hint with score penalty (not disqualification) | Keeps game flowing; penalty is softer than hard-blocking | Low | One hint deducts a fixed amount from final score. Player can still finish and score. Removes anxiety of "wasting" the game by asking for help. |
| Hint quality: strategy-revealing vs cell-revealing | Teaching hints ("this cell must be X because...") are far more valuable than "the answer is 5" | High | Zach Gage's Good Sudoku showed this is a major differentiator. High complexity to implement well. Simpler fallback: reveal a single correct cell value. For v1, cell-reveal hint is acceptable. |
| MMD / E-ink native UI | App feels designed for the device, not ported | Medium | Uses ThemeMMD, high-contrast layout, full-width grid. No ripple effects, no animations. Instant state updates. This is the primary differentiator vs. any generic Android Sudoku port. |
| Distraction-free, no ads / no social | The mindful device has a specific audience who chose it deliberately | Low | No banners, no notification prompts, no "rate this app" pop-ups. Silence is a feature. |
| Graceful handling of incomplete puzzles on relaunch | User trust on an E-ink device where battery/power cycles happen | Low | Resume prompt on launch if a paused game exists. Prevents data loss surprise. |

---

## Anti-Features

Features to explicitly NOT build. Each is a deliberate omission, not an oversight.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Animations and transitions | E-ink ghosting — transitions leave visual artifacts; also violates mindful ethos | Instant state updates only. Selection highlight flips on/off. No slide, fade, or bounce. |
| Real-time error highlighting (auto-check) | Removes cognitive engagement; turns Sudoku into a find-the-red-cell puzzle | Silent tracking. Surface errors only at game completion. |
| Timer / time-based scoring | Creates anxiety; contradicts the Mudita device philosophy of reducing screen stress | Error-based scoring only. No visible timer during play. |
| Daily challenges / streaks | Streak mechanics create obligation anxiety ("I must play today"). Against mindful ethos. | Unlimited on-demand puzzle generation per difficulty. Play when you want. |
| Global leaderboards / online ranking | Requires network, Google Services (unavailable on MuditaOS K), and introduces social pressure | Local per-difficulty high scores only. |
| Multiplayer | Network dependency; not aligned with single-player offline scope | N/A — out of scope by design. |
| Achievements / badges / XP | Gamification layer adds visual noise and extrinsic motivation loops. Contrary to mindful design. | The score and personal best IS the reward. Clean and sufficient. |
| Color themes / custom skins | E-ink is monochromatic. Theming is irrelevant and adds maintenance overhead. | Single high-contrast MMD theme. |
| Ad monetization | Violates the distraction-free premise; also likely incompatible with de-Googled AOSP | No ads. Paid or free-without-ads distribution model. |
| AI-powered coaching / hint explanations | Very high complexity; strategy explanation engine is non-trivial to build correctly | Simple cell-reveal hint for v1. Strategy hints are a v2+ consideration. |
| Auto-fill pencil marks | Reduces cognitive challenge; makes Medium/Hard effectively easier by default | Manual pencil marks only. Player applies their own candidates. |
| Cloud save / cross-device sync | No backend, no Google Services, local-only scope | DataStore or SharedPreferences for local persistence. |
| Custom puzzle input (import/creation) | Scope creep for v1; high UX complexity | Generated puzzles only. |
| Sound effects / haptics | E-ink audience values quiet focus; haptic feedback can drain battery | Silent play. Visual-only feedback. |

---

## Feature Dependencies

```
Puzzle generation (unique, solvable)
  └── Difficulty classification (technique-based)
        └── Three difficulty levels (Easy / Medium / Hard)

Cell selection
  └── Number entry
        └── Pencil mark entry (toggle mode)
              └── Silent error tracking
                    └── Completion detection
                          └── Completion summary (errors, hints, score)
                                └── Per-difficulty high score table

Pause state persistence
  └── Resume on relaunch prompt
        └── (depends on: cell state + pencil marks + error count + hint count)

Hint system
  └── Score penalty on hint use
        └── Hint count tracked
              └── (feeds into: completion summary + score calculation)
```

---

## MVP Recommendation

Prioritize (in order):

1. **Puzzle generation** — valid, unique, difficulty-classified. Without this, nothing else matters.
2. **Cell selection + number entry** — core interaction loop. Must feel snappy on E-ink.
3. **Silent error tracking + completion detection** — the game loop can close.
4. **Completion summary + error-based score** — satisfying feedback moment.
5. **Pencil marks** — required for Medium/Hard puzzles to be solvable.
6. **Undo** — essential safety net, especially on touch where misfires happen.
7. **Pause / resume persistence** — E-ink users need this; device goes idle frequently.
8. **Hint with score penalty** — single-cell reveal. Simple to implement, meaningful for flow.
9. **Per-difficulty high score table** — local storage, three entries. Low effort, high perceived value.

Defer:

- **Strategy-revealing hints** — v2 feature. Cell-reveal is sufficient for v1.
- **Advanced difficulty tuning beyond three tiers** — three levels cover the user need.
- **Statistics tracking across sessions** — nice to have but not required for a satisfying first run.

---

## Phase-Specific Notes

| Area | Flag | Detail |
|------|------|--------|
| Puzzle generation | Needs deeper research | Difficulty classification by required solving technique (not just givens count) is non-trivial. Backtracking generator is well-understood; technique classifier needs algorithm research. |
| E-ink input UX | Needs validation | Touch target sizing and selection state on 800x480 E-ink needs hands-on testing. What works on OLED may feel laggy or imprecise here. |
| Game state persistence | Standard patterns | DataStore or SharedPreferences are well-understood. Low research risk. |
| Scoring formula | Design decision, not research | How to convert error count + hint count to a final score is a product decision, not a technical unknown. |
| Hint implementation | Low research risk for v1 | Cell-reveal is straightforward: pick an unfilled cell, populate its correct value. No algorithm complexity. |

---

## Sources

- [Top 10 Best Sudoku Apps 2025 - sudokugames.org](https://www.sudokugames.org/blog/top-10-best-sudoku-apps-2025) — feature landscape survey (MEDIUM confidence — WebSearch)
- [16 Best Sudoku Apps - sudokutimes.com](https://sudokutimes.com/best-sudoku-apps/) — feature comparison across popular apps (MEDIUM confidence — WebSearch)
- [Sudoku Puzzle Difficulty Levels Explained - sudokugames.org](https://www.sudokugames.org/blog/sudoku-puzzle-difficulty-levels) — difficulty classification and givens guidance (MEDIUM confidence — WebSearch)
- [Sudoku Puzzles Generating: from Easy to Evil - zhangroup.aporc.org](https://zhangroup.aporc.org/images/files/Paper_3485.pdf) — difficulty classification by required techniques (HIGH confidence — academic paper)
- [Good Sudoku press kit - playgoodsudoku.com](https://www.playgoodsudoku.com/presskit/) — design philosophy: eliminating busywork, teaching hints (MEDIUM confidence — WebSearch)
- [Good Sudoku - Tools and Toys review](https://toolsandtoys.net/good-sudoku-for-ios-by-zach-gage-and-jack-schlesinger/) — mindful, no-timer design analysis (MEDIUM confidence — WebSearch)
- [Mudita Mindful Design developer page - mudita.com](https://mudita.com/developers/) — E-ink design framework overview (MEDIUM confidence — WebSearch summary)
- [E-Ink ghosting and refresh challenges - Core Electronics Forum](https://forum.core-electronics.com.au/t/e-ink-display-integration-ghosting-and-refresh-challenges/23151) — E-ink animation/refresh constraints (MEDIUM confidence — WebSearch)
- [Pencil marks / candidates explanation - sudokuconquest.com](https://www.sudokuconquest.com/blog/sudoku-basics-candidates-pencil-marking) — pencil mark mechanics (HIGH confidence — domain-specific source)
- [DKM Sudoku Scoring System](https://dkmgames.com/Sudoku/SudokuPoints.htm) — scoring system patterns (MEDIUM confidence — WebSearch)
