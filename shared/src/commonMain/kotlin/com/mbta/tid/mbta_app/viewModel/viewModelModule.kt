package com.mbta.tid.mbta_app.viewModel

import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * On Android, creates a ViewModel factory with a navigation-integrated lifecycle; on iOS, just
 * creates a singleton. Some ViewModels need to be shared across navigation layers, so those should
 * just be declared with a platform-independent [single].
 */
internal expect inline fun <reified T : MoleculeScopeViewModel> Module.viewModel(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>,
): KoinDefinition<T>

public fun viewModelModule(): Module = module {
    singleOf(::ErrorBannerViewModel).bind(IErrorBannerViewModel::class)
    single {
            FavoritesViewModel(
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherDefault")),
                get(),
            )
        }
        .bind(IFavoritesViewModel::class)
    single {
            MapViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherDefault")),
                get(named("coroutineDispatcherIO")),
            )
        }
        .bind(IMapViewModel::class)
    singleOf(::RouteCardDataViewModel).bind(IRouteCardDataViewModel::class)
    singleOf(::SearchRoutesViewModel).bind(ISearchRoutesViewModel::class)
    singleOf(::SearchViewModel).bind(ISearchViewModel::class)
    single {
            StopDetailsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherIO")),
            )
        }
        .bind(IStopDetailsViewModel::class)
    singleOf(::ToastViewModel).bind(IToastViewModel::class)
    singleOf(::TripDetailsPageViewModel).bind(ITripDetailsPageViewModel::class)
    single {
            TripDetailsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("coroutineDispatcherIO")),
            )
        }
        .bind(ITripDetailsViewModel::class)
}
