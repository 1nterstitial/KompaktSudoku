# Mudita Kompakt Sudoku

A Sudoku game built specifically for the **Mudita Kompakt** — a minimalist E-ink Android phone designed for mindful, distraction-free use.

## About

This app delivers a fully playable Sudoku experience that feels native on the Kompakt's 4.3" E-ink touchscreen. Every design decision — from the high-contrast grid to the silent error tracking — is tuned for the device's monochromatic display, deliberate touch interaction model, and the Mudita philosophy of calm, focused computing.

Built with Kotlin, Jetpack Compose, and the [Mudita Mindful Design (MMD)](https://github.com/mudita/MMD) library.

## Features

- **Three difficulty levels** — Easy, Medium, and Hard, each with calibrated puzzle complexity
- **Touch-native input** — tap a cell to select, tap a digit to fill; all touch targets ≥56dp
- **Pencil marks** — toggle between fill and pencil-mark mode; 2×2 annotation grid per cell
- **Undo** — step back through fills and pencil marks
- **Hints** — reveal one correct cell at a score penalty
- **Scoring** — error-based; errors tracked silently and revealed only at completion
- **High scores** — per-difficulty personal bests stored locally
- **Pause and resume** — full game state persisted on exit; resume prompt on next launch
- **Exit confirmation** — back press shows Save/Forfeit dialog; no accidental progress loss

## E-ink Optimizations

- No animations, transitions, or ripple effects (prevents E-ink ghosting)
- Monochromatic palette via MMD's `eInkColorScheme`
- High-contrast grid with bold cell borders
- Large, legible digit font with Roboto Condensed on number pad
- Pencil marks scale to fit 4 digits in a 2×2 grid within each cell

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose (BOM 2026.03.00) + MMD 1.0.1 |
| Architecture | MVVM + StateFlow |
| Puzzle generation | [Sudoklify](https://github.com/teogor/sudoklify) 1.0.0-beta04 |
| Persistence | DataStore Preferences 1.2.1 |
| Serialization | kotlinx.serialization 1.8.0 |

## Requirements

- **Device:** Mudita Kompakt (MuditaOS K — de-Googled AOSP Android 12)
- **Min SDK:** API 31
- **Target SDK:** API 31

## Building

The MMD library is distributed via GitHub Packages. You need a GitHub personal access token with `read:packages` scope.

Add to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_TOKEN
```

Then build:

```bash
./gradlew assembleDebug
```

## License

MIT
