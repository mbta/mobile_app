package com.mapbox.maps.extension.style.layers

import com.mapbox.bindgen.Value
import com.mbta.tid.mbta_app.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

fun Layer.getCachedLayerProperties(): JsonObject {
    val method = Layer::class.java.getDeclaredMethod("getCachedLayerProperties")
    method.isAccessible = true
    val data: Value = method.invoke(this) as Value
    return json.parseToJsonElement(data.toJson()).jsonObject
}
