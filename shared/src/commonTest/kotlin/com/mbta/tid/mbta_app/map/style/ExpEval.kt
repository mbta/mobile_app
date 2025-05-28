package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private fun JSONValue.toKotlin(): JsonElement =
    when (this) {
        is JSONValue.Array -> JsonArray(this.data.map { it.toKotlin() })
        is JSONValue.Boolean -> JsonPrimitive(this.data)
        is JSONValue.Number -> JsonPrimitive(this.data)
        is JSONValue.Object -> JsonObject(this.data.mapValues { it.value.toKotlin() })
        is JSONValue.String -> JsonPrimitive(this.data)
    }

private data class EvaluationContext(
    val featureProperties: FeatureProperties,
    val zoom: Double,
    val letBindings: Map<String, JsonElement> = emptyMap(),
) {
    fun evaluate(json: JsonElement): JsonElement {
        return if (json is JsonArray) {
            when (val operator = (json.firstOrNull() as? JsonPrimitive)?.contentOrNull) {
                "array" -> {
                    val type = if (json.size > 2) evaluate(json[1]).jsonPrimitive.content else null
                    val size = if (json.size == 4) evaluate(json[2]).jsonPrimitive.int else null
                    val value = evaluate(json.last()).jsonArray
                    when (type) {
                        "boolean" -> value.forEach { checkNotNull(it.jsonPrimitive.booleanOrNull) }
                        "number" ->
                            value.forEach {
                                checkNotNull(
                                    it.jsonPrimitive.longOrNull ?: it.jsonPrimitive.doubleOrNull
                                )
                            }
                        "string" -> value.forEach { check(it.jsonPrimitive.isString) }
                        null -> {}
                        else -> throw IllegalArgumentException("can’t have array of type $type")
                    }
                    when (size) {
                        null -> {}
                        else -> check(value.size == size)
                    }
                    value
                }
                "boolean" -> JsonPrimitive(evaluate(json[1]).jsonPrimitive.boolean)
                "image" -> JsonPrimitive(evaluate(json[1]).jsonPrimitive.content)
                "literal" -> json[1]
                "number" ->
                    evaluate(json[1]).also {
                        checkNotNull(it.jsonPrimitive.longOrNull ?: it.jsonPrimitive.doubleOrNull)
                    }
                "string" -> JsonPrimitive(evaluate(json[1]).jsonPrimitive.content)
                "at" -> {
                    val index = evaluate(json[1]).jsonPrimitive.int
                    val array = evaluate(json[2]).jsonArray
                    array[index]
                }
                "get" -> {
                    val property = evaluate(json[1]).jsonPrimitive.content
                    if (json.size == 2) {
                        featureProperties.data.getValue(property).toKotlin()
                    } else {
                        evaluate(json[2]).jsonObject.getValue(property)
                    }
                }
                "has" -> {
                    val property = evaluate(json[1]).jsonPrimitive.content
                    if (json.size == 2) {
                        JsonPrimitive(featureProperties.data.containsKey(property))
                    } else {
                        JsonPrimitive(evaluate(json[2]).jsonObject.containsKey(property))
                    }
                }
                "in" -> {
                    val needle = evaluate(json[1])
                    val haystack = evaluate(json[2])
                    if (haystack is JsonArray) {
                        JsonPrimitive(haystack.contains(needle))
                    } else {
                        JsonPrimitive(
                            haystack.jsonPrimitive.content.contains(needle.jsonPrimitive.content)
                        )
                    }
                }
                "length" ->
                    when (val target = evaluate(json[1])) {
                        is JsonArray -> JsonPrimitive(target.size)
                        is JsonPrimitive -> JsonPrimitive(target.content.length)
                        else -> throw IllegalArgumentException("can't take length of $target")
                    }
                "!" -> JsonPrimitive(!evaluate(json[1]).jsonPrimitive.boolean)
                "!=" -> JsonPrimitive(evaluate(json[1]) != evaluate(json[2]))
                "<" -> {
                    val lhs = evaluate(json[1])
                    val rhs = evaluate(json[2])
                    if (lhs.jsonPrimitive.isString) {
                        JsonPrimitive(lhs.jsonPrimitive.content < rhs.jsonPrimitive.content)
                    } else {
                        JsonPrimitive(lhs.jsonPrimitive.double < rhs.jsonPrimitive.double)
                    }
                }
                "<=" -> {
                    val lhs = evaluate(json[1])
                    val rhs = evaluate(json[2])
                    if (lhs.jsonPrimitive.isString) {
                        JsonPrimitive(lhs.jsonPrimitive.content <= rhs.jsonPrimitive.content)
                    } else {
                        JsonPrimitive(lhs.jsonPrimitive.double <= rhs.jsonPrimitive.double)
                    }
                }
                "==" -> JsonPrimitive(evaluate(json[1]) == evaluate(json[2]))
                ">" -> {
                    val lhs = evaluate(json[1])
                    val rhs = evaluate(json[2])
                    if (lhs.jsonPrimitive.isString) {
                        JsonPrimitive(lhs.jsonPrimitive.content > rhs.jsonPrimitive.content)
                    } else {
                        JsonPrimitive(lhs.jsonPrimitive.double > rhs.jsonPrimitive.double)
                    }
                }
                ">=" -> {
                    val lhs = evaluate(json[1])
                    val rhs = evaluate(json[2])
                    if (lhs.jsonPrimitive.isString) {
                        JsonPrimitive(lhs.jsonPrimitive.content >= rhs.jsonPrimitive.content)
                    } else {
                        JsonPrimitive(lhs.jsonPrimitive.double >= rhs.jsonPrimitive.double)
                    }
                }
                "all" -> JsonPrimitive(json.drop(1).all { evaluate(it).jsonPrimitive.boolean })
                "any" -> JsonPrimitive(json.drop(1).any { evaluate(it).jsonPrimitive.boolean })
                "case" -> {
                    for ((condition, output) in
                        json.drop(1).dropLast(1).windowed(size = 2, step = 2)) {
                        if (evaluate(condition).jsonPrimitive.boolean) {
                            return evaluate(output)
                        }
                    }
                    evaluate(json.last())
                }
                "match" -> {
                    val input = evaluate(json[1])
                    for ((case, output) in json.drop(2).dropLast(1).windowed(size = 2, step = 2)) {
                        // case must be literal or list of literals so don’t evaluate
                        val matches = if (case is JsonArray) case.contains(input) else case == input
                        if (matches) {
                            return evaluate(output)
                        }
                    }
                    evaluate(json.last())
                }
                "interpolate" -> {
                    data class Stop(val stop: Double, val value: Double)
                    val interpolationSpec = json[1].jsonArray
                    val interpolationFunction: (Double, Double, Double) -> Double =
                        when (interpolationSpec[0].jsonPrimitive.content) {
                            "linear" -> ({ t, v0, v1 -> v0 * (1 - t) + v1 * t })
                            "exponential" -> {
                                val base = evaluate(interpolationSpec[1]).jsonPrimitive.double
                                { t, v0, v1 ->
                                    // this may not be quite how Mapbox implements it, but if we
                                    // have v = C * base ^ (k * t), then that means
                                    // v0 = C * base ^ 0
                                    // C = v0
                                    // v1 = C * base ^ k
                                    // v1 = v0 * base ^ k
                                    // v1/v0 = base ^ k
                                    // ln (v1/v0) = ln (base ^ k)
                                    // ln v1 - ln v0 = k ln base
                                    // k = (ln v1 - ln v0) / ln base
                                    val k = (ln(v1) - ln(v0)) / ln(base)
                                    v0 * base.pow(k * t)
                                }
                            }
                            else ->
                                throw IllegalArgumentException(
                                    "interpolate with spec $interpolationSpec not implemented in ExpEval"
                                )
                        }
                    val input = evaluate(json[2]).jsonPrimitive.double
                    val stops =
                        json.drop(3).windowed(size = 2, step = 2).map { (stop, outputExp) ->
                            // stop must be literal so don’t evaluate
                            val output =
                                checkNotNull(evaluate(outputExp).jsonPrimitive.doubleOrNull) {
                                    "interpolate with non-number output not implemented in ExpEval"
                                }
                            Stop(stop.jsonPrimitive.double, output)
                        }
                    val stopPairs =
                        (listOf(null) + stops + listOf(null)).windowed(size = 2, step = 1)
                    for ((stopBelow, stopAbove) in stopPairs) {
                        if (
                            input == stopBelow?.stop ||
                                (stopBelow != null && stopAbove == null && input < stopBelow.stop)
                        )
                            return JsonPrimitive(stopBelow.value)
                        if (
                            input == stopAbove?.stop ||
                                (stopAbove != null && stopBelow == null && input > stopAbove.stop)
                        )
                            return JsonPrimitive(stopAbove.value)
                        if (
                            stopBelow != null &&
                                stopAbove != null &&
                                input > stopBelow.stop &&
                                input < stopAbove.stop
                        ) {
                            val t = (input - stopBelow.stop) / (stopAbove.stop - stopBelow.stop)
                            val v0 = stopBelow.value
                            val v1 = stopAbove.value
                            return JsonPrimitive(interpolationFunction(t, v0, v1))
                        }
                    }
                    throw IllegalStateException("interpolate stops not exhaustive somehow")
                }
                "step" -> {
                    val input = evaluate(json[1]).jsonPrimitive.double
                    var previousOutput = evaluate(json[2])
                    for ((stopInput, stopOutput) in json.drop(3).windowed(size = 2, step = 2)) {
                        if (input < evaluate(stopInput).jsonPrimitive.double) break
                        previousOutput = evaluate(stopOutput)
                    }
                    previousOutput
                }
                "let" -> {
                    val bindings = this.letBindings.toMutableMap()
                    for ((nameExp, valueExp) in
                        json.drop(1).dropLast(1).windowed(size = 2, step = 2)) {
                        val name = evaluate(nameExp)
                        val value = evaluate(valueExp)
                        bindings[name.jsonPrimitive.content] = value
                    }
                    val childContext = this.copy(letBindings = bindings)
                    childContext.evaluate(json.last())
                }
                "var" -> {
                    val name = evaluate(json[1])
                    letBindings.getValue(name.jsonPrimitive.content)
                }
                "concat" ->
                    JsonPrimitive(
                        buildString {
                            for (input in json.drop(1)) {
                                append(evaluate(input).jsonPrimitive.content)
                            }
                        }
                    )
                "downcase" -> JsonPrimitive(evaluate(json[1]).jsonPrimitive.content.lowercase())
                "*" ->
                    JsonPrimitive(
                        json.drop(1).map { evaluate(it).jsonPrimitive.double }.reduce(Double::times)
                    )
                "zoom" -> JsonPrimitive(zoom)
                "collator",
                "format",
                "number-format",
                "object",
                "to-boolean",
                "to-color",
                "to-number",
                "to-string",
                "typeof",
                "accumulated",
                "feature-state",
                "geometry-type",
                "id",
                "line-progress",
                "properties",
                "config",
                "index-of",
                "measure-light",
                "slice",
                "coalesce",
                "within",
                "interpolate-hcl",
                "interpolate-lab",
                "is-supported-script",
                "resolved-locale",
                "upcase",
                "hsl",
                "hsla",
                "rgb",
                "rgba",
                "to-rgba",
                "-",
                "/",
                "%",
                "^",
                "+",
                "abs",
                "acos",
                "asin",
                "atan",
                "ceil",
                "cos",
                "distance",
                "e",
                "floor",
                "ln",
                "ln2",
                "log10",
                "log2",
                "max",
                "min",
                "pi",
                "random",
                "round",
                "sin",
                "sqrt",
                "tan",
                "distance-from-center",
                "pitch",
                "heatmap-density" ->
                    throw NotImplementedError("operation $operator not implemented in ExpEval")
                else -> json
            }
        } else {
            json
        }
    }
}

@JvmName("evaluateString")
fun Exp<Boolean>.evaluate(featureProperties: FeatureProperties, zoom: Double): Boolean {
    val result = EvaluationContext(featureProperties, zoom).evaluate(this.asJson())
    return result.jsonPrimitive.boolean
}

@JvmName("evaluateResolvedImage")
fun Exp<ResolvedImage>.evaluate(featureProperties: FeatureProperties, zoom: Double): String {
    val result = EvaluationContext(featureProperties, zoom).evaluate(this.asJson())
    return result.jsonPrimitive.content
}

@JvmName("evaluateString")
fun Exp<String>.evaluate(featureProperties: FeatureProperties, zoom: Double): String {
    val result = EvaluationContext(featureProperties, zoom).evaluate(this.asJson())
    return result.jsonPrimitive.content
}
