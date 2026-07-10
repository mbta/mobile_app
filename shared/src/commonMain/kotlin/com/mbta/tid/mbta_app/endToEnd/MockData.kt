package com.mbta.tid.mbta_app.endToEnd

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.Vehicle
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.time.Duration.Companion.minutes

internal fun endToEndMockData(): Triple<ObjectCollectionBuilder, String, String> {
    val now = EasternTimeInstant.now()
    val objects = TestData.clone()

    val govCenter = objects.getStop("70201")
    val greenRp0 = objects.getRoutePattern("Green-B-812-0")
    val greenTrip0 = objects.getTrip("canonical-Green-B-C1-0")
    val greenBVehicle0 = objects.vehicle {
        directionId = greenTrip0.directionId
        routeId = greenTrip0.routeId.idText
        tripId = greenTrip0.id
        stopId = govCenter.id
        currentStatus = Vehicle.CurrentStatus.IncomingAt
    }
    val predictionGreenGovCenter = objects.prediction {
        trip = greenTrip0
        stopSequence = 0
        stopId = govCenter.id
        departureTime = now + 10.minutes
        vehicleId = greenBVehicle0.id
    }
    val predictionGreenPark = objects.prediction {
        trip = greenTrip0
        stopSequence = 1
        stopId = "70200"
        departureTime = now + 11.minutes
        vehicleId = greenBVehicle0.id
    }
    val predictionGreenBoyls = objects.prediction {
        trip = greenTrip0
        stopSequence = 2
        stopId = "70158"
        departureTime = now + 12.minutes
        vehicleId = greenBVehicle0.id
    }

    val scheduleGreenGovCenter = objects.schedule {
        trip = greenTrip0
        stopSequence = 0
        stopId = govCenter.id
        departureTime = now + 5.minutes
    }
    val scheduleGreenPark = objects.schedule {
        trip = greenTrip0
        stopSequence = 1
        stopId = "70200"
        departureTime = now + 6.minutes
    }
    val scheduleGreenBoyls = objects.schedule {
        trip = greenTrip0
        stopSequence = 2
        stopId = "70158"
        departureTime = now + 7.minutes
    }

    return Triple(objects, greenTrip0.id, greenBVehicle0.id)
}
