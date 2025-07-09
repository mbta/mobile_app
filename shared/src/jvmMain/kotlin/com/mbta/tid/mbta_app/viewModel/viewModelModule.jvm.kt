package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun viewModelModule() = module {
    single { FavoritesViewModel(get(), get(named("coroutineDispatcherDefault"))) }
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
    singleOf(::ToastViewModel).bind(IToastViewModel::class)
}
