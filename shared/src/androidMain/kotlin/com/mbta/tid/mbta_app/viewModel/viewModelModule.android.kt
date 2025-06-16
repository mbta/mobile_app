package com.mbta.tid.mbta_app.viewModel

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule() = module {
    viewModelOf(::SearchRoutesViewModel)
    viewModelOf(::SearchViewModel)
}
