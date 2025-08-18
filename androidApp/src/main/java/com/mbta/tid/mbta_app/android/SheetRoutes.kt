package com.mbta.tid.mbta_app.android

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.routes.SheetRoutes
import kotlin.reflect.typeOf

val SheetRoutes.Companion.EntrypointSaver
    get() =
        Saver<SheetRoutes.Entrypoint?, String>(
            save = { json.encodeToString<SheetRoutes.Entrypoint?>(it) },
            restore = { json.decodeFromString<SheetRoutes.Entrypoint?>(it) },
        )

val SheetRoutes.Companion.typeMap
    get() =
        mapOf(
            typeOf<RouteDetailsContext>() to RouteDetailsContextParameterType,
            typeOf<RoutePickerPath>() to RoutePickerPathParameterType,
            typeOf<StopDetailsFilter?>() to StopFilterParameterType,
            typeOf<TripDetailsFilter?>() to TripFilterParameterType,
        )

fun SheetRoutes.Companion.fromNavBackStackEntry(backStackEntry: NavBackStackEntry): SheetRoutes {
    return if (backStackEntry.destination.route?.contains("StopDetails") == true) {
        backStackEntry.toRoute<SheetRoutes.StopDetails>()
    } else if (backStackEntry.destination.route?.contains("RouteDetails") == true) {
        backStackEntry.toRoute<SheetRoutes.RouteDetails>()
    } else if (backStackEntry.destination.route?.contains("RoutePicker") == true) {
        backStackEntry.toRoute<SheetRoutes.RoutePicker>()
    } else if (backStackEntry.destination.route?.contains("EditFavorites") == true) {
        backStackEntry.toRoute<SheetRoutes.EditFavorites>()
    } else if (backStackEntry.destination.route?.contains("Favorites") == true) {
        backStackEntry.toRoute<SheetRoutes.Favorites>()
    } else {
        backStackEntry.toRoute<SheetRoutes.NearbyTransit>()
    }
}

// Defining types for type-safe navigation with custom objects
// https://medium.com/@kosta.artur/sending-complex-type-safe-objects-in-compose-navigator-bd161e6adc09
val RouteDetailsContextParameterType =
    object : NavType<RouteDetailsContext>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): RouteDetailsContext =
            bundle.getString(key)?.let { parseValue(it) } ?: RouteDetailsContext.Details

        override fun put(bundle: Bundle, key: String, value: RouteDetailsContext) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): RouteDetailsContext = json.decodeFromString(value)

        override fun serializeAsValue(value: RouteDetailsContext): String =
            json.encodeToString(value)
    }

val RoutePickerPathParameterType =
    object : NavType<RoutePickerPath>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): RoutePickerPath =
            bundle.getString(key)?.let { parseValue(it) } ?: RoutePickerPath.Root

        override fun put(bundle: Bundle, key: String, value: RoutePickerPath) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): RoutePickerPath = json.decodeFromString(value)

        override fun serializeAsValue(value: RoutePickerPath): String = json.encodeToString(value)
    }

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
