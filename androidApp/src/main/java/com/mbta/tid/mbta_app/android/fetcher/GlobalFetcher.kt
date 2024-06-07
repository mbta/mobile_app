package com.mbta.tid.mbta_app.android.fetcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.Backend
import com.mbta.tid.mbta_app.model.response.GlobalResponse

data class GlobalData(
    val response: GlobalResponse? = null,
) {
    val stops = response?.stops ?: emptyMap()
    val routes = response?.routes ?: emptyMap()

    constructor(response: GlobalResponse) : this(response)
}

@Composable
fun fetchGlobalData(backend: Backend): GlobalData {
    var data by remember { mutableStateOf(GlobalData()) }

    LaunchedEffect(null) { data = GlobalData(backend.getGlobalData()) }

    return data
}
