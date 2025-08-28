package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public actual fun viewModelModule(): Module = module {
    singleOf(::ErrorBannerViewModel).bind(IErrorBannerViewModel::class)
    single {
        FavoritesViewModel(get(), get(), get(), get(named("coroutineDispatcherDefault")), get())
    }
    single {
        MapViewModel(
            get(),
            get(),
            get(),
            get(),
            get(named("coroutineDispatcherDefault")),
            get(named("coroutineDispatcherIO")),
        )
    }
    singleOf(::RouteCardDataViewModel).bind(IRouteCardDataViewModel::class)
    singleOf(::SearchRoutesViewModel)
    singleOf(::SearchViewModel)
    single { StopDetailsViewModel(get(), get(), get(), get(), get(named("coroutineDispatcherIO"))) }
        .bind(IStopDetailsViewModel::class)
    singleOf(::ToastViewModel)
    single { TripDetailsViewModel(get(), get(), get(), get(), get(named("coroutineDispatcherIO"))) }
        .bind(ITripDetailsViewModel::class)
}
