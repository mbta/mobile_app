package com.mbta.tid.mbta_app.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.repositories.IOnboardingRepository
import com.mbta.tid.mbta_app.repositories.ISentryRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import com.mbta.tid.mbta_app.utils.Targeting
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public interface INotificationsBetaViewModel {
    public val models: StateFlow<NotificationsBetaViewModel.State>

    public fun dismissBetaDialog()

    public fun dismissBetaToast()

    public fun setInstanceId(instanceId: String?)

    public fun setNotificationsEnabled(enabled: Boolean)

    public fun setSheetRoute(sheetRoute: SheetRoutes?)
}

public class NotificationsBetaViewModel(
    private val onboardingRepository: IOnboardingRepository,
    private val sentryRepository: ISentryRepository,
    private val toastViewModel: IToastViewModel,
) :
    MoleculeViewModel<NotificationsBetaViewModel.Event, NotificationsBetaViewModel.State>(),
    INotificationsBetaViewModel {

    public data class State(
        val showBetaToast: Boolean = false,
        val showBetaDialog: Boolean = false,
    ) {
        public constructor() : this(false, false)
    }

    public sealed interface Event {
        public data class SetSheetRoute(val sheetRoute: SheetRoutes?) : Event

        public data object DismissDialog : Event

        public data object DismissToast : Event
    }

    @set:JvmName("setInstanceIdState") private var instanceId: String? by mutableStateOf(null)
    @set:JvmName("setNotificationsEnabledState")
    private var notificationsEnabled: Boolean by mutableStateOf(false)

    @set:JvmName("setSheetRouteState") private var sheetRoute: SheetRoutes? by mutableStateOf(null)

    @Composable
    override fun runLogic(): State {
        var showBetaDialog by remember { mutableStateOf<Boolean?>(null) }
        var showBetaToast by remember { mutableStateOf<Boolean?>(null) }
        var overrideTargeting by remember { mutableStateOf<Boolean?>(null) }

        val inBetaGroup =
            remember(instanceId, overrideTargeting) {
                overrideTargeting ?: Targeting.get(Targeting.Target.NotificationsBeta, instanceId)
            }

        LaunchedEffect(Unit) {
            showBetaDialog = onboardingRepository.notificationsBetaFeedbackDialogShouldShow()
            showBetaToast = onboardingRepository.notificationsBetaPromptShouldShow()
            overrideTargeting = onboardingRepository.notificationsBetaTargetingOverride()
        }

        LaunchedEffect(showBetaToast) {
            if (showBetaToast == false) {
                toastViewModel.hideToast()
                onboardingRepository.notificationsBetaPromptDismissed()
            }
        }

        LaunchedEffect(showBetaDialog) {
            showBetaDialog?.let { onboardingRepository.notificationsBetaFeedbackDialogSetState(it) }
        }

        LaunchedEffect(notificationsEnabled, showBetaDialog) {
            if (!notificationsEnabled && showBetaDialog == false) showBetaDialog = true
        }

        EventSink(eventHandlingTimeout = 1.seconds, sentryRepository = sentryRepository) { event ->
            when (event) {
                Event.DismissDialog -> showBetaDialog = false
                Event.DismissToast -> showBetaToast = false
                is Event.SetSheetRoute -> sheetRoute = event.sheetRoute
            }
        }

        return State(
            inBetaGroup &&
                !notificationsEnabled &&
                sheetRoute is SheetRoutes.Entrypoint &&
                showBetaToast == true,
            notificationsEnabled && showBetaDialog == true,
        )
    }

    override val models: StateFlow<State>
        get() = internalModels

    override fun dismissBetaDialog(): Unit = fireEvent(Event.DismissDialog)

    override fun dismissBetaToast(): Unit = fireEvent(Event.DismissToast)

    override fun setInstanceId(instanceId: String?): Unit {
        this.instanceId = instanceId
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    override fun setSheetRoute(sheetRoute: SheetRoutes?): Unit =
        fireEvent(Event.SetSheetRoute(sheetRoute))
}

public class MockNotificationsBetaViewModel
@DefaultArgumentInterop.Enabled
constructor(
    public val initialState: NotificationsBetaViewModel.State = NotificationsBetaViewModel.State(),
    public var onDismissBetaDialog: () -> Unit = {},
    public var onDismissBetaToast: () -> Unit = {},
    public var onSetInstanceId: (String?) -> Unit = {},
    public var onSetNotificationsEnabled: (Boolean) -> Unit = {},
    public var onSetSheetRoute: (SheetRoutes?) -> Unit = {},
) : INotificationsBetaViewModel {
    override val models: MutableStateFlow<NotificationsBetaViewModel.State> =
        MutableStateFlow(initialState)

    override fun dismissBetaDialog(): Unit = onDismissBetaDialog()

    override fun dismissBetaToast(): Unit = onDismissBetaToast()

    override fun setInstanceId(instanceId: String?): Unit = onSetInstanceId(instanceId)

    override fun setNotificationsEnabled(enabled: Boolean): Unit =
        onSetNotificationsEnabled(enabled)

    override fun setSheetRoute(sheetRoute: SheetRoutes?): Unit = onSetSheetRoute(sheetRoute)
}
