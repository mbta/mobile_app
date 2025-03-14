package com.mbta.tid.mbta_app.android.alertDetails

import android.content.Context
import android.icu.text.DateFormat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.toJavaDate
import com.mbta.tid.mbta_app.model.Alert
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Instant

private fun format(
    context: Context,
    instant: Instant,
    durationCertainty: Alert.DurationCertainty?,
    start: Boolean
): AnnotatedString {
    val date = instant.toJavaDate()
    val dateFormat =
        DateFormat.getInstanceForSkeleton(
            DateFormat.WEEKDAY + DateFormat.ABBR_MONTH + DateFormat.DAY
        )

    var formattedDate = dateFormat.format(date)
    var formattedTime = DateFormat.getInstanceForSkeleton(DateFormat.HOUR_MINUTE).format(date)

    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.timeZone = TimeZone.getTimeZone("America/New_York")
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minutes = calendar.get(Calendar.MINUTE)
    if (start && hour == 3 && minutes == 0) {
        formattedTime = context.getString(R.string.start_of_service)
    } else if (!start && hour == 2 && minutes == 59) {
        formattedTime = context.getString(R.string.end_of_service)
        val previousDate = instant.minus(24.hours).toJavaDate()
        formattedDate = dateFormat.format(previousDate)
    } else if (!start && durationCertainty == Alert.DurationCertainty.Estimated) {
        formattedTime = context.getString(R.string.later_today)
    }

    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formattedDate) }
        append(", ")
        append(formattedTime)
    }
}

fun Alert.ActivePeriod.formatStart(context: Context) =
    format(context, start, durationCertainty, true)

fun Alert.ActivePeriod.formatEnd(context: Context): AnnotatedString {
    return end?.let { format(context, it, durationCertainty, false) }
        ?: AnnotatedString(
            context.getString(R.string.until_further_notice),
            SpanStyle(fontWeight = FontWeight.Bold)
        )
}
