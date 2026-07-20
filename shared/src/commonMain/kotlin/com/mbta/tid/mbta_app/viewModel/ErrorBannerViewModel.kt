package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface IErrorBannerViewModel {
    public val models: StateFlow<ErrorBannerViewModel.State>

    public fun clearState()

    public fun setIsLoadingWhenPredictionsStale(isLoading: Boolean)

    public suspend fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        checkingSheetRoute: SheetRoutes,
        action: () -> Unit,
    )

    public fun setSheetRoute(sheetRoute: SheetRoutes?)
}

public class ErrorBannerViewModel(
    private val errorRepository: IErrorBannerStateRepository,
    private val sentryRepository: ISentryRepository,
) :
    MoleculeViewModel<ErrorBannerViewModel.Event, ErrorBannerViewModel.State>(),
    IErrorBannerViewModel {

    public data class State(
        val loadingWhenPredictionsStale: Boolean = false,
        val errorState: ErrorBannerState? = null,
    ) {
        public constructor() : this(false, null)
    }

    public sealed interface Event {
        public data class SetSheetRoute(val sheetRoute: SheetRoutes?) : Event

        public data object ClearState : Event
    }

    private var awaitingPredictionsAfterBackground: Boolean by mutableStateOf(false)

    @set:JvmName("setSheetRouteState") private var sheetRoute: SheetRoutes? by mutableStateOf(null)

    @Composable
    override fun runLogic(): State {
        LaunchedEffect(Unit) { errorRepository.subscribeToNetworkStatusChanges() }

        val errorState by errorRepository.state.collectAsState()

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                is Event.ClearState -> errorRepository.clearState()
                is Event.SetSheetRoute -> {
                    if (SheetRoutes.pageChanged(sheetRoute, event.sheetRoute)) {
                        errorRepository.clearState()
                    }
                    errorRepository.setSheetRoute(event.sheetRoute)
                    sheetRoute = event.sheetRoute
                }
            }
        }

        return State(awaitingPredictionsAfterBackground, errorState)
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun clearState() {
        fireEvent(Event.ClearState)
    }

    override fun setIsLoadingWhenPredictionsStale(isLoading: Boolean) {
        this.awaitingPredictionsAfterBackground = isLoading
    }

    override suspend fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        checkingSheetRoute: SheetRoutes,
        action: () -> Unit,
    ) {
        errorRepository.checkPredictionsStale(
            predictionsLastUpdated,
            predictionQuantity,
            checkingSheetRoute,
            action,
        )
    }

    override fun setSheetRoute(sheetRoute: SheetRoutes?) {
        fireEvent(Event.SetSheetRoute(sheetRoute))
    }
}

public class MockErrorBannerViewModel
@DefaultArgumentInterop.Enabled
constructor(initialState: ErrorBannerViewModel.State = ErrorBannerViewModel.State()) :
    IErrorBannerViewModel {
    override val models: MutableStateFlow<ErrorBannerViewModel.State> =
        MutableStateFlow(initialState)

    override fun clearState() {}

    override fun setIsLoadingWhenPredictionsStale(isLoading: Boolean) {}

    override suspend fun checkPredictionsStale(
        predictionsLastUpdated: EasternTimeInstant,
        predictionQuantity: Int,
        checkingSheetRoute: SheetRoutes,
        action: () -> Unit,
    ) {}

    override fun setSheetRoute(sheetRoute: SheetRoutes?) {}
}
