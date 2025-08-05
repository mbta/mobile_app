package com.mbta.tid.mbta_app.utils

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = EasternTimeInstant.Serializer::class)
class EasternTimeInstant
private constructor(private val instant: Instant, val local: LocalDateTime) :
    Comparable<EasternTimeInstant> {
    constructor(instant: Instant) : this(instant, instant.toLocalDateTime(timeZone))

    constructor(local: LocalDateTime) : this(local.toInstant(timeZone), local)

    constructor(
        year: Int,
        month: Month,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ) : this(LocalDateTime(year, month, day, hour, minute, second))

    /** The time component becomes irrelevant after this coercion and should be ignored */
    fun coerceInServiceDay(): EasternTimeInstant {
        if (local.date == serviceDate) return this
        val instant = this.instant - 24.hours
        return EasternTimeInstant(instant)
    }

    val serviceDate: LocalDate
        get() = if (local.hour >= 3) local.date else local.date.minus(DatePeriod(days = 1))

    // Service end times will be set to 3:00 in Alerts UI, which means that a LocalDateTime
    // representing the end of service on one date will think that it belongs to the next service
    // day, this allows you to specify if you want to round forward or backward in that case.
    fun serviceDate(rounding: ServiceDateRounding): LocalDate {
        return if (
            rounding == ServiceDateRounding.BACKWARDS && local.hour == 3 && local.minute == 0
        )
            serviceDate.minus(DatePeriod(days = 1))
        else serviceDate
    }

    fun secondsHasDivisor(divisor: Long) = this.instant.epochSeconds % divisor == 0L

    fun toEpochFracSeconds() = this.instant.toEpochMilliseconds() / 1000.0

    operator fun plus(duration: Duration) = EasternTimeInstant(instant + duration)

    operator fun minus(duration: Duration) = EasternTimeInstant(instant - duration)

    operator fun minus(other: EasternTimeInstant) = instant - other.instant

    override fun compareTo(other: EasternTimeInstant): Int {
        return this.instant.compareTo(other.instant)
    }

    override fun equals(other: Any?): Boolean {
        return other is EasternTimeInstant && instant == other.instant
    }

    override fun hashCode(): Int {
        return instant.hashCode()
    }

    override fun toString() = local.toString() + timeZone.offsetAt(instant).toString()

    enum class ServiceDateRounding {
        FORWARDS,
        BACKWARDS,
    }

    object Serializer : KSerializer<EasternTimeInstant> {
        private val delegateSerializer = Instant.serializer()

        override val descriptor =
            SerialDescriptor(
                "com.mbta.tid.mbta_app.utils.EasternTimeInstant",
                delegateSerializer.descriptor,
            )

        override fun serialize(encoder: Encoder, value: EasternTimeInstant) {
            val data = value.instant
            encoder.encodeSerializableValue(delegateSerializer, data)
        }

        override fun deserialize(decoder: Decoder): EasternTimeInstant {
            val data = decoder.decodeSerializableValue(delegateSerializer)
            return EasternTimeInstant(data)
        }
    }

    companion object {
        val timeZone by lazy { TimeZone.Companion.of("America/New_York") }

        @DefaultArgumentInterop.Enabled
        fun now(clock: Clock = Clock.System) = EasternTimeInstant(clock.now())
    }
}
