package com.mbta.tid.mbta_app.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RouteType.Serializer::class)
enum class RouteType {
    TRAM,
    SUBWAY,
    RAIL,
    BUS,
    FERRY;

    object Serializer : KSerializer<RouteType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RouteType", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): RouteType {
            return when (val intType = decoder.decodeInt()) {
                0 -> TRAM
                1 -> SUBWAY
                2 -> RAIL
                3 -> BUS
                4 -> FERRY
                else -> error("Unknown RouteType provided $intType")
            }
        }

        override fun serialize(encoder: Encoder, value: RouteType) {
            val intValue =
                when (value) {
                    TRAM -> 0
                    SUBWAY -> 1
                    RAIL -> 2
                    BUS -> 3
                    FERRY -> 4
                }
            encoder.encodeInt(intValue)
        }
    }
}
