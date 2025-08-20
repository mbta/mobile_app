package com.mbta.tid.mbta_app.viewModel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("unused")
public class ViewModelDI : KoinComponent {
    public val errorBanner: ErrorBannerViewModel by inject()
    public val favorites: FavoritesViewModel by inject()
    public val map: MapViewModel by inject()
    public val search: SearchViewModel by inject()
    public val searchRoutes: SearchRoutesViewModel by inject()
    public val toast: ToastViewModel by inject()
}
