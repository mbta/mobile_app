package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight

fun AnnotatedString.toHtml(): String {
    // conveniently, we only use bold
    var result = this.text
    for (range in this.spanStyles.asReversed()) {
        if (range.item.fontWeight != FontWeight.Normal) {
            result = result.replaceRange(range.end, range.end, "</b>")
            result = result.replaceRange(range.start, range.start, "<b>")
        }
    }
    return result
}
