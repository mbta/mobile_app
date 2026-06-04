package com.mbta.tid.mbta_app.model

public sealed class Matcher<T : Any> {
    public data class Data<T : Any>(val value: T) : Matcher<T>() {
        override fun matches(actual: T) = actual == this.value
    }

    public data class AnyOf<T : Any>(val values: Collection<T>) : Matcher<T>() {
        public constructor(vararg values: T) : this(values.toList())

        override fun matches(actual: T) = actual in this.values
    }

    public class Wildcard<T : Any> : Matcher<T>() {
        override fun matches(actual: T) = true
    }

    internal abstract fun matches(actual: T): Boolean
}
