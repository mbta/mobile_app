package com.mbta.tid.mbta_app.dependencyInjection

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin { modules(appModule()) }
}

fun initMockKoin() {
    startKoin { modules(mockAppModule()) }
}
