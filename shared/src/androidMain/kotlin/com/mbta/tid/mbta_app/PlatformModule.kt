package com.mbta.tid.mbta_app

import org.koin.dsl.module

fun platformModule() = module { single { createDataStore(get()) } }
