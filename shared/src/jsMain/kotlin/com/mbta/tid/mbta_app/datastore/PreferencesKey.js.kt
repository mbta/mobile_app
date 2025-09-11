package com.mbta.tid.mbta_app.datastore

internal actual data class PreferencesKey<T>(val name: String)

internal actual fun booleanPreferencesKey(name: String) = PreferencesKey<Boolean>(name)

internal actual fun stringPreferencesKey(name: String) = PreferencesKey<String>(name)

internal actual fun stringSetPreferencesKey(name: String) = PreferencesKey<Set<String>>(name)
