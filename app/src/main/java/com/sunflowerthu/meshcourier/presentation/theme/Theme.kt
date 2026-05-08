package com.sunflowerthu.meshcourier.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = OnBrandTeal,
    primaryContainer = BrandTealContainer,
    onPrimaryContainer = OnBrandTealContainer,

    secondary = BrandCoral,
    onSecondary = OnBrandCoral,
    secondaryContainer = BrandCoralContainer,
    onSecondaryContainer = OnBrandCoralContainer,

    tertiary = BrandSage,
    onTertiary = OnBrandSage,
    tertiaryContainer = BrandSageContainer,
    onTertiaryContainer = OnBrandSageContainer,

    background = BrandSurface,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandTealDark,
    onPrimary = OnBrandTealDark,
    primaryContainer = BrandTealContainerDark,
    onPrimaryContainer = OnBrandTealContainerDark,

    secondary = BrandCoralDark,
    onSecondary = OnBrandCoralDark,
    secondaryContainer = BrandCoralContainerDark,
    onSecondaryContainer = OnBrandCoralContainerDark,

    tertiary = BrandSageDark,
    onTertiary = OnBrandSageDark,
    tertiaryContainer = BrandSageContainerDark,
    onTertiaryContainer = OnBrandSageContainerDark,

    background = BrandSurfaceDark,
    onBackground = BrandOnSurfaceDark,
    surface = BrandSurfaceDark,
    onSurface = BrandOnSurfaceDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark,
)

@Composable
fun MeshCourierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}