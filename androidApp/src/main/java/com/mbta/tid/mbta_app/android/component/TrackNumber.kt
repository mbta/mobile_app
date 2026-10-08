package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.model.TripDetailsStopList

@Composable
fun TrackNumber(track: TripDetailsStopList.Track) {
    val trackText =
        when (track) {
            is TripDetailsStopList.Track.Number ->
                stringResource(R.string.track_number, track.number)
            is TripDetailsStopList.Track.TBD -> stringResource(R.string.track_tbd)
        }
    val trackDescription =
        when (track) {
            is TripDetailsStopList.Track.Number ->
                stringResource(R.string.boarding_track, track.number)
            is TripDetailsStopList.Track.TBD -> stringResource(R.string.boarding_track_tbd)
        }
    Text(
        trackText,
        Modifier.semantics { contentDescription = trackDescription }.placeholderIfLoading(),
        color = colorResource(R.color.text),
        style = Typography.footnote,
    )
}
