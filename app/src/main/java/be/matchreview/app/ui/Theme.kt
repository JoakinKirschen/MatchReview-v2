package be.matchreview.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** How the app looks; Outdoor is a high-contrast light scheme for bright sunlight. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    OUTDOOR("Outdoor");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF087F5B),
    secondary = Color(0xFF2F9E44),
    tertiary = Color(0xFF1971C2),
    background = Color(0xFFF4F7FA),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF102A43)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF63E6BE),
    secondary = Color(0xFF8CE99A),
    tertiary = Color(0xFF74C0FC),
    background = Color(0xFF0B1F33),
    surface = Color(0xFF102A43)
)

/** Pure white surfaces, black text and dark, saturated accents that stay readable in the sun. */
private val OutdoorColors = lightColorScheme(
    primary = Color(0xFF00492F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2C9),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF14532D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F0C8),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF0B3D91),
    tertiaryContainer = Color(0xFFC9DAFF),
    onTertiaryContainer = Color.Black,
    error = Color(0xFFB00020),
    errorContainer = Color(0xFFFFD6DB),
    onErrorContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    outlineVariant = Color(0xFF5C5C5C)
)

@Composable
fun MatchReviewTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colors = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.OUTDOOR -> OutdoorColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
