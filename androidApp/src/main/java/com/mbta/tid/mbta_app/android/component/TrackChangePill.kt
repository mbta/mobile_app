package com.mbta.tid.mbta_app.android.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

sealed class TrackChangePillStyle {
    class Compact(val isDimmed: Boolean = false) : TrackChangePillStyle()

    object Expanded : TrackChangePillStyle()
}

sealed class TrackChangePillType {
    object Alert : TrackChangePillType()

    class Scheduled(val trackNumber: String, val trackLabel: String?) : TrackChangePillType()
}

@Composable
fun TrackChangePill(
    pillType: TrackChangePillType,
    pillStyle: TrackChangePillStyle,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding =
        when (pillStyle) {
            is TrackChangePillStyle.Compact -> 2.dp
            is TrackChangePillStyle.Expanded -> 4.dp
        }
    val horizontalArrangement =
        when (pillStyle) {
            is TrackChangePillStyle.Compact -> Arrangement.Center
            is TrackChangePillStyle.Expanded -> Arrangement.spacedBy(8.dp)
        }
    val backgroundColor =
        when (pillType) {
            is TrackChangePillType.Alert ->
                colorResource(R.color.track_change_pill_alert_background)
            is TrackChangePillType.Scheduled -> {
                if (pillStyle is TrackChangePillStyle.Compact && pillStyle.isDimmed) {
                    colorResource(R.color.halo_dark)
                } else {
                    colorResource(R.color.scheduled_track_change_background)
                }
            }
        }

    val iconColor =
        when (pillType) {
            is TrackChangePillType.Alert -> colorResource(R.color.track_change_pill_icon)

            is TrackChangePillType.Scheduled -> {
                if (pillStyle is TrackChangePillStyle.Compact && pillStyle.isDimmed) {
                    Color.White
                } else {
                    colorResource(R.color.track_change_pill_icon)
                }
            }
        }

    Row(
        modifier
            .testTag("track_change_pill")
            .heightIn(min = 24.dp)
            .widthIn(min = 24.dp)
            .background(backgroundColor, shape = RoundedCornerShape(88.dp))
            .padding(horizontal = horizontalPadding, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        Icon(
            painterResource(R.drawable.fa_shuffle),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp),
        )
        when (pillStyle) {
            is TrackChangePillStyle.Expanded ->
                Text(
                    text =
                        when (pillType) {
                            is TrackChangePillType.Alert -> "Track change"
                            is TrackChangePillType.Scheduled ->
                                pillType.trackLabel ?: pillType.trackNumber
                        },
                    color = iconColor,
                    style = Typography.footnote,
                )
            else -> {}
        }
        when (pillStyle) {
            is TrackChangePillStyle.Compact -> {}
            is TrackChangePillStyle.Expanded ->
                Icon(
                    painterResource(R.drawable.fa_circle_info),
                    contentDescription = null,
                    tint = colorResource(R.color.deemphasized),
                    modifier = Modifier.size(16.dp),
                )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun TrackChangePillPreview() {
    Column(Modifier.background(colorResource(R.color.fill3))) {
        TrackChangePill(
            pillType = TrackChangePillType.Alert,
            pillStyle = TrackChangePillStyle.Expanded,
        )
        TrackChangePill(
            pillType = TrackChangePillType.Alert,
            pillStyle = TrackChangePillStyle.Compact(),
        )
        TrackChangePill(
            pillType = TrackChangePillType.Scheduled("2", "Track 2"),
            pillStyle = TrackChangePillStyle.Expanded,
        )
        TrackChangePill(
            pillType = TrackChangePillType.Scheduled("2", "Track 2"),
            pillStyle = TrackChangePillStyle.Compact(),
        )
        Column(modifier = Modifier.background(colorResource(R.color.text))) {
            TrackChangePill(
                pillType = TrackChangePillType.Scheduled("2", "Track 2"),
                pillStyle = TrackChangePillStyle.Compact(isDimmed = true),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TrackChangePillPreviewDark() {
    Column(Modifier.background(colorResource(R.color.fill3))) {
        TrackChangePill(
            pillType = TrackChangePillType.Alert,
            pillStyle = TrackChangePillStyle.Expanded,
        )
        TrackChangePill(
            pillType = TrackChangePillType.Alert,
            pillStyle = TrackChangePillStyle.Compact(),
        )
        TrackChangePill(
            pillType = TrackChangePillType.Scheduled("2", "Track 2"),
            pillStyle = TrackChangePillStyle.Expanded,
        )
        TrackChangePill(
            pillType = TrackChangePillType.Scheduled("2", "Track 2"),
            pillStyle = TrackChangePillStyle.Compact(),
        )
        TrackChangePill(
            pillType = TrackChangePillType.Scheduled("2", "Track 2"),
            pillStyle = TrackChangePillStyle.Compact(isDimmed = true),
        )
    }
}
