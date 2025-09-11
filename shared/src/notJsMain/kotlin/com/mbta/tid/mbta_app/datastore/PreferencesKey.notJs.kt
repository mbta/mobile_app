package com.mbta.tid.mbta_app.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey as realBooleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey as realStringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey as realStringSetPreferencesKey

internal actual typealias PreferencesKey<T> = Preferences.Key<T>

internal actual fun booleanPreferencesKey(name: String) = realBooleanPreferencesKey(name)

internal actual fun stringPreferencesKey(name: String) = realStringPreferencesKey(name)

internal actual fun stringSetPreferencesKey(name: String) = realStringSetPreferencesKey(name)
