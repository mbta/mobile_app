package com.mbta.tid.mbta_app.dependencyInjection

import IRepositories
import RealRepositories
import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.network.MobileBackendClient
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.IConfigRepository
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.INearbyRepository
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import com.mbta.tid.mbta_app.repositories.IPinnedRoutesRepository
import com.mbta.tid.mbta_app.repositories.IPredictionsRepository
import com.mbta.tid.mbta_app.repositories.IRailRouteShapeRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.IStopRepository
import com.mbta.tid.mbta_app.repositories.ITripPredictionsRepository
import com.mbta.tid.mbta_app.repositories.ITripRepository
import com.mbta.tid.mbta_app.repositories.IVehicleRepository
import com.mbta.tid.mbta_app.repositories.IVehiclesRepository
import com.mbta.tid.mbta_app.repositories.IVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.ConfigUseCase
import com.mbta.tid.mbta_app.usecases.TogglePinnedRouteUsecase
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import okio.FileSystem
import okio.SYSTEM
import org.koin.core.module.Module
import org.koin.dsl.module

/** Define the koin module with the resources to use in dependency injection */
fun appModule(appVariant: AppVariant) = module {
    includes(
        module { single { MobileBackendClient(appVariant) } },
        module { single { FileSystem.SYSTEM } },
        repositoriesModule(RealRepositories())
    )
}

fun repositoriesModule(repositories: IRepositories): Module {
    return module {
        single<IConfigRepository> { repositories.config }
        single<IErrorBannerStateRepository> { repositories.errorBanner }
        single<IGlobalRepository> { repositories.global }
        single<INearbyRepository> { repositories.nearby }
        single<IOnboardingRepository> { repositories.onboarding }
        single<IPinnedRoutesRepository> { repositories.pinnedRoutes }
        single<IRailRouteShapeRepository> { repositories.railRouteShapes }
        single<ISchedulesRepository> { repositories.schedules }
        single<ISearchResultRepository> { repositories.searchResults }
        single<ISentryRepository> { repositories.sentry }
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
        single<IVisitHistoryRepository> { repositories.visitHistory }
        single { ConfigUseCase(get(), get(), get()) }
        single { TogglePinnedRouteUsecase(get()) }
        single { VisitHistoryUsecase(get()) }
    }
}
