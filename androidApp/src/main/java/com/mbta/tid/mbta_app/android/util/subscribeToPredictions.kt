package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import org.koin.compose.koinInject

@Composable
fun subscribeToPredictions(
    stopIds: List<String>?,
    predictionsRepository: IPredictionsRepository = koinInject()
): PredictionsStreamDataResponse? {
    var predictions: PredictionsStreamDataResponse? by remember { mutableStateOf(null) }

    DisposableEffect(stopIds) {
        if (stopIds != null) {
            predictionsRepository.connect(stopIds) { predictions = it.data }
        }
        onDispose { predictionsRepository.disconnect() }
    }

    return predictions
}
