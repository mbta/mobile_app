package com.mbta.tid.mbta_app.android.util

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.compose.ui.graphics.Color", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Color) {
        val rawValue = value.value.toLong()
        encoder.encodeLong(rawValue)
    }

    override fun deserialize(decoder: Decoder): Color {
        val rawValue = decoder.decodeLong()
        return Color(rawValue.toULong())
    }
}
