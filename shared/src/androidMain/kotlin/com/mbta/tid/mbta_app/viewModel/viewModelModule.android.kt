package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public actual fun viewModelModule(): Module = module {
    singleOf(::ErrorBannerViewModel).bind(IErrorBannerViewModel::class)
    single {
            FavoritesViewModel(
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherDefault")),
                get(),
            )
        }
        .bind(IFavoritesViewModel::class)
    viewModel {
        MapViewModel(
            get(),
            get(),
            get(),
            get(),
            get(named("coroutineDispatcherDefault")),
            get(named("coroutineDispatcherIO")),
        )
    }
    singleOf(::SearchRoutesViewModel).bind(ISearchRoutesViewModel::class)
    singleOf(::SearchViewModel).bind(ISearchViewModel::class)
    // Use singleOf to ensure a shared ToastViewModel across all views that need it, it should be
    // injected using koinInject() rather than koinViewModel(), because if it gets destroyed by the
    // VM lifecycle management, it will break the toast state across different composables.
    singleOf(::ToastViewModel).bind(IToastViewModel::class)
}
