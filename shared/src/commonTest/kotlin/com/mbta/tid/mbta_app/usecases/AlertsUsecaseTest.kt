package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.Alert
import com.mbta.tid.mbta_app.model.Facility
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IdleGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockAlertsRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AlertsUsecaseTest {
    @Test
    fun `connect with successful alerts response and existing global state`() {
        val objects = ObjectCollectionBuilder()
        val facility = objects.facility { type = Facility.Type.Elevator }
        val alert =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
            }

        val alertResponse = AlertsStreamDataResponse(objects)
        val mockGlobalRepository = MockGlobalRepository(GlobalResponse(objects))
        val mockAlertsRepository = MockAlertsRepository(alertResponse)
        val usecase = AlertsUsecase(mockAlertsRepository, mockGlobalRepository)

        var result: ApiResult<AlertsStreamDataResponse>? = null

        usecase.connect { result = it }

        assertNotNull(result)
        assertTrue(result is ApiResult.Ok)
        assertEquals(
            facility,
            (result as ApiResult.Ok<AlertsStreamDataResponse>)
                .data
                .getAlert(alert.id)
                ?.facilities
                ?.get(facility.id),
        )
    }

    @Test
    fun `global data is still injected when alert data changes`() {
        val objects = ObjectCollectionBuilder()
        val facility = objects.facility { type = Facility.Type.Elevator }
        val alert1 =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
            }

        val mockGlobalRepository = MockGlobalRepository(GlobalResponse(objects))
        val mockAlertsRepository = MockAlertsRepository(AlertsStreamDataResponse(objects))
        val usecase = AlertsUsecase(mockAlertsRepository, mockGlobalRepository)

        var result: ApiResult<AlertsStreamDataResponse>? = null

        usecase.connect { result = it }

        assertNotNull(result)
        assertTrue(result is ApiResult.Ok)
        assertEquals(
            facility,
            (result as ApiResult.Ok<AlertsStreamDataResponse>)
                .data
                .getAlert(alert1.id)
                ?.facilities
                ?.get(facility.id),
        )

        val alert2 =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility.id,
                )
            }
        mockAlertsRepository.receiveResult(ApiResult.Ok(AlertsStreamDataResponse(objects)))
        assertEquals(
            facility,
            (result as ApiResult.Ok<AlertsStreamDataResponse>)
                .data
                .getAlert(alert2.id)
                ?.facilities
                ?.get(facility.id),
        )
    }

    @Test
    fun `error responses are passed through`() {
        val mockGlobalRepository = IdleGlobalRepository()
        val mockAlertsRepository = MockAlertsRepository(ApiResult.Error(message = "Error"))
        val usecase = AlertsUsecase(mockAlertsRepository, mockGlobalRepository)

        var result: ApiResult<AlertsStreamDataResponse>? = null

        usecase.connect { result = it }

        assertNotNull(result)
        assertTrue(result is ApiResult.Error)
        assertEquals("Error", (result as ApiResult.Error<AlertsStreamDataResponse>).message)
    }

    @Test
    fun `alerts are passed through when global data is null`() {
        val objects = ObjectCollectionBuilder()
        val alert = objects.alert {}

        val mockGlobalRepository = IdleGlobalRepository()
        val mockAlertsRepository = MockAlertsRepository(AlertsStreamDataResponse(objects))
        val usecase = AlertsUsecase(mockAlertsRepository, mockGlobalRepository)

        var result: ApiResult<AlertsStreamDataResponse>? = null

        usecase.connect { result = it }

        assertNotNull(result)
        assertTrue(result is ApiResult.Ok)
        val okResult = (result as ApiResult.Ok<AlertsStreamDataResponse>)
        assertEquals(alert, okResult.data.getAlert(alert.id))
        assertNull(okResult.data.getAlert(alert.id)?.facilities)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `alerts are updated when global data is updated`() = runTest {
        val objects = ObjectCollectionBuilder()
        val facility1 = objects.facility { type = Facility.Type.Elevator }
        val alert =
            objects.alert {
                informedEntity(
                    listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                    facility = facility1.id,
                )
            }
        val mockGlobalRepository = MockGlobalRepository(GlobalResponse(objects))
        val mockAlertsRepository = MockAlertsRepository(AlertsStreamDataResponse(objects))
        val usecase =
            AlertsUsecase(
                mockAlertsRepository,
                mockGlobalRepository,
                StandardTestDispatcher(testScheduler),
            )

        var result: ApiResult<AlertsStreamDataResponse>? = null

        usecase.connect { result = it }

        assertNotNull(result)
        assertTrue(result is ApiResult.Ok)
        assertEquals(
            facility1,
            (result as ApiResult.Ok<AlertsStreamDataResponse>)
                .data
                .getAlert(alert.id)
                ?.facilities
                ?.get(facility1.id),
        )

        val facility2 =
            objects.facility {
                id = facility1.id
                shortName = "New name"
                type = Facility.Type.Elevator
            }
        mockGlobalRepository.updateGlobalData(GlobalResponse(objects))

        advanceUntilIdle()

        assertEquals(
            facility2,
            (result as ApiResult.Ok<AlertsStreamDataResponse>)
                .data
                .getAlert(alert.id)
                ?.facilities
                ?.get(facility2.id),
        )
    }
}
