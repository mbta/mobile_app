package com.mbta.tid.mbta_app.map.style

import kotlinx.serialization.json.JsonPrimitive

internal sealed interface ArrayType<T> : MapboxStyleObject {
    data object String : ArrayType<kotlin.String>

    data object Number : ArrayType<kotlin.Number>

    data object Boolean : ArrayType<kotlin.Boolean>

    override fun asJson() =
        when (this) {
            String -> JsonPrimitive("string")
            Number -> JsonPrimitive("number")
            Boolean -> JsonPrimitive("boolean")
        }
}
