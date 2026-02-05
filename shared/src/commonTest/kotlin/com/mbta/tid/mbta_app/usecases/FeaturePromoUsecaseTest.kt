package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.AppVersion
import com.mbta.tid.mbta_app.model.FeaturePromo
import com.mbta.tid.mbta_app.model.addedInVersion
import com.mbta.tid.mbta_app.repositories.MockCurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.MockLastLaunchedAppVersionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

class FeaturePromoUsecaseTest {
    @Test
    fun `empty when no current version`() = runBlocking {
        val useCase =
            FeaturePromoUseCase(
                MockCurrentAppVersionRepository(null),
                MockLastLaunchedAppVersionRepository(AppVersion(0u, 1u, 0u), onSet = { fail() }),
            )
        assertEquals(emptyList(), useCase.getFeaturePromos())
    }

    @Test
    fun `writes current and returns empty when no last launched version`() = runBlocking {
        var savedVersion: AppVersion? = null
        val useCase =
            FeaturePromoUseCase(
                MockCurrentAppVersionRepository(AppVersion(0u, 1u, 0u)),
                MockLastLaunchedAppVersionRepository(null, onSet = { savedVersion = it }),
            )
        assertEquals(emptyList(), useCase.getFeaturePromos())
        assertEquals(AppVersion(0u, 1u, 0u), savedVersion)
    }

    @Test
    fun `returns new features when version bumped`() = runBlocking {
        // instead of an @Ignore we may forget, skip the test if there are no features
        if (FeaturePromo.entries.all { it.addedInVersion == AppVersion(0u, 0u, 0u) })
            return@runBlocking

        val useCase =
            FeaturePromoUseCase(
                MockCurrentAppVersionRepository(AppVersion(999u, 999u, 999u)),
                MockLastLaunchedAppVersionRepository(AppVersion(0u, 0u, 0u)),
            )

        val expectedPromos =
            FeaturePromo.entries.filter { it.addedInVersion > AppVersion(0u, 0u, 0u) }
        assertEquals(expectedPromos, useCase.getFeaturePromos())
    }

    @Test
    fun `skips promos when too many`() = runBlocking {
        // instead of an @Ignore we may forget, skip the test if there aren't enough features
        if (
            FeaturePromo.entries.filter { it.addedInVersion > AppVersion(0u, 0u, 0u) }.size <=
                FeaturePromoUseCase.MAX_PROMOS
        )
            return@runBlocking
        val useCase =
            FeaturePromoUseCase(
                MockCurrentAppVersionRepository(AppVersion(999u, 999u, 999u)),
                MockLastLaunchedAppVersionRepository(AppVersion(0u, 0u, 0u)),
            )
        assertEquals(emptyList(), useCase.getFeaturePromos())
    }
}
