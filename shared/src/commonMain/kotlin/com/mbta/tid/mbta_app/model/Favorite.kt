package com.mbta.tid.mbta_app.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Favorites.Serializer::class)
public data class Favorites(
    val routeStopDirection: Map<RouteStopDirection, FavoriteSettings> = emptyMap()
) {
    @OptIn(ExperimentalObjCName::class)
    public fun isFavorite(@ObjCName(swiftName = "_") rsd: RouteStopDirection): Boolean =
        routeStopDirection.containsKey(rsd)

    internal object Serializer : KSerializer<Favorites> {
        override val descriptor: SerialDescriptor =
            SerialDescriptor(
                "com.mbta.tid.mbta_app.model.Favorites",
                SerializedFavorites.serializer().descriptor,
            )

        override fun serialize(encoder: Encoder, value: Favorites) {
            val serialized =
                SerializedFavorites(
                    preNotificationsRSDs = null,
                    postNotificationsRSDs = value.routeStopDirection.toList(),
                )
            encoder.encodeSerializableValue(SerializedFavorites.serializer(), serialized)
        }

        override fun deserialize(decoder: Decoder): Favorites {
            val serialized = decoder.decodeSerializableValue(SerializedFavorites.serializer())
            return when {
                serialized.postNotificationsRSDs != null ->
                    Favorites(serialized.postNotificationsRSDs.toMap())
                serialized.preNotificationsRSDs != null ->
                    Favorites(
                        serialized.preNotificationsRSDs.associateWith {
                            FavoriteSettings(
                                notifications = FavoriteSettings.Notifications.disabled
                            )
                        }
                    )
                else -> Favorites()
            }
        }
    }

    @Serializable
    private data class SerializedFavorites(
        @SerialName("routeStopDirection") val preNotificationsRSDs: Set<RouteStopDirection>? = null,
        val postNotificationsRSDs: List<Pair<RouteStopDirection, FavoriteSettings>>? = null,
    )
}

@Serializable
public data class RouteStopDirection(val route: String, val stop: String, val direction: Int)

@Serializable
public data class FavoriteSettings(val notifications: Notifications) {
    @Serializable
    public data class Notifications(val enabled: Boolean, val windows: Set<Window>) {
        @Serializable
        public data class Window(
            val startTime: LocalTime,
            val endTime: LocalTime,
            val daysOfWeek: Set<DayOfWeek>,
        )

        public companion object {
            public val disabled: Notifications =
                Notifications(enabled = false, windows = emptySet())
        }
    }
}
