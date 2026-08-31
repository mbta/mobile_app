package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
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
                    Favorites(serialized.preNotificationsRSDs.associateWith { FavoriteSettings() })
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
public data class RouteStopDirection(
    val route: LineOrRoute.Id,
    val stop: String,
    val direction: Int,
)

@Serializable
public data class FavoriteSettings
@DefaultArgumentInterop.Enabled
constructor(val notifications: Notifications = Notifications.disabled) {
    @Serializable
    public data class Notifications(val enabled: Boolean, val windows: List<Window>) {
        @Serializable
        public data class Window(
            val startTime: LocalTime,
            val endTime: LocalTime,
            val daysOfWeek: Set<DayOfWeek>,
        ) {
            public companion object {

                public val morningDefault: Window =
                    Window(
                        startTime = LocalTime(6, 0),
                        endTime = LocalTime(10, 0),
                        daysOfWeek =
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                    )

                public val middayDefault: Window =
                    Window(
                        startTime = LocalTime(10, 0),
                        endTime = LocalTime(16, 0),
                        daysOfWeek =
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                    )

                public val eveningDefault: Window =
                    Window(
                        startTime = LocalTime(16, 0),
                        endTime = LocalTime(20, 0),
                        daysOfWeek =
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                    )
                // TODO: Handle default that crosses midnight boundary
                public val allDayDefault: Window =
                    Window(
                        startTime = LocalTime(0, 0),
                        endTime = LocalTime(23, 59),
                        daysOfWeek =
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                    )

                public fun defaultFromCurrentTime(now: EasternTimeInstant): Window {
                    val presets =
                        listOf(morningDefault, middayDefault, eveningDefault, allDayDefault)

                    return presets.firstOrNull { now.local.time in it.startTime..it.endTime }
                        ?: allDayDefault
                }

                public fun customFromCurrentTime(now: EasternTimeInstant): Window {
                    val startTime = LocalTime(now.local.time.hour, 0)
                    val endTime =
                        if (startTime.hour == 23) LocalTime(now.local.time.hour, 59)
                        else
                            LocalTime(
                                now.local.time.hour + 1,
                                0,
                            )
                    return Window(
                        startTime = startTime,
                        endTime = endTime,
                        daysOfWeek =
                            setOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                            ),
                    )
                }
            }
        }

        public companion object {
            public val disabled: Notifications =
                Notifications(enabled = false, windows = emptyList())
        }
    }
}
