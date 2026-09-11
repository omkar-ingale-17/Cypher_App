package com.cypher.assistant.ui.theme

import androidx.compose.ui.graphics.Color

// -- Cypher Brand Colors ------------------------------------------------------
// Palette concept: Deep-space dark with electric cyan primary and neon-violet accent.

// Backgrounds
val CypherBackground      = Color(0xFF060B14)   // Near-black navy
val CypherSurface         = Color(0xFF0D1826)   // Dark blue-grey card surface
val CypherSurfaceVariant  = Color(0xFF152033)   // Slightly lighter card variant
val CypherOutline         = Color(0xFF1E3050)   // Subtle border / divider

// Primary — Electric Cyan
val CypherCyan            = Color(0xFF00E5FF)   // Primary action / active glow
val CypherCyanDim         = Color(0xFF009AB8)   // Pressed / disabled variant
val CypherCyanContainer   = Color(0xFF00313F)   // Container background

// Secondary — Neon Violet / Purple
val CypherViolet          = Color(0xFF9C40FF)   // Secondary accent
val CypherVioletDim       = Color(0xFF6B2BAE)
val CypherVioletContainer = Color(0xFF2A0F52)
val CypherPurple          = CypherViolet

// Semantic
val CypherSuccess         = Color(0xFF00E676)   // Green — command success
val CypherGreen           = CypherSuccess
val CypherError           = Color(0xFFFF4655)   // Red — command failure / error
val CypherRed             = CypherError
val CypherWarning         = Color(0xFFFFD740)   // Amber — confirmation required

// Text
val CypherOnBackground    = Color(0xFFE8EDF4)   // Primary text on dark background
val CypherOnSurface       = Color(0xFFBEC8D9)   // Secondary text
val CypherOnSurfaceMuted  = Color(0xFF607080)   // Muted / hint text

// Glow colors for animations
val CypherGlowCyan        = Color(0x5500E5FF)   // 33% alpha cyan glow
val CypherGlowViolet      = Color(0x559C40FF)   // 33% alpha violet glow
val CypherRippleCyan      = Color(0x2200E5FF)   // 13% alpha for pulse rings
