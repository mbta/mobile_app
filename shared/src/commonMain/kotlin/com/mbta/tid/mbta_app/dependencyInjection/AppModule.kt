package com.mbta.tid.mbta_app.dependencyInjection

import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.IdleScheduleRepository
import com.mbta.tid.mbta_app.repositories.PinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.SchedulesRepository
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule() = module {
    single { MobileBackendClient() }
    single<ISchedulesRepository> { SchedulesRepository() }
    single<IPinnedRoutesRepository> { PinnedRoutesRepository() }
    single { TogglePinnedRouteUsecase(get()) }
}

fun mockAppModule() = module {
    single {
        MobileBackendClient(
            MockEngine { _ ->
                respond(
                    content = ByteReadChannel("""
                """),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )
    }
    single<ISchedulesRepository> { IdleScheduleRepository() }
}
