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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
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
                onPrimary = colorResource(R.color.text),
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
                tertiaryContainer = Color.Magenta,
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
                onPrimary = colorResource(R.color.text),
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
                tertiaryContainer = Color.Magenta,
                error = Color.Magenta,
                errorContainer = Color.Magenta,
            )
        }
    val fontFamily =
        FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
            Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
        )

    @Composable
    fun textStyle(fontSize: TextUnit, fontWeight: FontWeight? = null): TextStyle =
        TextStyle(
            // setting to unspecified will inherit local color correctly
            color = Color.Unspecified,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    val typography =
        Typography(
            bodyLarge = textStyle(fontSize = 24.sp),
            bodyMedium = textStyle(fontSize = 17.sp), // Body
            bodySmall = textStyle(fontSize = 16.sp),
            headlineLarge = textStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold), // Headline
            headlineMedium = textStyle(fontSize = 16.sp), // Callout
            headlineSmall = textStyle(fontSize = 15.sp), // Subheadline
            titleLarge = textStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold), // Title 1
            titleMedium = textStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), // Title 2
            titleSmall = textStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), // Title 3
            labelLarge = textStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal), // Footnote
            labelMedium = textStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal), // Caption 1
            labelSmall = textStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal), // Caption 2
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        // default text style is bodyLarge
        ProvideTextStyle(value = MaterialTheme.typography.bodySmall, content = content)
    }
}
