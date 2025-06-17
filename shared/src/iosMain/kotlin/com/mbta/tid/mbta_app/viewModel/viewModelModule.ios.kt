package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun viewModelModule() = module {
    singleOf(::FavoritesViewModel)
    singleOf(::SearchRoutesViewModel)
    singleOf(::SearchViewModel)
}
