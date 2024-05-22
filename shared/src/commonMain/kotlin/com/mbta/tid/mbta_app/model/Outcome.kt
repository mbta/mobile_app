package com.mbta.tid.mbta_app.model

sealed interface Failure

/**
 * The Obj-C/Swift interop doesn't support Kotlin's inline Result class so this is a simple
 * re-creation of it.
 */
data class Outcome<T, E : Failure>(val data: T, val error: E?)
