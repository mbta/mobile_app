package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

sealed class TrackChangePillShape {
    object Compact : TrackChangePillShape()

    object Expanded : TrackChangePillShape()
}

sealed class TrackChangePillType {
    object Alert : TrackChangePillType()

    class Scheduled(val trackNumber: String, val trackLabel: String?) : TrackChangePillType()
}

@Composable
fun TrackChangePill(
    pillType: TrackChangePillType,
    pillShape: TrackChangePillShape,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding =
        when (pillShape) {
            is TrackChangePillShape.Compact -> 2.dp
            is TrackChangePillShape.Expanded -> 4.dp
        }
    val horizontalArrangement =
        when (pillShape) {
            is TrackChangePillShape.Compact -> Arrangement.Center
            is TrackChangePillShape.Expanded -> Arrangement.spacedBy(8.dp)
        }
    val backgroundColor =
        when (pillType) {
            is TrackChangePillType.Alert ->
                colorResource(R.color.track_change_pill_alert_background)
            // TODO: Add finalized color for scheduled track number pill type
            else -> colorResource(R.color.deemphasized)
        }

    Row(
        modifier
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
            tint = colorResource(R.color.text),
            modifier = Modifier.size(16.dp),
        )
        when (pillShape) {
            is TrackChangePillShape.Expanded ->
                Text(
                    text =
                        when (pillType) {
                            is TrackChangePillType.Alert -> "Track change"
                            // TODO: Add finalized text for scheduled track number pill type
                            else -> ""
                        },
                    color = colorResource(R.color.text),
                    style = Typography.footnote,
                )
            else -> {}
        }
        when (pillShape) {
            is TrackChangePillShape.Compact -> {}
            is TrackChangePillShape.Expanded ->
                Icon(
                    painterResource(R.drawable.fa_circle_info),
                    contentDescription = null,
                    tint = colorResource(R.color.deemphasized),
                    modifier = Modifier.size(16.dp),
                )
        }
    }
}
