package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R

object Typography {
    private val fontFamily =
        FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
            Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal)
        )

    private fun textStyle(fontSize: TextUnit): TextStyle =
        TextStyle(
            // setting to unspecified will inherit local color correctly
            color = Color.Unspecified,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = "tnum"
        )

    val largeTitle = textStyle(36.sp)
    val largeTitleBold = largeTitle.merge(fontWeight = FontWeight.Bold)

    val title1 = textStyle(32.sp)
    val title1Bold = title1.merge(fontWeight = FontWeight.Bold)

    val title2 = textStyle(24.sp)
    val title2Bold = title2.merge(fontWeight = FontWeight.Bold)

    val title3 = textStyle(20.sp)
    val title3Semibold = title3.merge(fontWeight = FontWeight.SemiBold)

    val headline = textStyle(17.sp)
    val headlineSemibold = headline.merge(fontWeight = FontWeight.SemiBold)
    val headlineBold = headline.merge(fontWeight = FontWeight.Bold)
    val headlineBoldItalic = headlineBold.merge(fontStyle = FontStyle.Italic)

    val body = textStyle(17.sp)
    val bodySemibold = body.merge(fontWeight = FontWeight.SemiBold)
    val bodyItalic = body.merge(fontStyle = FontStyle.Italic)
    val bodySemiboldItalic = bodySemibold.merge(fontStyle = FontStyle.Italic)

    val callout = textStyle(16.sp)
    val calloutSemibold = callout.merge(fontWeight = FontWeight.SemiBold)
    val calloutItalic = callout.merge(fontStyle = FontStyle.Italic)
    val calloutSemiboldItalic = calloutSemibold.merge(fontStyle = FontStyle.Italic)

    val subheadline = textStyle(15.sp)
    val subheadlineSemibold = subheadline.merge(fontWeight = FontWeight.SemiBold)
    val subheadlineItalic = subheadline.merge(fontStyle = FontStyle.Italic)
    val subheadlineSemiboldItalic = subheadlineSemibold.merge(fontStyle = FontStyle.Italic)

    val footnote = textStyle(13.sp)
    val footnoteSemibold = footnote.merge(fontWeight = FontWeight.SemiBold)
    val footnoteItalic = footnote.merge(fontStyle = FontStyle.Italic)
    val footnoteSemiboldItalic = footnoteSemibold.merge(fontStyle = FontStyle.Italic)

    val caption = textStyle(12.sp)
    val captionMedium = caption.merge(fontWeight = FontWeight.Medium)
    val captionItalic = caption.merge(fontStyle = FontStyle.Italic)
    val captionMediumItalic = captionMedium.merge(fontStyle = FontStyle.Italic)

    val caption2 = textStyle(11.sp)
    val caption2Semibold = caption2.merge(fontWeight = FontWeight.SemiBold)
    val caption2Italic = caption2.merge(fontStyle = FontStyle.Italic)
    val caption2SemiboldItalic = caption2Semibold.merge(fontStyle = FontStyle.Italic)
}
