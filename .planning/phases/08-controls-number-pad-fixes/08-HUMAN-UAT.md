---
status: partial
phase: 08-controls-number-pad-fixes
source: [08-VERIFICATION.md]
started: 2026-03-27T00:00:00Z
updated: 2026-03-27T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. CTRL-01 Condensed font
expected: Number pad digit buttons 1–9 render in a visibly taller/narrower font (Roboto Condensed / sans-serif-condensed) compared to the default Roboto; digits appear better vertically centered within their buttons
result: [pending]

### 2. CTRL-02 Two-line hint
expected: The "Get Hint" button displays "Get" on line 1 and "Hint" on line 2, both centered within the button
result: [pending]

### 3. CTRL-03 Perceptible mid-gray inactive background
expected: Whichever of Fill/Pencil is inactive shows a mid-gray (#E0E0E0) background clearly distinct from the active button's solid black background; on the E-ink display this gray is perceptible
result: [pending]

### 4. CTRL-04 Border frame around Fill+Pencil
expected: A thin rectangular black border visually encloses Fill and Pencil buttons together as a group; Undo and Get Hint buttons are outside this frame with no border overlap; corners and edges are clean with no pixel clipping
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
