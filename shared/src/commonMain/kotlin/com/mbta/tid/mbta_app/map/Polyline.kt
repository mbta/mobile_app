package com.mbta.tid.mbta_app.map

import io.github.dellisd.spatialk.geojson.Position
import kotlin.experimental.and
import kotlin.math.round

// https://developers.google.com/maps/documentation/utilities/polylinealgorithm
internal object Polyline {
    private sealed interface State {
        data object AwaitingLatitude : State

        data class AwaitingLongitude(val latitude: Double) : State
    }

    fun decode(data: String): List<Position> {
        val result = mutableListOf<Position>()
        var lastPoint = Position(latitude = 0.0, longitude = 0.0)
        val thisValueChunks = mutableListOf<Byte>()
        var state: State = State.AwaitingLatitude
        for (thisChar in data) {
            val thisByte = (thisChar.code - 63).toByte()
            // bits 0-4 contain actual data
            thisValueChunks.add(thisByte and 0x1F)
            // bit 5 is 0 if value is over
            if ((thisByte and 0x20) == 0.toByte()) {
                val thisValue = decodeValue(thisValueChunks)
                thisValueChunks.clear()
                when (state) {
                    State.AwaitingLatitude -> {
                        state = State.AwaitingLongitude(latitude = thisValue)
                    }
                    is State.AwaitingLongitude -> {
                        val thisPoint =
                            Position(
                                latitude = lastPoint.latitude + state.latitude,
                                longitude = lastPoint.longitude + thisValue,
                            )
                        result.add(thisPoint)
                        lastPoint = thisPoint
                        state = State.AwaitingLatitude
                    }
                }
            }
        }
        return result
    }

    private fun decodeValue(chunks: List<Byte>): Double {
        var thisValue = 0u
        for (chunk in chunks.asReversed()) {
            thisValue = (thisValue shl 5) or chunk.toUInt()
        }
        // since encoding will shl 1 and then invert if negative, trailing bit is sign bit
        val isNegative = (thisValue and 0x1u) > 0u
        if (isNegative) {
            thisValue = thisValue.inv()
        }
        // this shr will sign-extend if thisValue is a signed Int, which is why it's unsigned
        thisValue = (thisValue shr 1) and ((1u shl 31) - 1u)
        if (isNegative) thisValue = thisValue or (1u.rotateRight(1))
        return round(thisValue.toInt().toDouble()) / 1e5
    }
}
