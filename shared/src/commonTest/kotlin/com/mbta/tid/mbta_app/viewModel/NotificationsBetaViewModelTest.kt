package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.dependencyInjection.MockRepositories
import com.mbta.tid.mbta_app.dependencyInjection.repositoriesModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.repositories.MockOnboardingRepository
import com.mbta.tid.mbta_app.routes.SheetRoutes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
internal class NotificationsBetaViewModelTest : KoinTest {

    private fun setUpKoin(
        coroutineDispatcher: CoroutineDispatcher,
        toastVM: IToastViewModel = MockToastViewModel(),
        repositoriesBlock: MockRepositories.() -> Unit = {},
    ) {
        startKoin {
            modules(
                module {
                    single<CoroutineDispatcher>(named("coroutineDispatcherDefault")) {
                        coroutineDispatcher
                    }
                },
                repositoriesModule(
                    MockRepositories().apply {
                        useObjects(ObjectCollectionBuilder())
                        repositoriesBlock()
                    }
                ),
                viewModelModule(),
                module { single { toastVM }.bind(IToastViewModel::class) },
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun betaDialogIsEnabledThenDismissed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        var dialogDismissed = false
        val onboardingRepository =
            MockOnboardingRepository(
                onNotificationsBetaFeedbackDialogSet = { dialogDismissed = it == false }
            )
        onboardingRepository.notificationsBetaFeedbackDialogShouldShow = true
        onboardingRepository.notificationsBetaPromptShouldShow = true
        val toastVM = MockToastViewModel()

        setUpKoin(dispatcher, toastVM) { onboarding = onboardingRepository }

        val viewModel: NotificationsBetaViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())

            viewModel.setNotificationsEnabled(true)
            assertEquals(NotificationsBetaViewModel.State(false, true), awaitItem())

            viewModel.dismissBetaDialog()
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())

            assertTrue(dialogDismissed)
        }
    }

    @Test
    fun betaPromptIsEnabledThenDismissed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        var promptDismissed = false
        var toastHidden = false

        val onboardingRepository =
            MockOnboardingRepository(
                onNotificationsBetaPromptDismissed = { promptDismissed = true }
            )
        onboardingRepository.notificationsBetaFeedbackDialogShouldShow = true
        onboardingRepository.notificationsBetaPromptShouldShow = true
        val toastVM = MockToastViewModel()
        toastVM.onHideToast = { toastHidden = true }

        setUpKoin(dispatcher, toastVM) { onboarding = onboardingRepository }

        val viewModel: NotificationsBetaViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())

            viewModel.setNotificationsEnabled(false)
            viewModel.setInstanceId("83bdbf80ce1c431cbf42f622ad32a639") // truthy target id
            viewModel.setSheetRoute(SheetRoutes.NearbyTransit)
            assertEquals(NotificationsBetaViewModel.State(true, false), awaitItem())

            viewModel.dismissBetaToast()
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())

            assertTrue(promptDismissed)
            assertTrue(toastHidden)
        }
    }

    @Test
    fun overrideTargeting() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val onboardingRepository = MockOnboardingRepository()
        onboardingRepository.notificationsBetaFeedbackDialogShouldShow = true
        onboardingRepository.notificationsBetaPromptShouldShow = true
        onboardingRepository.notificationsTargetingOverride = true
        val toastVM = MockToastViewModel()

        setUpKoin(dispatcher, toastVM) { onboarding = onboardingRepository }

        val viewModel: NotificationsBetaViewModel = get()

        testViewModelFlow(viewModel).test {
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())

            viewModel.setNotificationsEnabled(false)
            viewModel.setInstanceId("62ba641b3a994558b9c249788c5c2c93") // falsy target id
            viewModel.setSheetRoute(SheetRoutes.NearbyTransit)
            assertEquals(NotificationsBetaViewModel.State(true, false), awaitItem())

            viewModel.dismissBetaToast()
            assertEquals(NotificationsBetaViewModel.State(false, false), awaitItem())
        }
    }
}
