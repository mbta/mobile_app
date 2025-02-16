package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.Alert

@Composable
fun Alert.downstreamEffectDescription(): String {
    val effectAhead = R.string.effect_ahead
    return when (effect) {
        Alert.Effect.Detour -> stringResource(effectAhead, stringResource(R.string.detour))
        Alert.Effect.ServiceChange ->
            stringResource(effectAhead, stringResource(R.string.service_change_sentence_case))
        Alert.Effect.Shuttle -> stringResource(effectAhead, stringResource(R.string.shuttle_buses))
        Alert.Effect.SnowRoute ->
            stringResource(effectAhead, stringResource(R.string.snow_route_sentence_case))
        Alert.Effect.StopClosure ->
            stringResource(effectAhead, stringResource(R.string.stop_closed_sentence_case))
        Alert.Effect.Suspension ->
            stringResource(effectAhead, stringResource(R.string.service_suspended))
        else -> stringResource(effectAhead, stringResource(R.string.alert))
    }
}
