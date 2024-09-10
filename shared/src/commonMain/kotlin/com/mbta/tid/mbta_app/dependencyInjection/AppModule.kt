package com.mbta.tid.mbta_app.dependencyInjection

import IRepositories
import RealRepositories
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.cache.ResponseCache
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.GetSettingUsecase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import org.koin.core.module.Module
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule(appVariant: AppVariant) = module {
    includes(
        module { single { MobileBackendClient(appVariant) } },
        cacheModule(),
        repositoriesModule(RealRepositories())
    )
}

fun cacheModule() = module {
    factory { params ->
        if (params.isNotEmpty()) {
            ResponseCache(maxAge = params.get())
        } else {
            ResponseCache()
        }
    }
}

fun repositoriesModule(repositories: IRepositories): Module {
    return module {
        single<IConfigRepository> { repositories.config }
        single<IGlobalRepository> { repositories.global }
        single<INearbyRepository> { repositories.nearby }
        single<IPinnedRoutesRepository> { repositories.pinnedRoutes }
        single<IRailRouteShapeRepository> { repositories.railRouteShapes }
        single<ISchedulesRepository> { repositories.schedules }
        single<ISearchResultRepository> { repositories.searchResults }
        single<ISettingsRepository> { repositories.settings }
        single<IStopRepository> { repositories.stop }
        single<ITripRepository> { repositories.trip }
        repositories.alerts?.let { alertsRepo -> factory<IAlertsRepository> { alertsRepo } }
        repositories.appCheck?.let { appCheckRepo -> factory<IAppCheckRepository> { appCheckRepo } }
        repositories.predictions?.let { predictionsRepo ->
            factory<IPredictionsRepository> { predictionsRepo }
        }
        repositories.tripPredictions?.let { tripPredictionsRepo ->
            factory<ITripPredictionsRepository> { tripPredictionsRepo }
        }
        repositories.vehicle?.let { vehicleRepo -> factory<IVehicleRepository> { vehicleRepo } }
        repositories.vehicles?.let { vehiclesRepo -> factory<IVehiclesRepository> { vehiclesRepo } }
        single { ConfigUseCase(get(), get()) }
        single { GetSettingUsecase(get()) }
        single { TogglePinnedRouteUsecase(get()) }
    }
}
