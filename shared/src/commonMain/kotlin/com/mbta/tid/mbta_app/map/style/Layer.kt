package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public sealed class Layer : MapboxStyleObject {
    public abstract val id: String
    public abstract val type: LayerType
    public abstract val filter: Exp<Boolean>?
    public abstract val source: String?

    public var minZoom: Double? = null

    final override fun asJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("type", type)

        filter?.let { put("filter", it) }
        minZoom?.let { put("minzoom", it) }

        put("layout", layoutAsJson())
        put("paint", paintAsJson())

        put("source", source)
    }

    internal abstract fun layoutAsJson(): JsonElement

    internal abstract fun paintAsJson(): JsonElement
}
