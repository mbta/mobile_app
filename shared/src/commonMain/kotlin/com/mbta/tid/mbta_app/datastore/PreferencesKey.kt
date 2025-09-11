package com.mbta.tid.mbta_app.datastore

internal expect class PreferencesKey<T>

internal expect fun booleanPreferencesKey(name: String): PreferencesKey<Boolean>

internal expect fun stringPreferencesKey(name: String): PreferencesKey<String>

internal expect fun stringSetPreferencesKey(name: String): PreferencesKey<Set<String>>
