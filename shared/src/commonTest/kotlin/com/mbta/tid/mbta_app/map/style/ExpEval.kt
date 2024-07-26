package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun JSONValue.toKotlin(): JsonElement =
    when (this) {
        is JSONValue.Array -> JsonArray(this.data.map { it.toKotlin() })
        is JSONValue.Boolean -> JsonPrimitive(this.data)
        is JSONValue.Number -> JsonPrimitive(this.data)
        is JSONValue.Object -> JsonObject(this.data.mapValues { it.value.toKotlin() })
        is JSONValue.String -> JsonPrimitive(this.data)
    }

private data class EvaluationContext(val featureProperties: FeatureProperties, val zoom: Double) {
    fun evaluate(json: JsonElement): JsonElement {
        return if (json is JsonArray) {
            when (val operator = (json.firstOrNull() as? JsonPrimitive)?.contentOrNull) {
                "boolean" -> JsonPrimitive(evaluate(json[1]).jsonPrimitive.boolean)
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
                "length" ->
                    when (val target = evaluate(json[1])) {
                        is JsonArray -> JsonPrimitive(target.size)
                        is JsonPrimitive -> JsonPrimitive(target.content.length)
                        else -> throw IllegalArgumentException("can't take length of $target")
                    }
                "!" -> JsonPrimitive(!evaluate(json[1]).jsonPrimitive.boolean)
                "==" -> JsonPrimitive(evaluate(json[1]) == evaluate(json[2]))
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
                "step" -> {
                    val input = evaluate(json[1]).jsonPrimitive.double
                    var previousOutput = evaluate(json[2])
                    for ((stopInput, stopOutput) in json.drop(3).windowed(size = 2, step = 2)) {
                        if (input < evaluate(stopInput).jsonPrimitive.double) break
                        previousOutput = evaluate(stopOutput)
                    }
                    return previousOutput
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
                "zoom" -> JsonPrimitive(zoom)
                "array",
                "collator",
                "format",
                "image",
                "literal",
                "number",
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
                "in",
                "index-of",
                "measure-light",
                "slice",
                "!=",
                "<",
                "<=",
                ">",
                "coalesce",
                "match",
                "within",
                "interpolate",
                "interpolate-hcl",
                "interpolate-lab",
                "let",
                "var",
                "downcase",
                "is-supported-script",
                "resolved-locale",
                "upcase",
                "hsl",
                "hsla",
                "rgb",
                "rgba",
                "to-rgba",
                "-",
                "*",
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
