package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule() = module {
    // can’t `viewModel<IFavoritesViewModel>` since `IFavoritesViewModel` might not be a real
    // Android ViewModel
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::SearchRoutesViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::MapViewModel)
}
