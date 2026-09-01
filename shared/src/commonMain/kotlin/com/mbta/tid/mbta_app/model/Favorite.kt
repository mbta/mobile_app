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
            val id: String
                get() = "$startTime $endTime ${daysOfWeek.joinToString(",")}"

            public companion object {
                public val weekdays: Set<DayOfWeek> =
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                    )

                public val weekend: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

                public fun defaultDaysOfWeek(now: EasternTimeInstant): Set<DayOfWeek> {
                    return if (weekend.contains(now.local.dayOfWeek)) {
                        weekend
                    } else {
                        weekdays
                    }
                }

                public fun morningDefault(daysOfWeek: Set<DayOfWeek>): Window =
                    Window(LocalTime(6, 0), LocalTime(10, 0), daysOfWeek)

                public fun middayDefault(daysOfWeek: Set<DayOfWeek>): Window =
                    Window(LocalTime(10, 0), LocalTime(16, 0), daysOfWeek)

                public fun eveningDefault(daysOfWeek: Set<DayOfWeek>): Window =
                    Window(LocalTime(16, 0), LocalTime(20, 0), daysOfWeek)

                public fun allDayDefault(daysOfWeek: Set<DayOfWeek>): Window =
                    Window(LocalTime(0, 0), LocalTime(23, 59), daysOfWeek)

                public fun defaultFromCurrentTime(now: EasternTimeInstant): Window {
                    val daysOfWeek = defaultDaysOfWeek(now)
                    val presets =
                        listOf(
                            morningDefault(daysOfWeek),
                            middayDefault(daysOfWeek),
                            eveningDefault(daysOfWeek),
                            allDayDefault(daysOfWeek),
                        )

                    return presets.firstOrNull { now.local.time in it.startTime..it.endTime }
                        ?: allDayDefault(daysOfWeek)
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
                        daysOfWeek = defaultDaysOfWeek(now),
                    )
                }

                public fun default(
                    existingWindows: List<Window>,
                    presetsEnabled: Boolean,
                    now: EasternTimeInstant,
                ): Window {

                    if (presetsEnabled) {
                        return defaultFromCurrentTime(now)
                    } else {
                        if (existingWindows.isEmpty()) {
                            return Window(
                                startTime = LocalTime(8, 0, second = 0, nanosecond = 0),
                                endTime = LocalTime(9, 0, second = 0, nanosecond = 0),
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
                        return Window(
                            startTime = LocalTime(12, 0),
                            endTime = LocalTime(13, 0),
                            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        )
                    }
                }

                /**
                 * The earliest possible end time for a given start time - one minute after start.
                 */
                public fun minimumEndTime(startTime: LocalTime): LocalTime {
                    val startHour = startTime.hour
                    val startMinute = startTime.minute
                    if (startHour == 23 && startMinute == 59) {
                        return startTime
                    }
                    if (startMinute < 59) {
                        return LocalTime(hour = startHour, minute = startMinute + 1, second = 0)
                    }
                    return LocalTime(hour = startHour + 1, minute = 0, second = 0)
                }

                /**
                 * Returns a safe end time for a given start time and end time. If the given end
                 * time is before the start time, it pushes the end time out 1 hour.
                 */
                public fun safeEndTime(startTime: LocalTime, endTime: LocalTime): LocalTime {
                    return if (endTime > startTime) {
                        endTime
                    } else {
                        if (startTime.hour < 23) {
                            LocalTime(
                                hour = startTime.hour + 1,
                                minute = startTime.minute,
                                second = 0,
                            )
                        } else {
                            LocalTime(hour = 23, minute = 59, second = 0)
                        }
                    }
                }
            }
        }

        public companion object {
            public val disabled: Notifications =
                Notifications(enabled = false, windows = emptyList())
        }
    }
}
