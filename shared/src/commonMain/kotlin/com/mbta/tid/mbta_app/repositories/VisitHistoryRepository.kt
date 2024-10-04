package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.encodeToString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IVisitHistoryRepository {
    suspend fun getVisitHistory(): VisitHistory

    suspend fun setVisitHistory(visits: VisitHistory)
}

class VisitHistoryRepository : IVisitHistoryRepository, KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()
    private val mutex = Mutex()

    private val visitHistoryKey = stringPreferencesKey("visit_history")

    override suspend fun getVisitHistory(): VisitHistory {
        val encoded = dataStore.data.map { it[visitHistoryKey] }.first()
        if (encoded.isNullOrBlank()) {
            return VisitHistory()
        }
        return try {
            json.decodeFromString(encoded)
        } catch (e: Exception) {
            VisitHistory()
        }
    }

    override suspend fun setVisitHistory(visits: VisitHistory) {
        dataStore.edit { it[visitHistoryKey] = json.encodeToString(visits) }
    }
}

class MockVisitHistoryRepository : IVisitHistoryRepository {
    private var history = VisitHistory()

    override suspend fun getVisitHistory(): VisitHistory {
        return history
    }

    override suspend fun setVisitHistory(visits: VisitHistory) {
        history = visits
    }
}
