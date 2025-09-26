package com.mbta.tid.mbta_app.android

import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.model.stopDetailsPage.ExplainerType
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A modal bottom sheet which may be open and should cover everything else.
 *
 * Named to rhyme with [Routes] and [SheetRoutes] but does not actually use the Android navigation
 * subsystem.
 */
@Serializable
sealed interface ModalRoutes {
    @Serializable
    data class AlertDetails(
        val alertId: String,
        val lineId: String?,
        val routeIds: List<String>?,
        val stopId: String?,
    ) : ModalRoutes

    @Serializable
    data class Explainer(
        @SerialName("explainerType") val type: ExplainerType,
        val routeAccents: TripRouteAccents,
    ) : ModalRoutes

    @Serializable
    data class SaveFavorite(
        val routeId: String,
        val stopId: String,
        val selectedDirection: Int,
        val context: EditFavoritesContext,
    ) : ModalRoutes
}
