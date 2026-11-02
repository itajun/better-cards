package com.betterklarna.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Predefined palette for loyalty cards
val CardPalette = listOf(
    "#E53935", // Red
    "#D81B60", // Magenta
    "#8E24AA", // Purple
    "#3949AB", // Indigo
    "#1E88E5", // Blue
    "#00897B", // Teal
    "#2E7D32", // Forest Green
    "#FB8C00", // Amber Orange
    "#E64A19", // Coral
    "#37474F", // Charcoal
    "#000000", // Pure Black
    "#FFFFFF"  // Pure White
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF1E88E5)): Color {
    return try {
        val cleanHex = hex.trim().removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun isLightCardColor(color: Color): Boolean {
    return color.luminance() > 0.62f
}

fun isVeryDarkCardColor(color: Color): Boolean {
    return color.luminance() < 0.05f
}

fun cardContentColor(cardBgColor: Color): Color {
    return if (isLightCardColor(cardBgColor)) {
        Color(0xFF111827)
    } else {
        Color.White
    }
}

fun cardPillBackgroundColor(cardBgColor: Color): Color {
    return if (isLightCardColor(cardBgColor)) {
        Color.Black.copy(alpha = 0.07f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }
}
