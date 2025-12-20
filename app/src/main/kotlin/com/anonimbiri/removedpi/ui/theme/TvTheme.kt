package com.anonimbiri.removedpi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.anonimbiri.removedpi.data.AppTheme

@OptIn(ExperimentalTvMaterial3Api::class)
val TvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RemoveDPITvTheme(
    themeMode: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppTheme.SYSTEM -> darkColorScheme(
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
            background = BackgroundDark,
            onBackground = OnBackgroundDark,
            surface = SurfaceDark,
            onSurface = OnSurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            error = ErrorDark,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            border = OutlineDark,
            borderVariant = OutlineVariantDark
        )
        AppTheme.AMOLED -> darkColorScheme(
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
            background = AmoledBackgroundDark,
            onBackground = AmoledOnBackgroundDark,
            surface = AmoledSurfaceDark,
            onSurface = AmoledOnSurfaceDark,
            surfaceVariant = AmoledSurfaceVariantDark,
            onSurfaceVariant = AmoledOnSurfaceVariantDark,
            error = AmoledErrorDark,
            onError = AmoledOnErrorDark,
            errorContainer = AmoledErrorContainerDark,
            onErrorContainer = AmoledOnErrorContainerDark,
            border = AmoledOutlineDark,
            borderVariant = AmoledOutlineVariantDark
        )
        AppTheme.ANIME -> darkColorScheme(
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
            background = AnimeBackgroundDark,
            onBackground = AnimeOnBackgroundDark,
            surface = AnimeSurfaceDark,
            onSurface = AnimeOnSurfaceDark,
            surfaceVariant = AnimeSurfaceVariantDark,
            onSurfaceVariant = AnimeOnSurfaceVariantDark,
            error = AnimeErrorDark,
            onError = AnimeOnErrorDark,
            errorContainer = AnimeErrorContainerDark,
            onErrorContainer = AnimeOnErrorContainerDark,
            border = AnimeOutlineDark,
            borderVariant = AnimeOutlineVariantDark
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TvTypography,
        content = content
    )
}