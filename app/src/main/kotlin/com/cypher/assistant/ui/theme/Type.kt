package com.cypher.assistant.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Using system default sans-serif (maps to Roboto on most Android devices).
// To use a custom font: add the .ttf files under res/font/ and reference them here.
val CypherFontFamily = FontFamily.Default

val CypherTypography = Typography(
    // -- Display - used for the main "CYPHER" logo header -------------------
    displayLarge = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Bold,
        fontSize    = 48.sp,
        lineHeight  = 56.sp,
        letterSpacing = 6.sp
    ),

    // -- Headline - screen titles -------------------------------------------
    headlineLarge = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 28.sp,
        lineHeight  = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 22.sp,
        lineHeight  = 28.sp,
        letterSpacing = 0.sp
    ),

    // -- Body --------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 16.sp,
        lineHeight  = 24.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // -- Label -------------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        letterSpacing = 1.2.sp   // Uppercase tracking for tech aesthetic
    ),
    labelMedium = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
        letterSpacing = 1.0.sp
    ),
    labelSmall = TextStyle(
        fontFamily  = CypherFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 10.sp,
        lineHeight  = 14.sp,
        letterSpacing = 1.0.sp
    )
)
