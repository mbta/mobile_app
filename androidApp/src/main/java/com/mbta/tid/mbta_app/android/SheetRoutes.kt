package com.mbta.tid.mbta_app.android

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsFilter
import com.mbta.tid.mbta_app.model.TripDetailsPageFilter
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
            typeOf<TripDetailsPageFilter>() to TripPageFilterParameterType,
        )

fun SheetRoutes.Companion.fromNavBackStackEntry(backStackEntry: NavBackStackEntry): SheetRoutes {
    return if (backStackEntry.destination.route?.contains("EditFavorites") == true) {
        backStackEntry.toRoute<SheetRoutes.EditFavorites>()
    } else if (backStackEntry.destination.route?.contains("Favorites") == true) {
        backStackEntry.toRoute<SheetRoutes.Favorites>()
    } else if (backStackEntry.destination.route?.contains("StopDetails") == true) {
        backStackEntry.toRoute<SheetRoutes.StopDetails>()
    } else if (backStackEntry.destination.route?.contains("RouteDetails") == true) {
        backStackEntry.toRoute<SheetRoutes.RouteDetails>()
    } else if (backStackEntry.destination.route?.contains("RoutePicker") == true) {
        backStackEntry.toRoute<SheetRoutes.RoutePicker>()
    } else if (backStackEntry.destination.route?.contains("TripDetails") == true) {
        backStackEntry.toRoute<SheetRoutes.TripDetails>()
    } else {
        backStackEntry.toRoute<SheetRoutes.NearbyTransit>()
    }
}

// Defining types for type-safe navigation with custom objects
// https://medium.com/@kosta.artur/sending-complex-type-safe-objects-in-compose-navigator-bd161e6adc09
inline fun <reified T> jsonNavType(default: T? = null) =
    object : NavType<T>(isNullableAllowed = null is T) {
        override fun get(bundle: Bundle, key: String): T? =
            bundle.getString(key)?.let { parseValue(it) } ?: default

        override fun put(bundle: Bundle, key: String, value: T) {
            bundle.putString(key, serializeAsValue(value))
        }

        override fun parseValue(value: String): T = json.decodeFromString(value)

        override fun serializeAsValue(value: T): String = json.encodeToString(value)
    }

val RouteDetailsContextParameterType =
    jsonNavType<RouteDetailsContext>(default = RouteDetailsContext.Details)

val RoutePickerPathParameterType = jsonNavType<RoutePickerPath>(default = RoutePickerPath.Root)
val StopFilterParameterType = jsonNavType<StopDetailsFilter?>()
val TripFilterParameterType = jsonNavType<TripDetailsFilter?>()
val TripPageFilterParameterType = jsonNavType<TripDetailsPageFilter>()
