package com.mbta.tid.mbta_app.map.style

import com.mbta.tid.mbta_app.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

private fun JsonArrayBuilder.addOperand(operand: Any?) {
    when (operand) {
        null -> add(JsonNull)
        is Exp -> add(operand.body)
        is Number -> add(operand)
        is Pair<*, *> -> {
            addOperand(operand.first)
            addOperand(operand.second)
        }
        is String -> add(operand)
        else -> throw UnsupportedOperationException("unsupported operand type $operand")
    }
}

/**
 * Represents a
 * [Mapbox Style Spec expression](https://docs.mapbox.com/style-spec/reference/expressions).
 *
 * If Mapbox ever gives a type error like `Expected boolean but found array<string, 3> instead.`,
 * that probably means you've spelled an operator incorrectly (e.g. `eq` instead of `==`), and the
 * nonexistent operator is being interpreted as just the first value in an array literal.
 */
class Exp private constructor(val body: JsonElement) {
    constructor(operator: String) : this(buildJsonArray { add(operator) })

    constructor(
        operator: String,
        vararg operands: Any
    ) : this(
        buildJsonArray {
            add(operator)
            for (operand in operands) {
                addOperand(operand)
            }
        }
    )

    fun toJsonString() = json.encodeToString(body)

    companion object {
        fun bareValue(value: JsonElement) = Exp(value)
    }
}
