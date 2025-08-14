package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.network.INetworkConnectivityMonitor
import com.mbta.tid.mbta_app.network.NetworkConnectivityMonitor
import com.mbta.tid.mbta_app.utils.IOSSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
import org.koin.dsl.module

internal fun platformModule() = module {
    includes(
        module { single { createDataStore() } },
        module { single<SystemPaths> { IOSSystemPaths() } },
        module { single<INetworkConnectivityMonitor> { NetworkConnectivityMonitor() } },
    )
}
