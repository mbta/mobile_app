package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.cache.createCacheFile
import org.koin.dsl.module

fun platformModule() = module {
    includes(
        module { single { createDataStore() } },
        module { factory { params -> createCacheFile(params.get()) } }
    )
}
