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
                 * Returns a safe end time for a given start time and end time. If the given end
                 * time is before the start time and after the 3am end of service, it's set to the
                 * next 15 minute increment after the start time.
                 */
                @DefaultArgumentInterop.Enabled
                public fun safeEndTime(
                    startTime: LocalTime,
                    endTime: LocalTime,
                    roundUp: Boolean = false,
                ): LocalTime =
                    if (endTime > startTime || endTime <= serviceBoundary) endTime
                    else if (roundUp) serviceBoundary else nextQuarterHour(startTime)

                /** Returns the next 15 minute increment strictly after the given time */
                private fun nextQuarterHour(time: LocalTime): LocalTime {
                    val nextMinute = ((time.minute / 15) + 1) * 15
                    return if (nextMinute == 60) LocalTime(hour = (time.hour + 1) % 24, minute = 0)
                    else LocalTime(hour = time.hour, minute = nextMinute)
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
