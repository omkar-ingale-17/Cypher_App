package com.cypher.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cypher is dark-only — a light theme is intentionally not provided in Stage 1.
// If light mode support is needed later, a lightColorScheme() block can be added.
private val CypherDarkColorScheme = darkColorScheme(
    primary            = CypherCyan,
    onPrimary          = CypherBackground,
    primaryContainer   = CypherCyanContainer,
    onPrimaryContainer = CypherCyan,

    secondary          = CypherViolet,
    onSecondary        = Color.White,
    secondaryContainer = CypherVioletContainer,
    onSecondaryContainer = CypherViolet,

    background         = CypherBackground,
    onBackground       = CypherOnBackground,

    surface            = CypherSurface,
    onSurface          = CypherOnSurface,
    surfaceVariant     = CypherSurfaceVariant,
    onSurfaceVariant   = CypherOnSurfaceMuted,

    outline            = CypherOutline,
    error              = CypherError,
    onError            = Color.White,

    inverseSurface     = CypherOnBackground,
    inverseOnSurface   = CypherBackground,
    inversePrimary     = CypherCyanDim
)

/**
 * Cypher Material 3 theme wrapper.
 * Always uses the dark color scheme regardless of system setting,
 * matching Cypher's deep-space visual identity.
 */
@Composable
fun CypherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CypherDarkColorScheme,
        typography  = CypherTypography,
        content     = content
    )
}
