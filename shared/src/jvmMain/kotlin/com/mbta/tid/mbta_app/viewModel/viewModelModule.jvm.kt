package com.mbta.tid.mbta_app.viewModel

import com.mbta.tid.mbta_app.dependencyInjection.KoinName
import org.koin.core.module.Module
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
    single {
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
    single { ToastViewModel(get(named(KoinName.OnEventBufferOverflow))) }
        .bind(IToastViewModel::class)
}
