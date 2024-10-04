package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class VisitHistoryUsecaseTest {

    @Test
    fun testAddVisits() = runBlocking {
        val repository = MockVisitHistoryRepository()
        val usecase = VisitHistoryUsecase(repository)

        val objects = ObjectCollectionBuilder()
        val stopA = objects.stop()
        val stopB = objects.stop()
        val stopC = objects.stop()
        assertTrue { usecase.getLatestVisits().isEmpty() }
        val visitA = Visit.StopVisit(stopId = stopA.id)
        usecase.addVisit(visitA)
        assertEquals(usecase.getLatestVisits(), listOf(visitA))
        val visitB = Visit.StopVisit(stopId = stopB.id)
        usecase.addVisit(visitB)
        assertEquals(usecase.getLatestVisits(), listOf(visitB, visitA))
        val visitC = Visit.StopVisit(stopId = stopC.id)
        usecase.addVisit(visitC)
        assertEquals(usecase.getLatestVisits(), listOf(visitC, visitB, visitA))
        val visitBAgain = Visit.StopVisit(stopId = stopB.id)
        usecase.addVisit(visitBAgain)
        assertEquals(usecase.getLatestVisits(), listOf(visitBAgain, visitC, visitA))
    }

    @Test
    fun testLimitReturned() = runBlocking {
        val repository = MockVisitHistoryRepository()
        val usecase = VisitHistoryUsecase(repository)

        val objects = ObjectCollectionBuilder()

        var visitCount = 0
        while (visitCount < VisitHistory.SAVE_COUNT + 2) {
            val stop = objects.stop()
            usecase.addVisit(Visit.StopVisit(stopId = stop.id))
            visitCount++
        }

        assertEquals(VisitHistory.RETRIEVE_COUNT, usecase.getLatestVisits().size)
        val stop = objects.stop()
        val visit = Visit.StopVisit(stopId = stop.id)
        usecase.addVisit(visit)
        val latest = usecase.getLatestVisits()
        assertEquals(visit, latest.first())
        assertEquals(VisitHistory.RETRIEVE_COUNT, latest.size)
        assertEquals(
            VisitHistory.SAVE_COUNT,
            repository.getVisitHistory().latest(VisitHistory.SAVE_COUNT + 5).size
        )
    }
}
