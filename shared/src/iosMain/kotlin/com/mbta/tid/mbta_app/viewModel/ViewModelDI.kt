package com.mbta.tid.mbta_app.viewModel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("unused")
class ViewModelDI : KoinComponent {
    val search: SearchViewModel by inject()
    val searchRoutes: SearchRoutesViewModel by inject()
}
