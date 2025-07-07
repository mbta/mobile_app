package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule() = module {
    // can’t `viewModel<IFavoritesViewModel>` since `IFavoritesViewModel` might not be a real
    // Android ViewModel
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::MapViewModel)
    viewModelOf(::SearchRoutesViewModel)
    viewModelOf(::SearchViewModel)
    // Use singleOf to ensure a shared ToastViewModel across all views that need it, it should be
    // injected using koinInject() rather than koinViewModel(), because if it gets destroyed by the
    // VM lifecycle management, it will break the toast state across different composables.
    singleOf(::ToastViewModel)
}
