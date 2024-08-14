package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class ManagedPinnedRoutes(
    val pinnedRoutes: Set<String>?,
    val togglePinnedRoute: (String) -> Unit
)

@Composable
fun managePinnedRoutes(
    pinnedRoutesRepository: IPinnedRoutesRepository = koinInject(),
    togglePinnedRouteUsecase: TogglePinnedRouteUsecase = koinInject()
): ManagedPinnedRoutes {
    var pinnedRoutes: Set<String>? by remember { mutableStateOf(null) }

    LaunchedEffect(null) { pinnedRoutes = pinnedRoutesRepository.getPinnedRoutes() }

    val coroutineScope = rememberCoroutineScope()

    val togglePinnedRoute: (String) -> Unit = { routeId ->
        coroutineScope.launch {
            togglePinnedRouteUsecase.execute(routeId)
            pinnedRoutes = pinnedRoutesRepository.getPinnedRoutes()
        }
    }

    return ManagedPinnedRoutes(pinnedRoutes, togglePinnedRoute)
}
