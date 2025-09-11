package com.mbta.tid.mbta_app.datastore

internal actual abstract class Preferences {
    actual abstract operator fun <T> get(key: PreferencesKey<T>): T?

    abstract fun toMutablePreferences(): MutablePreferences
}

internal actual class MutablePreferences(
    private val data: MutableMap<String, Any?> = mutableMapOf()
) : Preferences() {
    override fun toMutablePreferences(): MutablePreferences {
        return MutablePreferences(data.toMutableMap())
    }

    actual override fun <T> get(key: PreferencesKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return data[key.name] as? T?
    }

    actual operator fun <T> set(key: PreferencesKey<T>, value: T) {
        data[key.name] = value
    }
}
