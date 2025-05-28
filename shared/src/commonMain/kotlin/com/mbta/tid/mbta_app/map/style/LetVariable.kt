package com.mbta.tid.mbta_app.map.style

data class LetVariable<T>(val name: String) {
    infix fun boundTo(value: Exp<T>) = Binding(this, value)

    data class Binding<T>(val variable: LetVariable<T>, val value: Exp<T>)
}
