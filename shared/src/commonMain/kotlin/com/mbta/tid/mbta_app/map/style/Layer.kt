package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class Layer : MapboxStyleObject {
    abstract val id: String
    abstract val type: LayerType
    abstract val filter: Exp<Boolean>?
    abstract val source: String?

    var minZoom: Number? = null

    final override fun asJson() = buildJsonObject {
        put("id", id)
        put("type", type)

        filter?.let { put("filter", it) }
        minZoom?.let { put("minzoom", it) }

        put("layout", layoutAsJson())
        put("paint", paintAsJson())

        put("source", source)
    }

    abstract fun layoutAsJson(): JsonElement

    abstract fun paintAsJson(): JsonElement
}
