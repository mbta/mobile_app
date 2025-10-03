package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.Typography as AppTypography

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            darkColorScheme(
                surface = colorResource(R.color.fill1),
                surfaceVariant = colorResource(R.color.fill2),
                background = colorResource(R.color.fill3),
                primary = colorResource(R.color.key),
                primaryContainer = colorResource(R.color.fill1),
                inversePrimary = colorResource(R.color.key_inverse),
                secondary = colorResource(R.color.key_inverse),
                tertiary = colorResource(R.color.deemphasized),
                outline = colorResource(R.color.halo),
                onPrimary = colorResource(R.color.fill3),
                onPrimaryContainer = colorResource(R.color.text),
                onSecondary = colorResource(R.color.text),
                onSecondaryContainer = colorResource(R.color.text),
                onTertiary = colorResource(R.color.text),
                onTertiaryContainer = colorResource(R.color.text),
                onBackground = colorResource(R.color.text),
                onSurface = colorResource(R.color.text),
                onSurfaceVariant = colorResource(R.color.text),
                inverseOnSurface = colorResource(R.color.text),
                onError = colorResource(R.color.text),
                onErrorContainer = colorResource(R.color.text),
                surfaceTint = colorResource(R.color.key),
                inverseSurface = colorResource(R.color.key_inverse),
                outlineVariant = colorResource(R.color.halo),
                scrim = colorResource(R.color.halo),
                surfaceBright = colorResource(R.color.key_inverse),
                surfaceContainer = colorResource(R.color.fill3),
                surfaceContainerHigh = colorResource(R.color.fill2),
                surfaceContainerHighest = colorResource(R.color.fill1),
                surfaceContainerLow = colorResource(R.color.fill1),
                surfaceContainerLowest = colorResource(R.color.fill2),
                secondaryContainer = Color.Magenta,
                tertiaryContainer = colorResource(R.color.fill1),
                error = Color.Magenta,
                errorContainer = Color.Magenta,
            )
        } else {
            lightColorScheme(
                surface = colorResource(R.color.fill1),
                surfaceVariant = colorResource(R.color.fill2),
                background = colorResource(R.color.fill3),
                primary = colorResource(R.color.key),
                primaryContainer = colorResource(R.color.fill1),
                inversePrimary = colorResource(R.color.key_inverse),
                secondary = colorResource(R.color.key_inverse),
                tertiary = colorResource(R.color.deemphasized),
                outline = colorResource(R.color.halo),
                onPrimary = colorResource(R.color.fill3),
                onPrimaryContainer = colorResource(R.color.text),
                onSecondary = colorResource(R.color.text),
                onSecondaryContainer = colorResource(R.color.text),
                onTertiary = colorResource(R.color.text),
                onTertiaryContainer = colorResource(R.color.text),
                onBackground = colorResource(R.color.text),
                onSurface = colorResource(R.color.text),
                onSurfaceVariant = colorResource(R.color.text),
                inverseOnSurface = colorResource(R.color.text),
                onError = colorResource(R.color.text),
                onErrorContainer = colorResource(R.color.text),
                surfaceTint = colorResource(R.color.key),
                inverseSurface = colorResource(R.color.key_inverse),
                outlineVariant = colorResource(R.color.halo),
                scrim = colorResource(R.color.halo),
                surfaceBright = colorResource(R.color.key_inverse),
                surfaceContainer = colorResource(R.color.fill2),
                surfaceContainerHigh = colorResource(R.color.fill3),
                surfaceContainerHighest = colorResource(R.color.fill3),
                surfaceContainerLow = colorResource(R.color.fill2),
                surfaceContainerLowest = colorResource(R.color.fill2),
                secondaryContainer = Color.Magenta,
                tertiaryContainer = colorResource(R.color.fill2),
                error = Color.Magenta,
                errorContainer = Color.Magenta,
            )
        }

    val typography =
        Typography(
            bodyLarge = AppTypography.title2,
            bodyMedium = AppTypography.body,
            bodySmall = AppTypography.callout,
            headlineLarge = AppTypography.headlineBold,
            headlineMedium = AppTypography.callout,
            headlineSmall = AppTypography.subheadline,
            titleLarge = AppTypography.title1Bold,
            titleMedium = AppTypography.title2Bold,
            titleSmall = AppTypography.title3Semibold,
            labelLarge = AppTypography.footnote,
            labelMedium = AppTypography.caption,
            labelSmall = AppTypography.caption2,
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp),
        )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        ProvideTextStyle(value = AppTypography.body, content = content)
    }
}
