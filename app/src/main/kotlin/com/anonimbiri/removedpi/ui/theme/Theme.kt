package com.anonimbiri.removedpi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.anonimbiri.removedpi.data.AppTheme

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║                              VARSAYILAN TEMA                                  ║
// ╚══════════════════════════════════════════════════════════════════════════════╝

private val DefaultLightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DefaultDarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║                               ANIME TEMA                                      ║
// ╚══════════════════════════════════════════════════════════════════════════════╝

private val AnimeLightScheme = lightColorScheme(
    primary = AnimePrimaryLight,
    onPrimary = AnimeOnPrimaryLight,
    primaryContainer = AnimePrimaryContainerLight,
    onPrimaryContainer = AnimeOnPrimaryContainerLight,
    secondary = AnimeSecondaryLight,
    onSecondary = AnimeOnSecondaryLight,
    secondaryContainer = AnimeSecondaryContainerLight,
    onSecondaryContainer = AnimeOnSecondaryContainerLight,
    tertiary = AnimeTertiaryLight,
    onTertiary = AnimeOnTertiaryLight,
    tertiaryContainer = AnimeTertiaryContainerLight,
    onTertiaryContainer = AnimeOnTertiaryContainerLight,
    error = AnimeErrorLight,
    onError = AnimeOnErrorLight,
    errorContainer = AnimeErrorContainerLight,
    onErrorContainer = AnimeOnErrorContainerLight,
    background = AnimeBackgroundLight,
    onBackground = AnimeOnBackgroundLight,
    surface = AnimeSurfaceLight,
    onSurface = AnimeOnSurfaceLight,
    surfaceVariant = AnimeSurfaceVariantLight,
    onSurfaceVariant = AnimeOnSurfaceVariantLight,
    outline = AnimeOutlineLight,
    outlineVariant = AnimeOutlineVariantLight
)

private val AnimeDarkScheme = darkColorScheme(
    primary = AnimePrimaryDark,
    onPrimary = AnimeOnPrimaryDark,
    primaryContainer = AnimePrimaryContainerDark,
    onPrimaryContainer = AnimeOnPrimaryContainerDark,
    secondary = AnimeSecondaryDark,
    onSecondary = AnimeOnSecondaryDark,
    secondaryContainer = AnimeSecondaryContainerDark,
    onSecondaryContainer = AnimeOnSecondaryContainerDark,
    tertiary = AnimeTertiaryDark,
    onTertiary = AnimeOnTertiaryDark,
    tertiaryContainer = AnimeTertiaryContainerDark,
    onTertiaryContainer = AnimeOnTertiaryContainerDark,
    error = AnimeErrorDark,
    onError = AnimeOnErrorDark,
    errorContainer = AnimeErrorContainerDark,
    onErrorContainer = AnimeOnErrorContainerDark,
    background = AnimeBackgroundDark,
    onBackground = AnimeOnBackgroundDark,
    surface = AnimeSurfaceDark,
    onSurface = AnimeOnSurfaceDark,
    surfaceVariant = AnimeSurfaceVariantDark,
    onSurfaceVariant = AnimeOnSurfaceVariantDark,
    outline = AnimeOutlineDark,
    outlineVariant = AnimeOutlineVariantDark
)

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║                               AMOLED TEMA                                     ║
// ╚══════════════════════════════════════════════════════════════════════════════╝

private val AmoledLightScheme = lightColorScheme(
    primary = AmoledPrimaryLight,
    onPrimary = AmoledOnPrimaryLight,
    primaryContainer = AmoledPrimaryContainerLight,
    onPrimaryContainer = AmoledOnPrimaryContainerLight,
    secondary = AmoledSecondaryLight,
    onSecondary = AmoledOnSecondaryLight,
    secondaryContainer = AmoledSecondaryContainerLight,
    onSecondaryContainer = AmoledOnSecondaryContainerLight,
    tertiary = AmoledTertiaryLight,
    onTertiary = AmoledOnTertiaryLight,
    tertiaryContainer = AmoledTertiaryContainerLight,
    onTertiaryContainer = AmoledOnTertiaryContainerLight,
    error = AmoledErrorLight,
    onError = AmoledOnErrorLight,
    errorContainer = AmoledErrorContainerLight,
    onErrorContainer = AmoledOnErrorContainerLight,
    background = AmoledBackgroundLight,
    onBackground = AmoledOnBackgroundLight,
    surface = AmoledSurfaceLight,
    onSurface = AmoledOnSurfaceLight,
    surfaceVariant = AmoledSurfaceVariantLight,
    onSurfaceVariant = AmoledOnSurfaceVariantLight,
    outline = AmoledOutlineLight,
    outlineVariant = AmoledOutlineVariantLight
)

private val AmoledDarkScheme = darkColorScheme(
    primary = AmoledPrimaryDark,
    onPrimary = AmoledOnPrimaryDark,
    primaryContainer = AmoledPrimaryContainerDark,
    onPrimaryContainer = AmoledOnPrimaryContainerDark,
    secondary = AmoledSecondaryDark,
    onSecondary = AmoledOnSecondaryDark,
    secondaryContainer = AmoledSecondaryContainerDark,
    onSecondaryContainer = AmoledOnSecondaryContainerDark,
    tertiary = AmoledTertiaryDark,
    onTertiary = AmoledOnTertiaryDark,
    tertiaryContainer = AmoledTertiaryContainerDark,
    onTertiaryContainer = AmoledOnTertiaryContainerDark,
    error = AmoledErrorDark,
    onError = AmoledOnErrorDark,
    errorContainer = AmoledErrorContainerDark,
    onErrorContainer = AmoledOnErrorContainerDark,
    background = AmoledBackgroundDark,
    onBackground = AmoledOnBackgroundDark,
    surface = AmoledSurfaceDark,
    onSurface = AmoledOnSurfaceDark,
    surfaceVariant = AmoledSurfaceVariantDark,
    onSurfaceVariant = AmoledOnSurfaceVariantDark,
    outline = AmoledOutlineDark,
    outlineVariant = AmoledOutlineVariantDark
)

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║                              THEME COMPOSABLE                                 ║
// ╚══════════════════════════════════════════════════════════════════════════════╝

@Composable
fun RemoveDPITheme(
    themeMode: AppTheme = AppTheme.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when (themeMode) {
        AppTheme.SYSTEM -> {
            // Android 12+ ise Dynamic Colors kullan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) 
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DefaultDarkScheme 
                else DefaultLightScheme
            }
        }
        AppTheme.AMOLED -> {
            if (darkTheme) AmoledDarkScheme 
            else AmoledLightScheme
        }
        AppTheme.ANIME -> {
            if (darkTheme) AnimeDarkScheme 
            else AnimeLightScheme
        }
    }

    // Status bar rengini ayarla
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}