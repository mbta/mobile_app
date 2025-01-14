package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
                secondary = colorResource(R.color.key_inverse),
                tertiary = colorResource(R.color.deemphasized),
                outline = colorResource(R.color.halo)
            )
        } else {
            lightColorScheme(
                surface = colorResource(R.color.fill1),
                surfaceVariant = colorResource(R.color.fill2),
                background = colorResource(R.color.fill3),
                primary = colorResource(R.color.key),
                primaryContainer = colorResource(R.color.fill1),
                secondary = colorResource(R.color.key_inverse),
                tertiary = colorResource(R.color.deemphasized),
                outline = colorResource(R.color.halo)
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
            color = colorResource(R.color.text),
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    val typography =
        Typography(
            bodySmall = textStyle(fontSize = 16.sp),
            bodyMedium = textStyle(fontSize = 17.sp),
            bodyLarge = textStyle(fontSize = 24.sp),
            headlineSmall = textStyle(fontSize = 16.sp),
            headlineMedium = textStyle(fontSize = 17.sp),
            headlineLarge = textStyle(fontSize = 20.sp),
            titleLarge = textStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            labelSmall = textStyle(fontSize = 11.sp),
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
