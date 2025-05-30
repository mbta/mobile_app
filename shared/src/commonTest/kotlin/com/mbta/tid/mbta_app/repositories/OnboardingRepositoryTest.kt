package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mbta.tid.mbta_app.mocks.MockDatastoreStorage
import com.mbta.tid.mbta_app.model.OnboardingScreen
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class OnboardingRepositoryTest : KoinTest {
    private fun startKoin(
        isScreenReaderEnabled: Boolean = true,
        storage: Storage<Preferences> = MockDatastoreStorage(),
    ) {
        startKoin {
            modules(
                module {
                    single<IAccessibilityStatusRepository> {
                        MockAccessibilityStatusRepository(isScreenReaderEnabled)
                    }
                    single<DataStore<Preferences>> { PreferenceDataStoreFactory.create(storage) }
                }
            )
        }
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `returns all applicable screens when nothing is done`() = runBlocking {
        startKoin(isScreenReaderEnabled = true)
        val repo = OnboardingRepository()
        assertEquals(
            listOf(
                OnboardingScreen.Location,
                OnboardingScreen.StationAccessibility,
                OnboardingScreen.HideMaps,
                OnboardingScreen.Feedback,
            ),
            repo.getPendingOnboarding(),
        )
    }

    @Test
    fun `marks screens as completed`() = runBlocking {
        val storage = MockDatastoreStorage()
        startKoin(isScreenReaderEnabled = true, storage)
        val repo = OnboardingRepository()
        repo.markOnboardingCompleted(OnboardingScreen.Location)
        assertEquals(
            preferencesOf(
                stringSetPreferencesKey("onboardingScreensCompleted") to
                    setOf(OnboardingScreen.Location.name)
            ),
            storage.preferences,
        )
    }

    @Test
    fun `does not return screens that are completed`() = runBlocking {
        val storage = MockDatastoreStorage()
        storage.preferences =
            preferencesOf(
                stringSetPreferencesKey("onboardingScreensCompleted") to
                    setOf(OnboardingScreen.Location.name)
            )
        startKoin(isScreenReaderEnabled = true, storage)
        val repo = OnboardingRepository()
        assertEquals(
            listOf(
                OnboardingScreen.StationAccessibility,
                OnboardingScreen.HideMaps,
                OnboardingScreen.Feedback,
            ),
            repo.getPendingOnboarding(),
        )
    }

    @Test
    fun `does not return hide maps if it does not apply`() = runBlocking {
        startKoin(isScreenReaderEnabled = false)
        val repo = OnboardingRepository()
        assertEquals(
            listOf(
                OnboardingScreen.Location,
                OnboardingScreen.StationAccessibility,
                OnboardingScreen.Feedback,
            ),
            repo.getPendingOnboarding(),
        )
    }
}
