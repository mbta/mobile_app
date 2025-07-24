package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.FeaturePromo
import com.mbta.tid.mbta_app.repositories.ICurrentAppVersionRepository
import com.mbta.tid.mbta_app.repositories.ILastLaunchedAppVersionRepository
import org.koin.core.component.KoinComponent

class FeaturePromoUseCase(
    private val currentAppVersionRepository: ICurrentAppVersionRepository,
    private val lastLaunchedAppVersionRepository: ILastLaunchedAppVersionRepository,
) : KoinComponent {
    suspend fun getFeaturePromos(): List<FeaturePromo> {
        /*  val currentVersion =
            currentAppVersionRepository.getCurrentAppVersion() ?: return emptyList()
        val lastLaunchedVersion = lastLaunchedAppVersionRepository.getLastLaunchedAppVersion()
        if (lastLaunchedVersion == currentVersion) return emptyList()
        lastLaunchedAppVersionRepository.setLastLaunchedAppVersion(currentVersion)
        if (lastLaunchedVersion == null) return emptyList()
        val newFeatures = FeaturePromo.featuresBetween(lastLaunchedVersion, currentVersion)
        if (newFeatures.size > MAX_PROMOS) return emptyList()*/
        return FeaturePromo.entries
    }

    companion object {
        // https://www.xkcd.com/2615/
        const val MAX_PROMOS = 2
    }
}
