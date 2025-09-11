package com.mbta.tid.mbta_app.datastore

internal expect abstract class Preferences internal constructor() {
    abstract operator fun <T> get(key: PreferencesKey<T>): T?
}

internal expect class MutablePreferences : Preferences {
    override fun <T> get(key: PreferencesKey<T>): T?

    operator fun <T> set(key: PreferencesKey<T>, value: T)
}
