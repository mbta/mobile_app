package com.mbta.tid.mbta_app.android.alertDetails

import android.icu.text.DateFormat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mbta.tid.mbta_app.android.util.toJavaDate
import com.mbta.tid.mbta_app.model.Alert
import kotlinx.datetime.Instant

private fun format(instant: Instant): AnnotatedString {
    val date = instant.toJavaDate()
    val formattedDate =
        DateFormat.getInstanceForSkeleton(
                DateFormat.WEEKDAY + DateFormat.ABBR_MONTH + DateFormat.DAY
            )
            .format(date)
    val formattedTime = DateFormat.getInstanceForSkeleton(DateFormat.HOUR_MINUTE).format(date)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formattedDate) }
        append("  ")
        append(formattedTime)
    }
}

fun Alert.ActivePeriod.formatStart() = format(start)

fun Alert.ActivePeriod.formatEnd(): AnnotatedString {
    return end?.let { format(it) }
        ?: AnnotatedString("Until further notice", SpanStyle(fontWeight = FontWeight.Bold))
}
