package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.network.MockPhoenixSocket
import com.mbta.tid.mbta_app.network.PhoenixSocket
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

fun assertMatches(regex: Regex, actual: String) =
    assert(regex.matches(actual)) { "$actual does not match $regex" }

/**
 * Loads mock repositories into the global Koin instance for use in tests, based on the given
 * [objects] by default but with whatever overridden repositories are provided.
 */
fun loadKoinMocks(
    objects: ObjectCollectionBuilder = ObjectCollectionBuilder(),
    analytics: Analytics = MockAnalytics(),
    socket: PhoenixSocket = MockPhoenixSocket(),
    clock: Clock = Clock.System,
    repositoryOverrides: MockRepositories.() -> Unit = {},
) {
    val modules =
        listOf(
            module {
                single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                    Dispatchers.Default
                }
                single<CoroutineDispatcher>(named("coroutineDispatcherIO")) { Dispatchers.IO }

                single<Analytics> { analytics }
                single<PhoenixSocket> { socket }
                single<Clock> { clock }
            },
            repositoriesModule(
                MockRepositories().apply {
                    useObjects(objects)
                    repositoryOverrides()
                }
            ),
            viewModelModule(),
            MainApplication.koinViewModelModule(),
        )
    KoinPlatformTools.defaultContext().loadKoinModules(modules)
}
