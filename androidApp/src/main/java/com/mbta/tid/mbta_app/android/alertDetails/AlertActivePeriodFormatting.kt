package com.mbta.tid.mbta_app.android.alertDetails

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.formattedServiceDayAndDate
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.model.Alert

/**
 * Return a localized string containing the date and time of an alert. This includes special
 * handling to check if the alert starts at the beginning of the service day, ends at the end of the
 * service day, or should be shown as ending "later today".
 *
 * @param context The context to get string resources from.
 * @param instant The instant of the active period to format.
 * @param durationCertainty The duration certainty of the active period.
 * @param isStart True if the provided instant is the start of the period, false if it's the end.
 * @return A localized and formatted string describing the active period.
 */
private fun format(
    resources: Resources,
    period: Alert.ActivePeriod,
    isStart: Boolean,
): AnnotatedString {
    val instant =
        if (isStart) period.start
        else
            period.end
                ?: return AnnotatedString(
                    resources.getString(R.string.until_further_notice),
                    SpanStyle(fontWeight = FontWeight.Bold),
                )

    val formattedDate = instant.formattedServiceDayAndDate()
    var formattedTime = instant.formattedTime()

    if (isStart && period.fromStartOfService) {
        formattedTime = resources.getString(R.string.start_of_service)
    } else if (!isStart && period.toEndOfService) {
        formattedTime = resources.getString(R.string.end_of_service)
    } else if (!isStart && period.endingLaterToday) {
        formattedTime = resources.getString(R.string.later_today)
    }

    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formattedDate) }
        append(", ")
        append(formattedTime)
    }
}

fun Alert.ActivePeriod.formatStart(resources: Resources) = format(resources, this, true)

fun Alert.ActivePeriod.formatEnd(resources: Resources) = format(resources, this, false)
