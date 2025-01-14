package com.mbta.tid.mbta_app.android

import android.os.Bundle
import androidx.navigation.NavType
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
sealed class SheetRoutes {
    @Serializable data object NearbyTransit : SheetRoutes()

    @Serializable
    data class StopDetails(
        val stopId: String,
        val stopFilter: StopDetailsFilter?,
        val tripFilter: TripDetailsFilter?,
    ) : SheetRoutes()
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
