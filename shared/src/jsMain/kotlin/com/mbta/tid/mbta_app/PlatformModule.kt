package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.utils.MockSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
import kotlin.time.Clock
import org.koin.dsl.module

internal fun platformModule() = module {
    single<Clock> { Clock.System }
    single<SystemPaths> { MockSystemPaths("/data", "/cache") }
}
