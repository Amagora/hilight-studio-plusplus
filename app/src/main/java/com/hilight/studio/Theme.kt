package com.hilight.studio

import android.app.Activity
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Pixel-flavoured Material 3 Theming Engine.
 *
 * Supports dynamic wallpaper-derived Material You color schemes, curated Material 3 palettes,
 * custom light/dark/system themes, and an AMOLED pitch-black mode tailored for Pixel foldable OLED screens.
 */

enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_mode_system),
    LIGHT(R.string.theme_mode_light),
    DARK(R.string.theme_mode_dark),
    AMOLED_BLACK(R.string.theme_mode_amoled),
    ;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class ThemePalette(@StringRes val labelRes: Int, val primaryAccent: Color) {
    DYNAMIC(R.string.palette_dynamic, Color(0xFF6750A4)),
    INDIGO(R.string.palette_indigo, Color(0xFF5B3FBF)),
    BLUE(R.string.palette_blue, Color(0xFF006399)),
    EMERALD(R.string.palette_emerald, Color(0xFF1E6C45)),
    CORAL(R.string.palette_coral, Color(0xFFBF4A30)),
    AMBER(R.string.palette_amber, Color(0xFF855400)),
    ROSE(R.string.palette_rose, Color(0xFF984062)),
    MONOCHROME(R.string.palette_monochrome, Color(0xFF49454F)),
    ;

    companion object {
        fun fromName(name: String?): ThemePalette = entries.firstOrNull { it.name == name } ?: DYNAMIC
    }
}

// ---------------- Indigo (Classic Pixel Indigo) ----------------

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFB69DFF),
    onPrimary = Color(0xFF2B1667),
    primaryContainer = Color(0xFF422C7F),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFF7FD8E8),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF004E5A),
    onSecondaryContainer = Color(0xFFB0EDFB),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF4C252B),
    tertiaryContainer = Color(0xFF653A41),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFF121116),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF121116),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceContainerLowest = Color(0xFF0D0C11),
    surfaceContainerLow = Color(0xFF1B1A1F),
    surfaceContainer = Color(0xFF1E1D22),
    surfaceContainerHigh = Color(0xFF29282D),
    surfaceContainerHighest = Color(0xFF343238),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454E),
)

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF5B3FBF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF006876),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0EDFB),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFF805158),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF321017),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F1FA),
    surfaceContainer = Color(0xFFF2ECF4),
    surfaceContainerHigh = Color(0xFFECE6EE),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4CF),
)

// ---------------- Blue (Ocean Blue) ----------------

private val BlueDark = darkColorScheme(
    primary = Color(0xFF94CCFF),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004A75),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFFB9C8DA),
    onSecondary = Color(0xFF233240),
    secondaryContainer = Color(0xFF3A4857),
    onSecondaryContainer = Color(0xFFD5E4F6),
    tertiary = Color(0xFFD2BFE7),
    onTertiary = Color(0xFF382A49),
    tertiaryContainer = Color(0xFF4F4061),
    onTertiaryContainer = Color(0xFFEEDCFF),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE0E3E7),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE0E3E7),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceContainerLowest = Color(0xFF0B0F12),
    surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
)

private val BlueLight = lightColorScheme(
    primary = Color(0xFF006399),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE5FF),
    onPrimaryContainer = Color(0xFF001D32),
    secondary = Color(0xFF51606F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E4F6),
    onSecondaryContainer = Color(0xFF0E1D2A),
    tertiary = Color(0xFF67587A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEDCFF),
    onTertiaryContainer = Color(0xFF221533),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F7),
    surfaceContainer = Color(0xFFECEFF2),
    surfaceContainerHigh = Color(0xFFE6E9EC),
    surfaceContainerHighest = Color(0xFFE0E3E7),
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
)

// ---------------- Emerald (Forest Green) ----------------

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF8BD8A5),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFFA6F5C0),
    secondary = Color(0xFFB7CCB8),
    onSecondary = Color(0xFF223526),
    secondaryContainer = Color(0xFF394B3C),
    onSecondaryContainer = Color(0xFFD3E8D4),
    tertiary = Color(0xFFA5CFE0),
    onTertiary = Color(0xFF073644),
    tertiaryContainer = Color(0xFF244D5B),
    onTertiaryContainer = Color(0xFFC0EBFD),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDCE3D9),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDCE3D9),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF171D18),
    surfaceContainer = Color(0xFF1B211C),
    surfaceContainerHigh = Color(0xFF252C26),
    surfaceContainerHighest = Color(0xFF303731),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF1E6C45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F5C0),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF506353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D4),
    onSecondaryContainer = Color(0xFF0E1F13),
    tertiary = Color(0xFF3D6574),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC0EBFD),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF6FBF3),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFF6FBF3),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5EB),
    surfaceContainer = Color(0xFFE8EFE5),
    surfaceContainerHigh = Color(0xFFE2E9DF),
    surfaceContainerHighest = Color(0xFFDCE3D9),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9BF),
)

// ---------------- Coral (Sunset Peach) ----------------

private val CoralDark = darkColorScheme(
    primary = Color(0xFFFFB4A2),
    onPrimary = Color(0xFF5E1705),
    primaryContainer = Color(0xFF812D19),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFE7BDB2),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D3F37),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFDBC58D),
    onTertiary = Color(0xFF3C2F05),
    tertiaryContainer = Color(0xFF544519),
    onTertiaryContainer = Color(0xFFF8E1A6),
    background = Color(0xFF181211),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF181211),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),
    surfaceContainerLowest = Color(0xFF120D0C),
    surfaceContainerLow = Color(0xFF211918),
    surfaceContainer = Color(0xFF251D1C),
    surfaceContainerHigh = Color(0xFF302726),
    surfaceContainerHighest = Color(0xFF3C3230),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),
)

private val CoralLight = lightColorScheme(
    primary = Color(0xFFBF4A30),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF3D0700),
    secondary = Color(0xFF77574E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C150F),
    tertiary = Color(0xFF6D5D2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8E1A6),
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF53433F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF1EE),
    surfaceContainer = Color(0xFFF5EBE8),
    surfaceContainerHigh = Color(0xFFEFE5E2),
    surfaceContainerHighest = Color(0xFFE9DFDD),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD8C2BC),
)

// ---------------- Amber (Warm Golden Amber) ----------------

private val AmberDark = darkColorScheme(
    primary = Color(0xFFFFB951),
    onPrimary = Color(0xFF462A00),
    primaryContainer = Color(0xFF653E00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDFC3A1),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF57432B),
    onSecondaryContainer = Color(0xFFFCE0BE),
    tertiary = Color(0xFFBACD9F),
    onTertiary = Color(0xFF263514),
    tertiaryContainer = Color(0xFF3C4C28),
    onTertiaryContainer = Color(0xFFD6E9B9),
    background = Color(0xFF17130B),
    onBackground = Color(0xFFE8E1D9),
    surface = Color(0xFF17130B),
    onSurface = Color(0xFFE8E1D9),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceContainerLowest = Color(0xFF110E07),
    surfaceContainerLow = Color(0xFF201B13),
    surfaceContainer = Color(0xFF241F17),
    surfaceContainerHigh = Color(0xFF2F2921),
    surfaceContainerHighest = Color(0xFF3B342B),
    outline = Color(0xFF9C8F80),
    outlineVariant = Color(0xFF4F4539),
)

private val AmberLight = lightColorScheme(
    primary = Color(0xFF855400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF705B40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE0BE),
    onSecondaryContainer = Color(0xFF281805),
    tertiary = Color(0xFF53643E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E9B9),
    onTertiaryContainer = Color(0xFF121F03),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201B14),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201B14),
    surfaceVariant = Color(0xFFEFE0CF),
    onSurfaceVariant = Color(0xFF4F4539),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F2EA),
    surfaceContainer = Color(0xFFF3ECE4),
    surfaceContainerHigh = Color(0xFFEDE6DE),
    surfaceContainerHighest = Color(0xFFE8E1D9),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),
)

// ---------------- Rose (Berry Rose) ----------------

private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B294A),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5B3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF472A0C),
    tertiaryContainer = Color(0xFF613F1F),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF181113),
    onBackground = Color(0xFFEDE0E2),
    surface = Color(0xFF181113),
    onSurface = Color(0xFFEDE0E2),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD6C2C6),
    surfaceContainerLowest = Color(0xFF120C0E),
    surfaceContainerLow = Color(0xFF21191C),
    surfaceContainer = Color(0xFF251D20),
    surfaceContainerHigh = Color(0xFF30272A),
    surfaceContainerHighest = Color(0xFF3B3235),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFF984062),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001E),
    secondary = Color(0xFF74565F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1C),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1C),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF0F3),
    surfaceContainer = Color(0xFFF4EAED),
    surfaceContainerHigh = Color(0xFFEEE4E7),
    surfaceContainerHighest = Color(0xFFE8DFE2),
    outline = Color(0xFF847377),
    outlineVariant = Color(0xFFD6C2C6),
)

// ---------------- Monochrome (Grayscale Minimalist) ----------------

private val MonochromeDark = darkColorScheme(
    primary = Color(0xFFE3E3E3),
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFF474747),
    onPrimaryContainer = Color(0xFFE3E3E3),
    secondary = Color(0xFFC7C7C7),
    onSecondary = Color(0xFF2B2B2B),
    secondaryContainer = Color(0xFF424242),
    onSecondaryContainer = Color(0xFFE3E3E3),
    tertiary = Color(0xFFAAAAAA),
    onTertiary = Color(0xFF242424),
    tertiaryContainer = Color(0xFF383838),
    onTertiaryContainer = Color(0xFFE3E3E3),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF474747),
    onSurfaceVariant = Color(0xFFC7C7C7),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF8F8F8F),
    outlineVariant = Color(0xFF474747),
)

private val MonochromeLight = lightColorScheme(
    primary = Color(0xFF363636),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E3E3),
    onPrimaryContainer = Color(0xFF1B1B1B),
    secondary = Color(0xFF5E5E5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E3E3),
    onSecondaryContainer = Color(0xFF1B1B1B),
    tertiary = Color(0xFF757575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF212121),
    background = Color(0xFFFBFBFB),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFBFBFB),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF474747),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFE7E7E7),
    surfaceContainerHighest = Color(0xFFE1E1E1),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFC7C7C7),
)

/**
 * Transforms any dark ColorScheme into AMOLED pitch-black mode with pure #000000 background and surface,
 * and ultra-dark containers/buttons with high-contrast subtle outlines.
 */
private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF0C0C0F),
    surfaceVariant = Color(0xFF0C0C0F),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050507),
    surfaceContainer = Color(0xFF09090C),
    surfaceContainerHigh = Color(0xFF0F0F13),
    surfaceContainerHighest = Color(0xFF15151A),
    outline = Color(0xFF1F1F26),
    outlineVariant = Color(0xFF121217),
)

/** Rounder than stock Material 3 — Pixel's system surfaces sit around 28-32dp. */
private val PixelShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

/** Slightly tighter tracking and heavier headlines, closer to Pixel's system typography. */
private val PixelTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
}

@Composable
fun HiLightTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.DYNAMIC,
    amoledDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED_BLACK -> true
    }
    val isAmoled = isDark && (themeMode == ThemeMode.AMOLED_BLACK || amoledDark)

    val baseScheme: ColorScheme = if (themePalette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        when (themePalette) {
            ThemePalette.DYNAMIC, ThemePalette.INDIGO -> if (isDark) IndigoDark else IndigoLight
            ThemePalette.BLUE -> if (isDark) BlueDark else BlueLight
            ThemePalette.EMERALD -> if (isDark) EmeraldDark else EmeraldLight
            ThemePalette.CORAL -> if (isDark) CoralDark else CoralLight
            ThemePalette.AMBER -> if (isDark) AmberDark else AmberLight
            ThemePalette.ROSE -> if (isDark) RoseDark else RoseLight
            ThemePalette.MONOCHROME -> if (isDark) MonochromeDark else MonochromeLight
        }
    }

    val scheme = if (isAmoled) baseScheme.toAmoled() else baseScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        shapes = PixelShapes,
        typography = PixelTypography,
        content = content,
    )
}
