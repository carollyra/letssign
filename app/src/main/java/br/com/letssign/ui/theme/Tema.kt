package br.com.letssign.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoresClaras = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F7),
    onPrimaryContainer = Color(0xFF0A1D30),
    secondary = Color(0xFF52606F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4F5),
    onSecondaryContainer = Color(0xFF0F1D2A),
    tertiary = Color(0xFF1B6B4F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFEBD5),
    onTertiaryContainer = Color(0xFF002115),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
)

private val CoresEscuras = darkColorScheme(
    primary = Color(0xFFA3C9EE),
    onPrimary = Color(0xFF03324F),
    primaryContainer = Color(0xFF214A6C),
    onPrimaryContainer = Color(0xFFD3E4F7),
    secondary = Color(0xFFBAC8DA),
    onSecondary = Color(0xFF243240),
    tertiary = Color(0xFF8ED5B5),
    onTertiary = Color(0xFF003827),
    tertiaryContainer = Color(0xFF00513A),
    onTertiaryContainer = Color(0xFFBFEBD5),
)

@Composable
fun LetsSignTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) CoresEscuras else CoresClaras,
        content = content,
    )
}
