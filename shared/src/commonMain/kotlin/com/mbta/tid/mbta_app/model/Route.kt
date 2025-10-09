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
public data class Route(
    override val id: Id,
    val type: RouteType,
    val color: String,
    @SerialName("direction_names") val directionNames: List<String?>,
    @SerialName("direction_destinations") val directionDestinations: List<String?>,
    @SerialName("listed_route") val isListedRoute: Boolean = true,
    @SerialName("long_name") val longName: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("text_color") val textColor: String,
    @SerialName("line_id") val lineId: Line.Id? = null,
    @SerialName("route_pattern_ids") val routePatternIds: List<String>? = null,
) : Comparable<Route>, BackendObject<Route.Id> {
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

    override fun compareTo(other: Route): Int = sortOrder.compareTo(other.sortOrder)

    val label: String =
        when (type) {
            RouteType.BUS -> shortName
            RouteType.COMMUTER_RAIL -> longName.replace("/", " / ")
            else -> longName
        }

    internal val isShuttle: Boolean = id.idText.startsWith("Shuttle")
}
