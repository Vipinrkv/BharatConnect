package com.bharatconnect.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorPrimary6367FF = Color(0xFF6367FF)
val ColorAccent8494FF = Color(0xFF8494FF)
val ColorLavenderC9BEFF = Color(0xFFC9BEFF)
val ColorHighlightFFDBFD = Color(0xFFFFDBFD)
val ColorRoyal2F2FE4 = Color(0xFF2F2FE4)
val ColorMidnight162E93 = Color(0xFF162E93)
val ColorCard1A1953 = Color(0xFF1A1953)
val ColorBackground080616 = Color(0xFF080616)

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary6367FF,
    secondary = ColorAccent8494FF,
    tertiary = ColorLavenderC9BEFF,
    background = ColorBackground080616,
    surface = ColorCard1A1953,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BharatConnectTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
