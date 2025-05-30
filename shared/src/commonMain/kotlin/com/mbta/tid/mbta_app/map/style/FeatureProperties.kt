package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName

/**
 * [kotlinx.serialization.json.JsonPrimitive] stores values as strings, which makes sense when
 * actually round tripping to JSON but is too expensive if merely passing back and forth in memory.
 * Mapbox for Android uses GSON, which stores primitives as a boxed Java Object, which is nice but a
 * bit of a nuisance to work with in a more type-safe language like Kotlin. Mapbox for iOS defines
 * an enum like this.
 */
sealed interface JSONValue {
    data class Array(val data: JSONArray) : JSONValue

    data class Boolean(val data: kotlin.Boolean) : JSONValue

    data class Number(val data: Double) : JSONValue

    data class Object(val data: JSONObject) : JSONValue

    data class String(val data: kotlin.String) : JSONValue
}

val JSONValue.array: JSONArray
    get() {
        check(this is JSONValue.Array)
        return this.data
    }

val JSONValue.boolean: Boolean
    get() {
        check(this is JSONValue.Boolean)
        return this.data
    }

val JSONValue.number: Number
    get() {
        check(this is JSONValue.Number)
        return this.data
    }

val JSONValue.`object`: JSONObject
    get() {
        check(this is JSONValue.Object)
        return this.data
    }
val JSONValue.string: String
    get() {
        check(this is JSONValue.String)
        return this.data
    }

typealias JSONArray = List<JSONValue>

typealias JSONObject = Map<String, JSONValue>

data class FeatureProperties(val data: JSONObject) {
    operator fun get(property: FeatureProperty<Boolean>): Boolean? = data[property.key]?.boolean

    operator fun get(property: FeatureProperty<Number>): Number? = data[property.key]?.number

    operator fun get(property: FeatureProperty<String>): String? = data[property.key]?.string

    operator fun get(property: FeatureProperty<List<String>>): List<String>? =
        data[property.key]?.array?.map { it.string }

    @JvmName("getMapStringString")
    operator fun get(property: FeatureProperty<Map<String, String>>): Map<String, String>? =
        data[property.key]?.`object`?.mapValues { it.value.string }

    @JvmName("getMapStringListString")
    operator fun get(
        property: FeatureProperty<Map<String, List<String>>>
    ): Map<String, List<String>>? =
        data[property.key]?.`object`?.mapValues { it.value.array.map { it.string } }
}

class FeaturePropertiesBuilder(private val data: MutableMap<String, JSONValue> = mutableMapOf()) {
    fun put(property: FeatureProperty<Boolean>, value: Boolean) {
        data[property.key] = JSONValue.Boolean(value)
    }

    fun put(property: FeatureProperty<Number>, value: Double) {
        data[property.key] = JSONValue.Number(value)
    }

    fun put(property: FeatureProperty<Number>, value: Int) {
        data[property.key] = JSONValue.Number(value.toDouble())
    }

    fun put(property: FeatureProperty<String>, value: String) {
        data[property.key] = JSONValue.String(value)
    }

    fun put(property: FeatureProperty<List<String>>, value: List<String>) {
        data[property.key] = JSONValue.Array(value.map(JSONValue::String))
    }

    @JvmName("putMapStringString")
    fun put(property: FeatureProperty<Map<String, String>>, value: Map<String, String>) {
        data[property.key] = JSONValue.Object(value.mapValues { JSONValue.String(it.value) })
    }

    @JvmName("putMapStringListString")
    fun put(
        property: FeatureProperty<Map<String, List<String>>>,
        value: Map<String, List<String>>,
    ) {
        data[property.key] =
            JSONValue.Object(
                value.mapValues { JSONValue.Array(it.value.map { JSONValue.String(it) }) }
            )
    }

    fun built() = FeatureProperties(data)
}

inline fun buildFeatureProperties(block: FeaturePropertiesBuilder.() -> Unit): FeatureProperties {
    val builder = FeaturePropertiesBuilder()
    builder.block()
    return builder.built()
}
