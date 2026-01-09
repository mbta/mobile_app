package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import kotlin.math.min

@Composable
fun WithStatusIndicators(
    modifier: Modifier = Modifier,
    realtime: Boolean = false,
    lastTrip: Boolean = false,
    scheduleClock: Boolean = false,
    alpha: Float = 1.0f,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.Top,
    ) {
        if (realtime && scheduleClock) {
            throw IllegalArgumentException("A prediction can't be both realtime and scheduled")
        }

        if (realtime) {
            LiveIcon(
                Modifier.alignBy { it.measuredHeight - 4 }.testTag("realtimeIndicator"),
                size = 12.dp,
                alpha = min(0.6f, alpha),
            )
        }
        if (lastTrip && scheduleClock) {
            Image(
                painterResource(R.drawable.fa_clock),
                null,
                Modifier.placeholderIfLoading()
                    .size(12.dp)
                    .padding(top = 2.dp, end = 2.dp)
                    .alignBy { it.measuredHeight }
                    .testTag("lastScheduleIndicator"),
                colorFilter = ColorFilter.tint(LocalContentColor.current),
            )
        }
        if (lastTrip) {
            Text(
                stringResource(R.string.last),
                // Hidden from accessibility because the content description in
                // UpcomingTripView includes it separately
                Modifier.semantics { hideFromAccessibility() }
                    .placeholderIfLoading()
                    .alignByBaseline(),
                style = Typography.footnote,
            )
        }
        content()
    }
}
