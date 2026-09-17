package com.google.refereeschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    orgThemeColor: String = "Default",
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val colorScheme = remember(orgThemeColor, darkTheme) {
        if (orgThemeColor == "Default" || darkTheme) {
            baseColorScheme
        } else {
            val bgColor = when (orgThemeColor) {
                "Red" -> Color(0xFFD32F2F)
                "Blue" -> Color(0xFF1976D2)
                "Yellow" -> Color(0xFFFBC02D)
                "Brown" -> Color(0xFF795548)
                "Black" -> Color(0xFF000000)
                "Orange" -> Color(0xFFFB8C00)
                "Green" -> Color(0xFF4CAF50)
                else -> Color.White
            }
            
            val onBgColor = when (orgThemeColor) {
                "Yellow", "Orange" -> Color.Black
                else -> Color.White
            }

            baseColorScheme.copy(
                primary = if (orgThemeColor == "Yellow" || orgThemeColor == "Orange") Color.Black else Color.White,
                onPrimary = if (orgThemeColor == "Yellow" || orgThemeColor == "Orange") Color.White else Color.Black,
                background = bgColor,
                surface = bgColor,
                onBackground = onBgColor,
                onSurface = onBgColor,
                surfaceVariant = bgColor.copy(alpha = 0.8f),
                onSurfaceVariant = onBgColor,
                surfaceContainerLow = bgColor,
                surfaceContainer = bgColor,
                surfaceContainerHigh = bgColor,
                surfaceContainerHighest = bgColor,
                outline = onBgColor.copy(alpha = 0.5f),
                outlineVariant = onBgColor.copy(alpha = 0.2f)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
