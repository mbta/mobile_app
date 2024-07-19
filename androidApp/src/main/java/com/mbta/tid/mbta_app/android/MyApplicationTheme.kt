package com.mbta.tid.mbta_app.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors =
        if (darkTheme) {
            darkColorScheme(
                surface = Color(0xFF192026),
                surfaceVariant = Color(0xFF192026),
                background = Color(0xFF000000),
                primary = Color(0xFFE5E5E3),
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                surfaceContainer = Color(0x26FFFFFF)
            )
        } else {
            lightColorScheme(
                surface = Color(0xFFE5E5E3),
                surfaceVariant = Color(0xFFF5F4F2),
                background = Color(0xFFFFFFFF),
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC5),
                tertiary = Color(0xFF3700B3),
                surfaceContainer = Color(0x1A192026)
            )
        }
    val fontFamily = FontFamily(ResourcesCompat.getFont(MainApplication.appContext, R.font.inter_regular)!!)
    val typography =
        Typography(
            bodySmall =
                TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                ),
            bodyMedium =
                TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp
                ),
            bodyLarge =
                TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp
                )
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(0.dp)
        )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
