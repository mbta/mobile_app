package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun viewModelModule() = module {
    singleOf(::FavoritesViewModel)
    singleOf(::MapViewModel)
    singleOf(::SearchRoutesViewModel)
    singleOf(::SearchViewModel)
    singleOf(::ToastViewModel).bind(IToastViewModel::class)
}
