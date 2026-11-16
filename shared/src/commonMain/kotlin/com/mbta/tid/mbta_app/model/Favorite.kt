package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Favorites.Serializer::class)
public data class Favorites(
    val routeStopDirection: Map<RouteStopDirection, FavoriteSettings> = emptyMap(),
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
        public data class Window
        private constructor(
            @Transient val id: String = getId(),
            val startTime: LocalTime,
            val endTime: LocalTime,
            val daysOfWeek: Set<DayOfWeek>,
        ) {
            public constructor(
                startTime: LocalTime,
                endTime: LocalTime,
                daysOfWeek: Set<DayOfWeek>,
            ) : this(getId(), startTime, endTime, daysOfWeek)

            public constructor(
                preset: Preset,
                daysOfWeek: Set<DayOfWeek>,
            ) : this(preset.startTime, preset.endTime, daysOfWeek)

            public sealed class Type {
                public object Basic : Type()

                public object NextDay : Type()

                public object ServiceEnd : Type()

                public object ServiceStart : Type()
            }

            val startType: Type
                get() =
                    when {
                        startTime == serviceBoundary -> Type.ServiceStart
                        else -> Type.Basic
                    }

            val endType: Type
                get() =
                    when {
                        endTime == serviceBoundary -> Type.ServiceEnd
                        endTime <= startTime -> Type.NextDay
                        else -> Type.Basic
                    }

            // Copy function to preserve ID without making it modifiable
            @DefaultArgumentInterop.Enabled
            public fun copy(
                startTime: LocalTime = this.startTime,
                endTime: LocalTime = this.endTime,
                daysOfWeek: Set<DayOfWeek> = this.daysOfWeek,
            ): Window {
                return Window(this.id, startTime, endTime, daysOfWeek)
            }

            public companion object {
                @OptIn(ExperimentalUuidApi::class)
                private fun getId(): String = Uuid.random().toString()

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

                public fun defaultFromCurrentTime(now: EasternTimeInstant): Window {
                    val daysOfWeek = defaultDaysOfWeek(now)
                    val presets =
                        listOf(
                            Window(Preset.Morning, daysOfWeek),
                            Window(Preset.Midday, daysOfWeek),
                            Window(Preset.Evening, daysOfWeek),
                            Window(Preset.AllDay, daysOfWeek),
                        )

                    return presets.firstOrNull { now.local.time in it.startTime..it.endTime }
                        ?: Window(Preset.AllDay, daysOfWeek)
                }

                public fun customFromCurrentTime(now: EasternTimeInstant): Window {
                    val startTime = LocalTime(now.local.time.hour, 0)
                    val endTime =
                        if (startTime.hour == 23) LocalTime(1, 0)
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
                    now: EasternTimeInstant,
                ): Window {
                    return if (existingWindows.isEmpty()) {
                        defaultFromCurrentTime(now)
                    } else {
                        defaultFromCurrentTime(now).copy(daysOfWeek = weekend)
                    }
                }

                private const val MINIMUM_DURATION_MINUTES = 15
                private const val MINUTES_PER_DAY = 24 * 60

                private val LocalTime.minuteOfDay: Int
                    get() = hour * 60 + minute

                private fun localTime(minuteOfDay: Int): LocalTime {
                    val wrapped = minuteOfDay.mod(MINUTES_PER_DAY)
                    return LocalTime(wrapped / 60, wrapped % 60)
                }

                /**
                 * Returns the end time to use when the rider selects [endTime], keeping the window
                 * valid. Times before the 3am [serviceBoundary] are "after midnight", times after
                 * it are "during the day". An after midnight window can't cross the boundary, and
                 * otherwise the end must be at least 15 minutes after the start.
                 */
                public fun adjustedEndTime(startTime: LocalTime, endTime: LocalTime): LocalTime {
                    if (startTime == serviceBoundary || endTime == serviceBoundary) return endTime
                    val start = startTime.minuteOfDay
                    val end = endTime.minuteOfDay
                    return when {
                        startTime < serviceBoundary && endTime > serviceBoundary -> serviceBoundary
                        startTime > serviceBoundary && endTime < serviceBoundary -> endTime
                        end - start >= MINIMUM_DURATION_MINUTES -> endTime
                        else -> localTime(start + MINIMUM_DURATION_MINUTES)
                    }
                }

                /**
                 * Returns the start time to use when the rider selects [startTime], keeping the
                 * window valid. A daytime window can't start after midnight, and otherwise the
                 * start must be at least 15 minutes before the end.
                 */
                public fun adjustedStartTime(startTime: LocalTime, endTime: LocalTime): LocalTime {
                    if (startTime == serviceBoundary || endTime == serviceBoundary) return startTime
                    val start = startTime.minuteOfDay
                    val end = endTime.minuteOfDay
                    return when {
                        startTime < serviceBoundary && endTime > serviceBoundary -> serviceBoundary
                        startTime > serviceBoundary && endTime < serviceBoundary -> startTime
                        end - start >= MINIMUM_DURATION_MINUTES -> startTime
                        else -> localTime(end - MINIMUM_DURATION_MINUTES)
                    }
                }
            }
        }

        public companion object {
            public val disabled: Notifications =
                Notifications(enabled = false, windows = emptyList())

            public val serviceBoundary: LocalTime = LocalTime(3, 0, second = 0, nanosecond = 0)
        }
    }
}
