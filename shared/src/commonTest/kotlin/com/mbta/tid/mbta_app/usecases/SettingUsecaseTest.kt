package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.repositories.ISettingsRepository
import com.mbta.tid.mbta_app.repositories.Setting
import com.mbta.tid.mbta_app.repositories.Settings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SettingUsecaseTest {
    val defaultSettings = Settings.entries.map { Setting(it, false) }.toSet()

    @Test
    fun `can get a setting`() = runBlocking {
        val repo =
            object : ISettingsRepository {
                override suspend fun getSettings() =
                    setOf(Setting(Settings.Map, true), Setting(Settings.SearchRouteResults, false))

                override suspend fun setSettings(settings: Set<Setting>) =
                    throw NotImplementedError()
            }
        val useCase = SettingUsecase(repo)

        assertEquals(true, useCase.get(Settings.Map))
        assertEquals(false, useCase.get(Settings.SearchRouteResults))
    }

    @Test
    fun `settings default to false`() = runBlocking {
        val repo =
            object : ISettingsRepository {
                override suspend fun getSettings() = defaultSettings

                override suspend fun setSettings(settings: Set<Setting>) =
                    throw NotImplementedError()
            }
        val useCase = SettingUsecase(repo)

        for (setting in Settings.entries) {
            assertEquals(false, useCase.get(setting))
        }
    }

    @Test
    fun `can set a setting`() = runBlocking {
        val repo =
            mock<ISettingsRepository>(MockMode.autofill) {
                everySuspend { getSettings() }.returns(defaultSettings)
            }
        val useCase = SettingUsecase(repo)

        for (setting in Settings.entries) {
            useCase.set(setting, true)
            verifySuspend {
                repo.setSettings(defaultSettings - Setting(setting, false) + Setting(setting, true))
            }
        }
    }
}
