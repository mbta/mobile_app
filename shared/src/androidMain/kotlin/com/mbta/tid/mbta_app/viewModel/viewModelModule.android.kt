package com.mbta.tid.mbta_app.viewModel

import com.mbta.tid.mbta_app.dependencyInjection.KoinName
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public actual fun viewModelModule(): Module = module {
    single { ErrorBannerViewModel(get(), get(), get(), get(named(KoinName.OnEventBufferOverflow))) }
        .bind(IErrorBannerViewModel::class)
    single {
            FavoritesViewModel(
                get(),
                get(),
                get(),
                get(named(KoinName.CoroutineDispatcherDefault)),
                get(),
                get(named(KoinName.OnEventBufferOverflow)),
            )
        }
        .bind(IFavoritesViewModel::class)
    viewModel {
            MapViewModel(
                get(),
                get(),
                get(),
                get(named(KoinName.CoroutineDispatcherDefault)),
                get(named(KoinName.CoroutineDispatcherIO)),
                get(named(KoinName.OnEventBufferOverflow)),
            )
        }
        .bind(IMapViewModel::class)
    single {
            SearchRoutesViewModel(get(), get(), get(), get(named(KoinName.OnEventBufferOverflow)))
        }
        .bind(ISearchRoutesViewModel::class)
    single {
            SearchViewModel(get(), get(), get(), get(), get(named(KoinName.OnEventBufferOverflow)))
        }
        .bind(ISearchViewModel::class)
    // Use singleOf to ensure a shared ToastViewModel across all views that need it, it should be
    // injected using koinInject() rather than koinViewModel(), because if it gets destroyed by the
    // VM lifecycle management, it will break the toast state across different composables.
    single { ToastViewModel(get(named(KoinName.OnEventBufferOverflow))) }
        .bind(IToastViewModel::class)
}
