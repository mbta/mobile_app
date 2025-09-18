package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

// Use singleOf or single to ensure a shared VM across all views that need it. It should be
// injected using koinInject() rather than koinViewModel(), because if it gets destroyed by the
// VM lifecycle management, it will break VMs that share state across multiple views
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
    single {
        MapViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(named("coroutineDispatcherDefault")),
            get(named("coroutineDispatcherIO")),
        )
    }
    singleOf(::RouteCardDataViewModel).bind(IRouteCardDataViewModel::class)
    singleOf(::SearchRoutesViewModel).bind(ISearchRoutesViewModel::class)
    singleOf(::SearchViewModel).bind(ISearchViewModel::class)
    single {
            StopDetailsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherIO")),
            )
        }
        .bind(IStopDetailsViewModel::class)
    singleOf(::ToastViewModel).bind(IToastViewModel::class)
    singleOf(::TripDetailsPageViewModel).bind(ITripDetailsPageViewModel::class)
    single {
            TripDetailsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherIO")),
            )
        }
        .bind(ITripDetailsViewModel::class)
}
