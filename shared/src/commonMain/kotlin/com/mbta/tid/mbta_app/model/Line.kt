package com.mbta.tid.mbta_app.model

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
public data class Line(
    override val id: Id,
    val color: String,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
) : BackendObject<Line.Id> {
    @Serializable(with = Id.Serializer::class)
    public data class Id
    @OptIn(ExperimentalObjCName::class)
    constructor(@param:ObjCName(swiftName = "_") override val idText: String) : LineOrRoute.Id() {
        override fun toString(): String = idText

        internal object Serializer : KSerializer<Id> {
            override val descriptor =
                PrimitiveSerialDescriptor(Id::class.qualifiedName!!, PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: Id) = encoder.encodeString(value.idText)

            override fun deserialize(decoder: Decoder): Id = Id(decoder.decodeString())
        }
    }

    /** Grouped lines are displayed as though they are different branches of a single route. */
    val isGrouped: Boolean = this.id in groupedIds

    internal companion object {
        val groupedIds = setOf(Id("line-Green"))
    }
}
