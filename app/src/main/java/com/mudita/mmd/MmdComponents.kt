package com.mudita.mmd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Local stub implementations of MMD (Mudita Mindful Design) library components.
 *
 * These stubs exist because the MMD library is not resolvable from this environment
 * (GitHub Packages requires auth, JFrog instance deactivated). They provide the minimum
 * API surface needed to compile and test the production source files.
 *
 * In production (with MMD AAR available):
 * - Remove this file
 * - Switch compileOnly(libs.mmd) back to implementation(libs.mmd)
 * - The real MMD implementations provide E-ink optimizations (eInkColorScheme, ripple-free
 *   button behavior, E-ink typography)
 *
 * These stubs are NOT E-ink optimized — they are compile/test scaffolding only.
 */

/**
 * Stub ThemeMMD — applies MaterialTheme.
 * Real ThemeMMD provides eInkColorScheme (monochromatic) + E-ink typography.
 */
@Composable
fun ThemeMMD(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

/**
 * Stub ButtonMMD — renders as a clickable Box.
 * Real ButtonMMD is a ripple-free, E-ink optimized button component.
 */
@Composable
fun ButtonMMD(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        content()
    }
}

/**
 * Stub TextMMD — renders as a standard Material3 Text.
 * Real TextMMD uses E-ink optimized typography (tuned font sizes, monochromatic colors).
 */
@Composable
fun TextMMD(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        color = color
    )
}
