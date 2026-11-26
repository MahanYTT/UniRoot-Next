package com.uniroot.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Immutable
data class UniSemanticColors(
    val isDark: Boolean,
    val cardBorder: Color,
    val trackSurface: Color,
    val trackThumb: Color,
    val trackThumbBorder: Color,
    val textTertiary: Color,
    // Semantic Emerald (Rooted / Compatible / Up to date)
    val emerald: Color,
    val emeraldContainer: Color,
    val emeraldWashEnd: Color,
    val emeraldBorder: Color,
    val onEmeraldContainer: Color,
    // Semantic Indigo (Primary / Ready / Update available)
    val indigo: Color,
    val indigoContainer: Color,
    val indigoWashEnd: Color,
    val indigoBorder: Color,
    val onIndigoContainer: Color,
    // Semantic Amber (Caution / Shizuku / Manual / Knox 0x1)
    val amber: Color,
    val amberContainer: Color,
    val amberWashEnd: Color,
    val amberBorder: Color,
    val onAmberContainer: Color,
    // Semantic Rose (Unsupported / Error / Destructive)
    val rose: Color,
    val roseContainer: Color,
    val roseWashEnd: Color,
    val roseBorder: Color,
    val onRoseContainer: Color,
    // Semantic Sky (Active running / progress)
    val sky: Color,
    val skyContainer: Color,
    val skyBorder: Color,
    val onSkyContainer: Color,
    // Terminal Console (always high-contrast obsidian)
    val consoleBg: Color,
    val consoleBorder: Color,
    val consoleDefault: Color,
    val consoleSuccess: Color,
    val consoleWarning: Color,
    val consoleError: Color,
    val consoleProgress: Color,
)

private val LightSemanticColors = UniSemanticColors(
    isDark = false,
    cardBorder = Color(0xFFE2E8F0),
    trackSurface = Color(0xFFE9EEF5),
    trackThumb = Color(0xFFFFFFFF),
    trackThumbBorder = Color(0xFFDCE3EE),
    textTertiary = Color(0xFF7B8798),
    emerald = Color(0xFF059669),
    emeraldContainer = Color(0xFFECFDF5),
    emeraldWashEnd = Color(0xFFF6FEF9),
    emeraldBorder = Color(0xFFA7F3D0),
    onEmeraldContainer = Color(0xFF065F46),
    indigo = Color(0xFF4F46E5),
    indigoContainer = Color(0xFFEEF2FF),
    indigoWashEnd = Color(0xFFF7F9FF),
    indigoBorder = Color(0xFFC7D2FE),
    onIndigoContainer = Color(0xFF3730A3),
    amber = Color(0xFFD97706),
    amberContainer = Color(0xFFFFFBEB),
    amberWashEnd = Color(0xFFFFFDF5),
    amberBorder = Color(0xFFFDE68A),
    onAmberContainer = Color(0xFF92400E),
    rose = Color(0xFFE11D48),
    roseContainer = Color(0xFFFFF1F2),
    roseWashEnd = Color(0xFFFFF8F8),
    roseBorder = Color(0xFFFECDD3),
    onRoseContainer = Color(0xFF9F1239),
    sky = Color(0xFF0284C7),
    skyContainer = Color(0xFFF0F9FF),
    skyBorder = Color(0xFFBAE6FD),
    onSkyContainer = Color(0xFF075985),
    consoleBg = Color(0xFF090D14),
    consoleBorder = Color(0xFF1E293B),
    consoleDefault = Color(0xFFCBD5E1),
    consoleSuccess = Color(0xFF4ADE80),
    consoleWarning = Color(0xFFFBBF24),
    consoleError = Color(0xFFFB7185),
    consoleProgress = Color(0xFF38BDF8),
)

private val DarkSemanticColors = UniSemanticColors(
    isDark = true,
    cardBorder = Color(0xFF222B3B),
    trackSurface = Color(0xFF0F141D),
    trackThumb = Color(0xFF222B3D),
    trackThumbBorder = Color(0xFF33415C),
    textTertiary = Color(0xFF64748B),
    emerald = Color(0xFF34D399),
    emeraldContainer = Color(0xFF0D261C),
    emeraldWashEnd = Color(0xFF111B19),
    emeraldBorder = Color(0x4D22C55E),
    onEmeraldContainer = Color(0xFF6EE7B7),
    indigo = Color(0xFF818CF8),
    indigoContainer = Color(0xFF161B38),
    indigoWashEnd = Color(0xFF131826),
    indigoBorder = Color(0x526366F1),
    onIndigoContainer = Color(0xFFC7D2FE),
    amber = Color(0xFFFBBF24),
    amberContainer = Color(0xFF291F0B),
    amberWashEnd = Color(0xFF1B1712),
    amberBorder = Color(0x52F59E0B),
    onAmberContainer = Color(0xFFFDE68A),
    rose = Color(0xFFFB7185),
    roseContainer = Color(0xFF2B1218),
    roseWashEnd = Color(0xFF1D1318),
    roseBorder = Color(0x59F43F5E),
    onRoseContainer = Color(0xFFFDA4AF),
    sky = Color(0xFF38BDF8),
    skyContainer = Color(0xFF0E2336),
    skyBorder = Color(0x5238BDF8),
    onSkyContainer = Color(0xFF7DD3FC),
    consoleBg = Color(0xFF090D14),
    consoleBorder = Color(0xFF1E293B),
    consoleDefault = Color(0xFFCBD5E1),
    consoleSuccess = Color(0xFF4ADE80),
    consoleWarning = Color(0xFFFBBF24),
    consoleError = Color(0xFFFB7185),
    consoleProgress = Color(0xFF38BDF8),
)

val LocalUniColors = staticCompositionLocalOf { DarkSemanticColors }

private fun uniLightMiuixColors() = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF4338CA),
    onPrimaryVariant = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF3730A3),
    disabledPrimaryButton = Color(0xFFCBD5E1),
    disabledOnPrimaryButton = Color(0xFF64748B),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF0F172A),
    onBackgroundVariant = Color(0xFF475569),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceSecondary = Color(0xFF475569),
    onSurfaceVariantSummary = Color(0xFF64748B),
    onSurfaceVariantActions = Color(0xFF475569),
    surfaceContainer = Color(0xFFFFFFFF),
    onSurfaceContainer = Color(0xFF0F172A),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    onSurfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    dividerLine = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFF1F2),
    onErrorContainer = Color(0xFF9F1239),
)

private fun uniDarkMiuixColors() = darkColorScheme(
    primary = Color(0xFF5B5BF0),
    onPrimary = Color(0xFFFFFFFF),
    primaryVariant = Color(0xFF4F46E5),
    onPrimaryVariant = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1C2142),
    onPrimaryContainer = Color(0xFFA5B4FC),
    disabledPrimaryButton = Color(0xFF1E2636),
    disabledOnPrimaryButton = Color(0xFF64748B),
    background = Color(0xFF0A0D12),
    onBackground = Color(0xFFF1F5F9),
    onBackgroundVariant = Color(0xFF94A3B8),
    surface = Color(0xFF131822),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceSecondary = Color(0xFF94A3B8),
    onSurfaceVariantSummary = Color(0xFF7D8CA3),
    onSurfaceVariantActions = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF131822),
    onSurfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFF1A2130),
    onSurfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFF242E42),
    outline = Color(0xFF2B364B),
    dividerLine = Color(0xFF1F293B),
    error = Color(0xFFFB7185),
    onError = Color(0xFF2B0810),
    errorContainer = Color(0xFF2B1218),
    onErrorContainer = Color(0xFFFDA4AF),
)

@Composable
internal fun UniRootTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val miuixColors = if (darkTheme) uniDarkMiuixColors() else uniLightMiuixColors()
    CompositionLocalProvider(LocalUniColors provides semantic) {
        MiuixTheme(colors = miuixColors, content = content)
    }
}
