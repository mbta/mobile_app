package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.utils.IOSSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
import org.koin.dsl.module

fun platformModule() = module {
    includes(
        module { single { createDataStore() } },
        module { single<SystemPaths> { IOSSystemPaths() } }
    )
}
