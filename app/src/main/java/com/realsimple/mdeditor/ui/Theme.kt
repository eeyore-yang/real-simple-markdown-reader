package com.realsimple.mdeditor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neutral, paper-like colors: the text is the point, not the chrome.
private val Light = lightColorScheme(
    primary = Color(0xFF2F6BD8),
    onPrimary = Color.White,
    background = Color(0xFFFBFBFA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFBFBFA),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFEFEFEC),
    onSurfaceVariant = Color(0xFF5F6368),
    surfaceContainer = Color(0xFFF3F3F1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B1F44),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA8ABB0),
    surfaceContainer = Color(0xFF222222),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
