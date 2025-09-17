package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.utils.AndroidSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
import kotlin.time.Clock
import org.koin.dsl.module

internal fun platformModule() = module {
    includes(
        module { single { createDataStore(get()) } },
        module { single<SystemPaths> { AndroidSystemPaths(get()) } },
        module { single<Clock> { Clock.System } },
    )
}
