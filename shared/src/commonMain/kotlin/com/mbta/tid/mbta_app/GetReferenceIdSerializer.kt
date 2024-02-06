package com.mbta.tid.mbta_app

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object GetReferenceIdSerializer : KSerializer<String> {
    override val descriptor = Reference.serializer().descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val surrogate = Reference("unknown", value)
        encoder.encodeSerializableValue(Reference.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): String {
        val surrogate = decoder.decodeSerializableValue(Reference.serializer())
        return surrogate.id
    }
}

@Serializable data class Reference(val type: String, val id: String)
