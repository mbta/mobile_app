package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

public actual fun viewModelModule(): Module = module {
    singleOf(::ErrorBannerViewModel)
    single {
        FavoritesViewModel(get(), get(), get(), get(named("coroutineDispatcherDefault")), get())
    }
    single {
        MapViewModel(
            get(),
            get(),
            get(),
            get(named("coroutineDispatcherDefault")),
            get(named("coroutineDispatcherIO")),
        )
    }
    singleOf(::SearchRoutesViewModel)
    singleOf(::SearchViewModel)
    singleOf(::ToastViewModel)
}
