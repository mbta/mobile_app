package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

/**
 * Represents a
 * [Mapbox Style Spec expression](https://docs.mapbox.com/style-spec/reference/expressions).
 *
 * While we have to bridge [Exp]s between Kotlin and Swift manually, [Exp] must be an interface and
 * not a class. For reasons beyond mortal comprehension, Swift extensions can't define instance
 * methods on generic Objective-C classes, but Objective-C doesn't have generic interfaces, so
 * making [Exp] an interface avoids that restriction. This also makes it much more explicit in the
 * code when a bare [JsonElement] is being inserted directly.
 */
sealed interface Exp<T> : MapboxStyleObject {
    data class Bare<T>(val body: JsonElement) : Exp<T> {
        override fun asJson() = body

        companion object {
            fun arrayOf(vararg elements: String) =
                Bare<List<String>>(
                    buildJsonArray {
                        for (element in elements) {
                            add(element)
                        }
                    }
                )
        }
    }

    companion object {
        private fun <T> op(operator: String, block: JsonArrayBuilder.() -> Unit) =
            Bare<T>(
                buildJsonArray {
                    add(operator)
                    block()
                }
            )

        private fun <T> op(operator: String, vararg operands: Exp<*>): Exp<T> =
            op(operator) { for (operand in operands) add(operand) }

        fun <T> array(
            type: ArrayType<T>? = null,
            size: Number? = null,
            value: Exp<List<T>>
        ): Exp<List<T>> {
            check(!(type == null && size != null)) { "can't specify array size without array type" }
            return op("array") {
                type?.let { add(it) }
                size?.let { add(it) }
                add(value)
            }
        }

        fun boolean(value: Exp<Boolean>): Exp<Boolean> = op("boolean", value)

        fun image(value: Exp<String>): Exp<ResolvedImage> = op("image", value)

        fun <T> literal(value: JsonArray): Exp<List<T>> = op("literal") { add(value) }

        fun number(value: Exp<Number>): Exp<Number> = op("number", value)

        fun string(value: Exp<String>): Exp<String> = op("string", value)

        fun <T> at(index: Exp<Number>, array: Exp<List<T>>): Exp<T> = op("at", index, array)

        fun <T> get(property: Exp<String>, inObject: Exp<JsonObject>? = null): Exp<T> =
            op("get") {
                add(property)
                inObject?.let { add(it) }
            }

        fun has(property: Exp<String>, inObject: Exp<JsonObject>? = null): Exp<Boolean> =
            op("has") {
                add(property)
                inObject?.let { add(it) }
            }

        @JvmName("inArray")
        fun <T> `in`(keyword: Exp<T>, input: Exp<List<T>>): Exp<Boolean> = op("in", keyword, input)

        @JvmName("inString")
        fun `in`(keyword: Exp<String>, input: Exp<String>): Exp<Boolean> = op("in", keyword, input)

        @JvmName("lengthArray")
        fun <T> length(value: Exp<List<T>>): Exp<Number> = op("length", value)

        @JvmName("lengthString") fun length(value: Exp<String>): Exp<Number> = op("length", value)

        fun not(value: Exp<Boolean>): Exp<Boolean> = op("!", value)

        fun <T> notEq(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("!=", lhs, rhs)

        fun <T> lt(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("<", lhs, rhs)

        fun <T> le(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("<=", lhs, rhs)

        fun <T> eq(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("==", lhs, rhs)

        fun <T> gt(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op(">", lhs, rhs)

        fun <T> ge(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op(">=", lhs, rhs)

        fun all(vararg operands: Exp<Boolean>): Exp<Boolean> = op("all", *operands)

        fun any(vararg operands: Exp<Boolean>): Exp<Boolean> = op("any", *operands)

        // vararg is greedy so fallback must be passed as a named parameter
        fun <T> case(vararg cases: Pair<Exp<Boolean>, Exp<T>>, fallback: Exp<T>): Exp<T> =
            op("case") {
                for (case in cases) {
                    add(case.first)
                    add(case.second)
                }
                add(fallback)
            }

        // convenience overload for single case to avoid naming fallback
        fun <T> case(case: Pair<Exp<Boolean>, Exp<T>>, fallback: Exp<T>): Exp<T> =
            op("case", case.first, case.second, fallback)

        // case labels can be either Exp<I> or Exp<List<I>>, but that can't be checked without pain
        fun <I, O> match(
            input: Exp<I>,
            vararg cases: Pair<Exp<*>, Exp<O>>,
            fallback: Exp<O>
        ): Exp<O> =
            op("match") {
                add(input)
                for (case in cases) {
                    add(case.first)
                    add(case.second)
                }
                add(fallback)
            }

        fun <T> interpolate(
            interpolation: Interpolation,
            input: Exp<Number>,
            vararg stops: Pair<Exp<Number>, Exp<T>>
        ): Exp<T> =
            op("interpolate") {
                add(interpolation)
                add(input)
                for (stop in stops) {
                    add(stop.first)
                    add(stop.second)
                }
            }

        fun <T> step(
            input: Exp<Number>,
            outputBelow: Exp<T>,
            vararg stops: Pair<Exp<Number>, Exp<T>>
        ): Exp<T> =
            op("step") {
                add(input)
                add(outputBelow)
                for (stop in stops) {
                    add(stop.first)
                    add(stop.second)
                }
            }

        fun concat(vararg values: Exp<String>): Exp<String> = op("concat", *values)

        fun downcase(value: Exp<String>): Exp<String> = op("downcase", value)

        fun product(vararg values: Exp<Number>): Exp<Number> = op("*", *values)

        fun zoom(): Exp<Number> = op("zoom")
    }
}

fun Exp(value: Boolean): Exp<Boolean> = Exp.Bare(JsonPrimitive(value))

fun Exp(value: Number): Exp<Number> = Exp.Bare(JsonPrimitive(value))

fun Exp(value: String): Exp<String> = Exp.Bare(JsonPrimitive(value))

fun Exp<String>.downcastToColor(): Exp<Color> = Exp.Bare(this.asJson())

fun Exp<String>.downcastToResolvedImage(): Exp<ResolvedImage> = Exp.Bare(this.asJson())
