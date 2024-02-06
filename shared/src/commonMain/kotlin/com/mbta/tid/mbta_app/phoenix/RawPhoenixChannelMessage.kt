package com.mbta.tid.mbta_app.phoenix

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable(with = RawPhoenixChannelMessage.Serializer::class)
data class RawPhoenixChannelMessage(
    val joinRef: String?,
    val ref: String?,
    val topic: String,
    val event: String,
    val payload: JsonObject = JsonObject(emptyMap())
) {
    @OptIn(ExperimentalSerializationApi::class)
    object Serializer : KSerializer<RawPhoenixChannelMessage> {
        override val descriptor = listSerialDescriptor<JsonElement>()

        override fun serialize(encoder: Encoder, value: RawPhoenixChannelMessage) =
            encoder.encodeCollection(descriptor, 5) {
                encodeNullableSerializableElement(descriptor, 0, String.serializer(), value.joinRef)
                encodeNullableSerializableElement(descriptor, 1, String.serializer(), value.ref)
                encodeStringElement(descriptor, 2, value.topic)
                encodeStringElement(descriptor, 3, value.event)
                encodeSerializableElement(descriptor, 4, JsonObject.serializer(), value.payload)
            }

        override fun deserialize(decoder: Decoder): RawPhoenixChannelMessage =
            decoder.decodeStructure(descriptor) {
                var joinRef: String? = null
                var ref: String? = null
                var topic: String? = null
                var event: String? = null
                var payload: JsonObject? = null
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 ->
                            joinRef =
                                decodeNullableSerializableElement(
                                    descriptor,
                                    0,
                                    String.serializer(),
                                    joinRef
                                )
                        1 ->
                            ref =
                                decodeNullableSerializableElement(
                                    descriptor,
                                    1,
                                    String.serializer(),
                                    ref
                                )
                        2 -> topic = decodeStringElement(descriptor, 2)
                        3 -> event = decodeStringElement(descriptor, 3)
                        4 ->
                            payload =
                                decodeSerializableElement(
                                    descriptor,
                                    4,
                                    JsonObject.serializer(),
                                    payload
                                )
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                requireNotNull(topic)
                requireNotNull(event)
                requireNotNull(payload)
                RawPhoenixChannelMessage(joinRef, ref, topic, event, payload)
            }
    }
}
