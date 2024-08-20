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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
                surface = colorResource(R.color.fill1), // Fill 1
                surfaceVariant = Color(0xFF192026), // Fill 2
                background = Color(0xFF000000), // Fill 3
                primary = Color(0xFF66B2FF), // Key
                primaryContainer = Color(0xFF3E454D), // Fill 1
                secondary = Color(0xFF006CD9), // Key Inverse
                tertiary = Color(0xFF8A9199), // De-emphasized
                outline = Color(0x26FFFFFF) // Halo
            )
        } else {
            lightColorScheme(
                surface = Color(0xFFE5E5E3), // Fill 1
                surfaceVariant = Color(0xFFF5F4F2), // Fill 2
                background = Color(0xFFFFFFFF), // Fill 3
                primary = Color(0xFF006CD9), // Key
                secondary = Color(0xFF66B2FF), // Key Inverse
                tertiary = Color(0xFF8A9199), // De-emphasized
                outline = Color(0x1A192026) // Halo
            )
        }
    val fontFamily =
        FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
            Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
        )
    val typography =
        Typography(
            bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 17.sp),
            bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = 24.sp),
            headlineSmall = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
            headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = 17.sp),
            headlineLarge = TextStyle(fontFamily = fontFamily, fontSize = 20.sp),
            titleLarge =
                TextStyle(fontFamily = fontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            labelSmall = TextStyle(fontFamily = fontFamily, fontSize = 11.sp),
        )
    val shapes =
        Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
