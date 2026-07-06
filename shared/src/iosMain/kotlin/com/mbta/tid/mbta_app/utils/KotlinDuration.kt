package com.mbta.tid.mbta_app.utils

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Durations become opaque Int64s in Swift, so this class represents a Kotlin Duration in a way
 * that’s legible and constructible from Swift
 */
public class KotlinDuration(public val duration: Duration) {
    @DefaultArgumentInterop.Enabled
    public constructor(
        days: Int = 0,
        hours: Int = 0,
        minutes: Int = 0,
        seconds: Int = 0,
    ) : this(days.days + hours.hours + minutes.minutes + seconds.seconds)

    public val inWholeDays: Long = duration.inWholeDays
    public val inWholeHours: Long = duration.inWholeHours
    public val inWholeMicroseconds: Long = duration.inWholeMicroseconds
    public val inWholeMilliseconds: Long = duration.inWholeMilliseconds
    public val inWholeMinutes: Long = duration.inWholeMinutes
    public val inWholeNanoseconds: Long = duration.inWholeNanoseconds
    public val inWholeSeconds: Long = duration.inWholeSeconds

    public companion object {
        public val INFINITE: KotlinDuration = KotlinDuration(Duration.INFINITE)
    }
}

public fun EasternTimeInstant.minus(other: EasternTimeInstant): KotlinDuration =
    KotlinDuration(instant - other.instant)
