package com.mbta.tid.mbta_app.android.util

/**
 * Represents an object which may be created in the future so that operations can be run instantly
 * or queued if the object is not ready.
 */
class LazyObjectQueue<T> {
    var `object`: T? = null
        private set

    suspend fun setObject(value: T?) {
        `object` = value
        if (value != null) {
            val oldQueue = queue
            queue = mutableListOf()
            for (op in oldQueue) {
                value.op()
            }
        }
    }

    private var queue: MutableList<suspend T.() -> Unit> = mutableListOf()

    suspend fun run(op: suspend T.() -> Unit) {
        val `object` = this.`object`
        if (`object` != null) {
            `object`.op()
        } else {
            queue.add(op)
        }
    }
}
