package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.repositories.AlertsRepository
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.PredictionsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

fun makeNativeModule(socket: PhoenixSocket): Module {
    return module {
        single<PhoenixSocket> { socket }
        factory<IPredictionsRepository> { PredictionsRepository(get()) }
        factory<IAlertsRepository> { AlertsRepository(get()) }
    }
}
