package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF0D5C53),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF003549),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = InfoBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    background = SleekBackground,
    onBackground = SleekOnSurface,
    surface = SleekSurface,
    onSurface = SleekOnSurface,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant,
    outline = SleekOutline,
    outlineVariant = Color(0xFFCBD5E1),
    error = ExpenseRed,
    onError = Color.White
)

enum class AppThemeMode(val displayName: String, val description: String) {
    LIGHT("Claro", "Diseño limpio y luminoso"),
    DARK("Oscuro", "Descanso visual con tonos pizarra y turquesa"),
    ELEGANT("Elegante", "Lujo obsidian con acentos dorados y esmeralda")
}

private val ElegantColorScheme = darkColorScheme(
    primary = Color(0xFFE5B842), // Rich Champagne Gold
    onPrimary = Color(0xFF1E1702),
    primaryContainer = Color(0xFF3D2F0A),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFF10B981), // Emerald luxury
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF0B101E), // Deep luxury midnight obsidian
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF141D33), // Obsidian navy surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1D2845),
    onSurfaceVariant = Color(0xFFA0AEC0),
    outline = Color(0xFF4A5568),
    outlineVariant = Color(0xFF2D3748),
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun FinanzasClaraTheme(
    themeMode: AppThemeMode? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val effectiveMode = themeMode ?: if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT
    val colorScheme = when (effectiveMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.ELEGANT -> ElegantColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
