package com.mbta.tid.mbta_app.android

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

@Serializable
sealed class SheetRoutes {

    @Serializable sealed interface Entrypoint

    @Serializable data object Favorites : SheetRoutes(), Entrypoint

    @Serializable data object NearbyTransit : SheetRoutes(), Entrypoint

    @Serializable
    data class StopDetails(
        val stopId: String,
        val stopFilter: StopDetailsFilter?,
        val tripFilter: TripDetailsFilter?,
    ) : SheetRoutes()

    val showSearchBar: Boolean
        get() =
            when (this) {
                is Favorites -> true
                is NearbyTransit -> true
                else -> false
            }

    companion object {
        val EntrypointSaver =
            Saver<Entrypoint, String>(
                save = { json.encodeToString<Entrypoint>(it) },
                restore = { json.decodeFromString<Entrypoint>(it) },
            )

        val typeMap =
            mapOf(
                typeOf<StopDetailsFilter?>() to StopFilterParameterType,
                typeOf<TripDetailsFilter?>() to TripFilterParameterType,
            )

        /**
         * Whether the page within the nearby transit tab changed. Moving from StopDetails to
         * StopDetails is only considered a page change if the stopId changed.
         */
        fun pageChanged(first: SheetRoutes?, second: SheetRoutes?): Boolean {
            return if (first is StopDetails && second is StopDetails) {
                first.stopId != second.stopId
            } else {
                first != second
            }
        }

        fun fromNavBackStackEntry(backStackEntry: NavBackStackEntry): SheetRoutes {
            return if (backStackEntry.destination.route?.contains("StopDetails") == true) {
                backStackEntry.toRoute<StopDetails>()
            } else if (backStackEntry.destination.route?.contains("Favorites") == true) {
                backStackEntry.toRoute<Favorites>()
            } else {
                backStackEntry.toRoute<NearbyTransit>()
            }
        }
    }
}

// Defining types for type-safe navigation with custom objects
// https://medium.com/@kosta.artur/sending-complex-type-safe-objects-in-compose-navigator-bd161e6adc09
val StopFilterParameterType =
    object : NavType<StopDetailsFilter?>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): StopDetailsFilter? =
            bundle.getString(key)?.let { parseValue(it) }

        override fun put(bundle: Bundle, key: String, value: StopDetailsFilter?) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): StopDetailsFilter? = json.decodeFromString(value)

        override fun serializeAsValue(value: StopDetailsFilter?): String =
            json.encodeToString(value)
    }

val TripFilterParameterType =
    object : NavType<TripDetailsFilter?>(isNullableAllowed = true) {
        override fun get(bundle: Bundle, key: String): TripDetailsFilter? =
            bundle.getString(key)?.let { parseValue(it) }

        override fun put(bundle: Bundle, key: String, value: TripDetailsFilter?) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): TripDetailsFilter? = json.decodeFromString(value)

        override fun serializeAsValue(value: TripDetailsFilter?): String =
            json.encodeToString(value)
    }
