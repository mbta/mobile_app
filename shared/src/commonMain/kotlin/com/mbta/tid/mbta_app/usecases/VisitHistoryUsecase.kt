package com.mbta.tid.mbta_app.usecases

import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.repositories.IVisitHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent

public class VisitHistoryUsecase(private val repository: IVisitHistoryRepository) : KoinComponent {
    private val mutex = Mutex()

    private val visitHistoryKey = stringPreferencesKey("visit_history")

    public suspend fun addVisit(visit: Visit) {
        mutex.withLock {
            val history = repository.getVisitHistory()
            history.add(visit)
            repository.setVisitHistory(visits = history)
        }
    }

    internal suspend fun getLatestVisits(): List<Visit> {
        mutex.withLock {
            return repository.getVisitHistory().latest()
        }
    }
}
