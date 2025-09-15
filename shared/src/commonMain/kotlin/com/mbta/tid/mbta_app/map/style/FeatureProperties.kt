package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName

/**
 * [kotlinx.serialization.json.JsonPrimitive] stores values as strings, which makes sense when
 * actually round tripping to JSON but is too expensive if merely passing back and forth in memory.
 * Mapbox for Android uses GSON, which stores primitives as a boxed Java Object, which is nice but a
 * bit of a nuisance to work with in a more type-safe language like Kotlin. Mapbox for iOS defines
 * an enum like this.
 */
public sealed interface JSONValue {
    public data class Array internal constructor(val data: JSONArray) : JSONValue

    public data class Boolean internal constructor(val data: kotlin.Boolean) : JSONValue

    public data class Number internal constructor(val data: Double) : JSONValue

    public data class Object internal constructor(val data: JSONObject) : JSONValue

    public data class String internal constructor(val data: kotlin.String) : JSONValue
}

internal val JSONValue.array: JSONArray
    get() {
        check(this is JSONValue.Array)
        return this.data
    }

internal val JSONValue.boolean: Boolean
    get() {
        check(this is JSONValue.Boolean)
        return this.data
    }

internal val JSONValue.number: Number
    get() {
        check(this is JSONValue.Number)
        return this.data
    }

internal val JSONValue.`object`: JSONObject
    get() {
        check(this is JSONValue.Object)
        return this.data
    }
internal val JSONValue.string: String
    get() {
        check(this is JSONValue.String)
        return this.data
    }

public typealias JSONArray = List<JSONValue>

public typealias JSONObject = Map<String, JSONValue>

public data class FeatureProperties internal constructor(val data: JSONObject) {
    internal operator fun get(property: FeatureProperty<Boolean>): Boolean? =
        data[property.key]?.boolean

    internal operator fun get(property: FeatureProperty<Number>): Number? =
        data[property.key]?.number

    internal operator fun get(property: FeatureProperty<String>): String? =
        data[property.key]?.string

    internal operator fun get(property: FeatureProperty<List<String>>): List<String>? =
        data[property.key]?.array?.map { it.string }

    @JvmName("getMapStringString")
    internal operator fun get(
        property: FeatureProperty<Map<String, String>>
    ): Map<String, String>? = data[property.key]?.`object`?.mapValues { it.value.string }

    @JvmName("getMapStringListString")
    internal operator fun get(
        property: FeatureProperty<Map<String, List<String>>>
    ): Map<String, List<String>>? =
        data[property.key]?.`object`?.mapValues { it.value.array.map { it.string } }
}

internal class FeaturePropertiesBuilder(
    private val data: MutableMap<String, JSONValue> = mutableMapOf()
) {
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

internal inline fun buildFeatureProperties(
    block: FeaturePropertiesBuilder.() -> Unit
): FeatureProperties {
    val builder = FeaturePropertiesBuilder()
    builder.block()
    return builder.built()
}
