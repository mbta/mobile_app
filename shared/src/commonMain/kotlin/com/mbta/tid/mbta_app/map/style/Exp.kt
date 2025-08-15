package com.mbta.tid.mbta_app.map.style

import kotlin.jvm.JvmName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
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
public sealed interface Exp<T> : MapboxStyleObject {
    public data class Bare<T> internal constructor(internal val body: JsonElement) : Exp<T> {
        override fun asJson(): JsonElement = body

        internal companion object {
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

    public companion object {
        private fun <T> op(operator: String, block: JsonArrayBuilder.() -> Unit) =
            Bare<T>(
                buildJsonArray {
                    add(operator)
                    block()
                }
            )

        private fun <T> op(operator: String, vararg operands: Exp<*>): Exp<T> =
            op(operator) { for (operand in operands) add(operand) }

        internal fun <T> array(
            type: ArrayType<T>? = null,
            size: Number? = null,
            value: Exp<List<T>>,
        ): Exp<List<T>> {
            check(!(type == null && size != null)) { "can't specify array size without array type" }
            return op("array") {
                type?.let { add(it) }
                size?.let { add(it) }
                add(value)
            }
        }

        internal fun boolean(value: Exp<Boolean>): Exp<Boolean> = op("boolean", value)

        internal fun image(value: Exp<String>): Exp<ResolvedImage> = op("image", value)

        internal fun <T> literal(value: JsonArray): Exp<List<T>> = op("literal") { add(value) }

        internal fun number(value: Exp<Number>): Exp<Number> = op("number", value)

        internal fun string(value: Exp<String>): Exp<String> = op("string", value)

        internal fun <T> at(index: Exp<Number>, array: Exp<List<T>>): Exp<T> =
            op("at", index, array)

        internal fun <T> get(property: FeatureProperty<T>): Exp<T> = op("get") { add(property.key) }

        internal fun <K, V> get(property: Exp<K>, inObject: Exp<Map<K, V>>): Exp<V> =
            op("get") {
                add(property)
                add(inObject)
            }

        internal fun has(property: FeatureProperty<*>): Exp<Boolean> =
            op("has") { add(property.key) }

        internal fun <K, V> has(property: Exp<K>, inObject: Exp<Map<K, V>>): Exp<Boolean> =
            op("has") {
                add(property)
                add(inObject)
            }

        @JvmName("inArray")
        internal fun <T> `in`(keyword: Exp<T>, input: Exp<List<T>>): Exp<Boolean> =
            op("in", keyword, input)

        @JvmName("inString")
        internal fun `in`(keyword: Exp<String>, input: Exp<String>): Exp<Boolean> =
            op("in", keyword, input)

        @JvmName("lengthArray")
        internal fun <T> length(value: Exp<List<T>>): Exp<Number> = op("length", value)

        @JvmName("lengthString")
        internal fun length(value: Exp<String>): Exp<Number> = op("length", value)

        internal fun not(value: Exp<Boolean>): Exp<Boolean> = op("!", value)

        internal fun <T> notEq(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("!=", lhs, rhs)

        internal fun <T> lt(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("<", lhs, rhs)

        internal fun <T> le(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("<=", lhs, rhs)

        internal fun <T> eq(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op("==", lhs, rhs)

        internal fun <T> gt(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op(">", lhs, rhs)

        internal fun <T> ge(lhs: Exp<T>, rhs: Exp<T>): Exp<Boolean> = op(">=", lhs, rhs)

        internal fun all(vararg operands: Exp<Boolean>): Exp<Boolean> = op("all", *operands)

        internal fun any(vararg operands: Exp<Boolean>): Exp<Boolean> = op("any", *operands)

        // vararg is greedy so fallback must be passed as a named parameter
        internal fun <T> case(vararg cases: Pair<Exp<Boolean>, Exp<T>>, fallback: Exp<T>): Exp<T> =
            op("case") {
                for (case in cases) {
                    add(case.first)
                    add(case.second)
                }
                add(fallback)
            }

        // convenience overload for single case to avoid naming fallback
        internal fun <T> case(case: Pair<Exp<Boolean>, Exp<T>>, fallback: Exp<T>): Exp<T> =
            op("case", case.first, case.second, fallback)

        // case labels can be either Exp<I> or Exp<List<I>>, but that can't be checked without pain
        internal fun <I, O> match(
            input: Exp<I>,
            vararg cases: Pair<Exp<*>, Exp<O>>,
            fallback: Exp<O>,
        ): Exp<O> =
            op("match") {
                add(input)
                for (case in cases) {
                    add(case.first)
                    add(case.second)
                }
                add(fallback)
            }

        internal fun <T> interpolate(
            interpolation: Interpolation,
            input: Exp<Number>,
            vararg stops: Pair<Exp<Number>, Exp<T>>,
        ): Exp<T> =
            op("interpolate") {
                add(interpolation)
                add(input)
                for (stop in stops) {
                    add(stop.first)
                    add(stop.second)
                }
            }

        internal fun <T> step(
            input: Exp<Number>,
            outputBelow: Exp<T>,
            vararg stops: Pair<Exp<Number>, Exp<T>>,
        ): Exp<T> =
            op("step") {
                add(input)
                add(outputBelow)
                for (stop in stops) {
                    add(stop.first)
                    add(stop.second)
                }
            }

        internal fun <T> let(vararg bindings: LetVariable.Binding<*>, body: Exp<T>): Exp<T> =
            op("let") {
                for (binding in bindings) {
                    add(binding.variable.name)
                    add(binding.value)
                }
                add(body)
            }

        internal fun <T> `var`(variable: LetVariable<T>): Exp<T> = op("var", Exp(variable.name))

        internal fun concat(vararg values: Exp<String>): Exp<String> = op("concat", *values)

        internal fun downcase(value: Exp<String>): Exp<String> = op("downcase", value)

        internal fun product(vararg values: Exp<Number>): Exp<Number> = op("*", *values)

        internal fun zoom(): Exp<Number> = op("zoom")
    }
}

internal fun Exp(value: Boolean): Exp<Boolean> = Exp.Bare(JsonPrimitive(value))

internal fun Exp(value: Number): Exp<Number> = Exp.Bare(JsonPrimitive(value))

public fun Exp(value: String): Exp<String> = Exp.Bare(JsonPrimitive(value))

internal fun Exp<String>.downcastToColor(): Exp<Color> = Exp.Bare(this.asJson())

internal fun Exp<String>.downcastToResolvedImage(): Exp<ResolvedImage> = Exp.Bare(this.asJson())
