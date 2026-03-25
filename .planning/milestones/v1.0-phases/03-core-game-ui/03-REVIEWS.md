---
phase: 03
reviewers: [codex]
reviewed_at: 2026-03-25T00:00:00Z
plans_reviewed: [03-01-PLAN.md, 03-02-PLAN.md, 03-03-PLAN.md, 03-04-PLAN.md]
---

# Cross-AI Plan Review — Phase 03

## Codex Review

## Plan 03-01: MMD Dependency + MainActivity + `eraseCell()`

**Summary**
This is a reasonable Wave 1 foundation, but it mixes two unrelated concerns: platform/UI bootstrapping and game-state mutation logic. The dependency and `MainActivity` work are necessary to unblock Phase 3, and `eraseCell()` is a useful gameplay primitive, but the plan does not fully prove MMD compliance or E-ink safety by itself. It is a good start, though slightly under-specified around app-wide theming scope and dependency resolution.

**Strengths**
- Establishes the MMD dependency early, which is a hard prerequisite for UI-01.
- Moves `MainActivity` to `ComponentActivity` with `ThemeMMD`, aligning with decision D-10.
- Adds a gameplay operation (`eraseCell()`) with explicit tests rather than leaving it implicit in UI wiring.
- Includes undo restoration in test scope, which is important for game-state correctness.

**Concerns**
- **MEDIUM:** `ThemeMMD + GameScreen stub` in `MainActivity` does not, by itself, guarantee "only MMD components" app-wide; it just establishes the root wrapper.
- **MEDIUM:** Adding Compose UI test dependencies here is slightly leaky scope for a plan otherwise focused on dependency/bootstrap and ViewModel logic.
- **MEDIUM:** `eraseCell()` test list is missing interaction with notes vs fill mode if those states have different undo semantics.
- **LOW:** No explicit mention of verifying that `MainActivity` no longer imports or uses Material theme/setup remnants.
- **LOW:** This plan assumes MMD dependency resolution is already functional, which Plan 03-04 later reveals may not be true on a clean machine.

**Suggestions**
- Split success checks into two explicit parts: "build resolves MMD on clean environment" and "root content wrapped in `ThemeMMD`".
- Add one `eraseCell()` test for mode-specific behavior if fill and pencil clearing affect history differently.
- Add a lightweight audit task to remove any lingering Material theme/provider usage from `MainActivity` or app root.
- If Plan 03-04 is accepted, make 03-01 depend on it or fold repo configuration into this wave.

**Risk Assessment**
**MEDIUM**. Good foundational plan, but it does not independently secure Phase 3 goals and may fail on fresh environments if repository setup is missing.

---

## Plan 03-02: GameScreen, GameGrid, NumberPad, ControlsRow

**Summary**
This is the core of the phase and broadly matches the target UX, layout, and interaction model. It addresses most visible requirements and user decisions directly, especially the layout, selection treatment, controls, and number row. The main risk is that it blends rendering, layout, and interaction into a fairly ambitious UI implementation without enough explicit guardrails around MMD-only compliance, E-ink redraw behavior, and touch sizing on a constrained 800×480 display.

**Strengths**
- Directly implements the key phase deliverable: complete playable game UI.
- The layout matches D-07 closely and respects the device form factor.
- Uses a Canvas-based grid, which is a sensible way to control line weights, cell fills, and pencil-mark rendering precisely.
- Captures the important visual decisions: strong selection contrast, given-vs-player distinction, thick/thin border hierarchy.
- Calls out a non-animated loading state, which is aligned with E-ink constraints.
- Includes dedicated Erase and Fill/Pencil controls matching D-06 and D-08.

**Concerns**
- **HIGH:** "Canvas-based 9×9 grid" may conflict with success criterion 1 if cells are rendered via custom drawing plus pointer input rather than MMD components. A strict reading of "uses only MMD components" makes this a compliance risk.
- **HIGH:** The plan does not explain how 81 cells will each achieve verified 56dp touch targets on an 800×480 screen. Physically, a full 9x9 board cannot expose 56dp-sized individual cells on this device.
- **HIGH:** D-05 says digit buttons should be in a single horizontal row of 9 plus dedicated Erase, but fitting 10 controls at usable width on 480px may be tight or impossible unless horizontal scrolling, weight compression, or alternative sizing is explicitly addressed.
- **MEDIUM:** The error-cell treatment is chosen arbitrarily ("1dp inset border") without validating whether this is sufficiently visible on monochrome E-ink.
- **MEDIUM:** Canvas drawing can reduce composable count, but if the whole board redraws on every tap it may worsen perceived ghosting or flashing on E-ink hardware.
- **MEDIUM:** `GameScreen` handles "Completed event" but the plan does not specify event consumption, one-shot state handling, or recomposition-safe navigation/dialog behavior.
- **MEDIUM:** "Centered `TextMMD` loading state" may be impossible if MMD does not provide a text primitive with the needed behavior; the plan assumes component availability.
- **LOW:** No explicit accessibility/semantics plan for grid cells, which matters for testability and selected-state assertions later.

**Suggestions**
- Resolve the core contradiction first: if 56dp applies to all interactive elements, the board design needs reinterpretation, zoom, or a documented exception for grid cells.
- Clarify what "MMD-only" means for Canvas. If custom drawing is allowed inside an MMD-themed screen, document that explicitly; otherwise this plan likely violates the phase goal.
- Add a requirement to minimize redraw scope: only invalidate/redraw affected cells or affected layers where possible.
- Specify one-shot event handling for completion to avoid duplicate dialogs/navigation on recomposition.
- Validate number-pad fit with actual dp math for 480px width before implementation.
- Add a quick hardware-check task for error-cell visibility and ghosting after repeated taps.

**Risk Assessment**
**HIGH**. This plan aims at the right screen, but it contains at least one likely requirement conflict (MMD-only vs Canvas) and one probable physical constraint issue (56dp targets for 81 cells, plus 10-button row on 480px width).

---

## Plan 03-03: Robolectric Compose UI Tests

**Summary**
This plan improves confidence, but it is overconfident about what Robolectric can prove for an E-ink-specific UI. It covers some useful structural checks, especially semantics and touch-target assertions, yet it cannot validate ghosting, hardware tap reliability, or actual absence of display artifacts. It is a helpful support plan, but not sufficient evidence for several phase success criteria.

**Strengths**
- Adds UI-level verification rather than relying only on ViewModel tests.
- Selected-state semantics and mode-toggle state are appropriate to assert in Compose tests.
- Touch-target checks are directly tied to UI-03.
- The explicit attempt to detect animation/ripple usage shows good awareness of E-ink constraints.

**Concerns**
- **HIGH:** "No `Animated*` composables, no `rememberRipple/indication`" is not a robust or complete way to prove no animations/transitions/ripples anywhere in the app. It will miss many cases.
- **HIGH:** Robolectric cannot validate physical-device ghosting, tap reliability, or display artifact behavior, so it does not cover success criteria 2, 3, and 4 fully.
- **MEDIUM:** `boundsInRoot.height ≥ 56.dp.toPx(density)` may not be stable or meaningful unless density and layout conditions are carefully controlled.
- **MEDIUM:** Visual distinction between given and player cells is not straightforward to assert in standard Compose tests if the grid is Canvas-based.
- **MEDIUM:** If the grid is drawn on Canvas, per-cell semantics may need custom semantics injection; otherwise `GameGridTest` may be difficult or brittle.
- **LOW:** The plan assumes existing test infrastructure can support `createComposeRule()` with Robolectric in this project without additional setup issues.

**Suggestions**
- Reframe these as "structural UI tests" rather than claiming they validate E-ink compliance.
- Add explicit physical-device manual verification steps for ghosting, single-tap reliability, and no-ripple/no-animation observation.
- Replace brittle implementation-detail checks (`Animated*`, `rememberRipple`) with policy-based checks where possible, plus code review gates for prohibited APIs.
- If Canvas remains, add explicit semantics mapping for each cell and selection state so tests can remain meaningful.
- Add tests for one-shot completion handling and erase/undo UI wiring, since those are user-visible behaviors.

**Risk Assessment**
**MEDIUM**. Useful as a regression net, but insufficient for the hardest phase requirements and somewhat brittle if it relies on implementation details.

---

## Plan 03-04: Gap Closure — GitHub Packages Repository for MMD

**Summary**
This is a strong and necessary gap-closure plan. If MMD cannot resolve on a fresh clone, the earlier plans are not truly reproducible. The conditional repository setup is directionally correct and the credential-gating approach is safer than hardcoding secrets, but the details matter: repository scoping, credential handling, and failure mode should be tightly defined to avoid accidental leakage or confusing builds.

**Strengths**
- Addresses a real blocker to reproducibility rather than assuming local cache state.
- Puts repository configuration in `settings.gradle.kts`, which is the correct level for centralized dependency resolution.
- Uses Gradle property-based credentials instead of embedding secrets in source.
- Includes a human checkpoint for developer setup, which is appropriate for private package access.

**Concerns**
- **HIGH:** "Graceful no-op if absent" may cause confusing dependency resolution behavior later if the repository is silently skipped but the build still requires MMD.
- **MEDIUM:** If `maven.pkg.github.com/mudita/MMD` is added broadly without content filtering, Gradle may query GitHub Packages for unrelated dependencies, slowing builds and increasing credential exposure surface.
- **MEDIUM:** Using only `githubToken` may be insufficient depending on GitHub Packages auth expectations; some setups also need a username or actor.
- **MEDIUM:** Verifying only `:app:compileDebugKotlin` is decent, but a truly fresh-clone test should ensure no relevant artifacts are already cached.
- **LOW:** The plan does not state whether documentation/README/CONTEXT should be updated so other developers know how to configure credentials.

**Suggestions**
- Do not silently no-op unless the build can still succeed without MMD. If MMD is mandatory, fail fast with a clear message when credentials are missing.
- Scope the repository with `content { includeGroup(...) }` or equivalent so only MMD artifacts resolve from GitHub Packages.
- Confirm the exact credential scheme required by Mudita's package registry and document it precisely.
- Add a developer-facing setup note in project docs or `README`.
- If possible, test in a clean Gradle user home or clearly document that the verification was intended to simulate a fresh environment.

**Risk Assessment**
**MEDIUM**. Necessary and mostly well-conceived, but credential handling and repository scoping need to be tightened to avoid fragile or confusing builds.

---

## Consensus Summary

*Note: Only Codex reviewed (Gemini unavailable, Claude skipped for independence as current runtime).*

### Agreed Strengths
- Clear wave ordering with sensible dependency flow (01→02→03→04 gap)
- Strong attention to E-ink constraints in principle: no animation, no ripple, high contrast
- ViewModel logic separated from UI work
- Test plan exists rather than leaving verification informal
- Reproducibility gap (MMD resolution) identified and turned into a concrete follow-up plan (03-04)

### Top Concerns

| Severity | Concern | Plans Affected |
|----------|---------|----------------|
| HIGH | Canvas grid vs "MMD-only components" — requirement interpretation needs explicit resolution | 03-02, 03-03 |
| HIGH | 56dp touch targets for 81 grid cells physically impossible on 800×480 display — needs documented exception or reinterpretation | 03-02, 03-03 |
| HIGH | Robolectric cannot validate ghosting, physical tap reliability, or runtime E-ink artifacts | 03-03 |
| HIGH | Credential "graceful no-op" in 03-04 may cause confusing silent failures | 03-04 |
| MEDIUM | Canvas whole-board redraw on every tap may worsen E-ink ghosting | 03-02 |
| MEDIUM | Completion event one-shot handling not specified (recomposition-safe navigation) | 03-02 |
| MEDIUM | GitHub Packages repository should be scoped with `content { includeGroup() }` | 03-04 |

### Divergent Views
None (single reviewer).

### Recommended Actions Before Execute
1. **Document Canvas exception** — add explicit note in CONTEXT.md or plan: "Canvas drawing inside ThemeMMD-wrapped Composable satisfies UI-01; grid cells are not ButtonMMD/TextMMD but are rendered inside ThemeMMD scope."
2. **Clarify 56dp scope** — UI-03 applies to interactive controls (digit buttons, mode toggle, undo); grid cell tap targets are sized by device physics, not 56dp. Add this scoping note to CONTEXT.md.
3. **Tighten 03-04 credential gating** — add `content { includeGroup("com.mudita") }` to the GitHub Packages block; fail with clear error rather than silent no-op if token absent and MMD not cached.
4. **Add physical-device checklist** — ghosting, tap reliability, and no-animation observation cannot be automated; add a manual test checklist as a phase deliverable.
