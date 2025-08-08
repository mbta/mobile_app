package com.mbta.tid.mbta_app.cache

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * This is used by the file caching to store the timestamps of cached data as JSON on disk, so that
 * the staleness of the data can be determined on load. However, doing so loses the benefits of
 * using monotonic time marks, since those can't reasonably be serialized or deserialized as is.
 * There isn't any good way to do this serialization that doesn't have the same issues that using
 * Instants would, which is that they're influenced by changes to the system clock.
 *
 * This could cause problems if the system clock is set into the past between persisting and loading
 * from disk, since then the data won't become stale for a longer duration than the cache expects.
 */
internal object TimeMarkSerializer : KSerializer<TimeSource.Monotonic.ValueTimeMark> {
    override val descriptor: SerialDescriptor
        get() =
            PrimitiveSerialDescriptor("TimeSource.Monotonic.ValueTimeMark", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TimeSource.Monotonic.ValueTimeMark {
        val instant = Instant.parse(decoder.decodeString())
        val now = Clock.System.now()
        return TimeSource.Monotonic.markNow().minus(now.minus(other = instant))
    }

    override fun serialize(encoder: Encoder, value: TimeSource.Monotonic.ValueTimeMark) {
        val elapsed = value.elapsedNow()
        val now = Clock.System.now()
        encoder.encodeString(now.minus(duration = elapsed).toString())
    }
}
