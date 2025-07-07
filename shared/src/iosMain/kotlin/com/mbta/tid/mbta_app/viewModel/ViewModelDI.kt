package com.mbta.tid.mbta_app.viewModel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("unused")
class ViewModelDI : KoinComponent {
    val favorites: FavoritesViewModel by inject()
    val map: MapViewModel by inject()
    val search: SearchViewModel by inject()
    val searchRoutes: SearchRoutesViewModel by inject()
    val toast: ToastViewModel by inject()
}
