//
//  HomeMapViewHandlerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

/*
 Functions for handling interactions with the map, like prop change, navigation, and tapping.
 */
extension HomeMapView {
    func handleAppear(location: LocationManager?, map: MapboxMap?) {
        handleTryLayerInit(map: map)

        // Set MapBox to use the current location to display puck
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        // If location data is provided, follow the user's location
        if locationDataManager.currentLocation != nil, viewportProvider.isDefault() {
            viewportProvider.follow(animation: .easeInOut(duration: .zero))
        }
        didAppear?(self)
    }

    func handleCameraChange(_ change: CameraChanged) {
        viewportProvider.updateCameraState(change.cameraState)
    }

    func handleNavStackChange(navigationStack: [SheetNavigationStackEntry]) {
        if let filter = navigationStack.lastStopDetailsFilter {
            joinVehiclesChannel(routeId: filter.routeId, directionId: filter.directionId)
        } else {
            leaveVehiclesChannel()
            vehiclesData = []
        }
    }

    func checkOnboardingLoaded() {
        if contentVM.onboardingScreensPending == [] {
            locationDataManager.requestWhenInUseAuthorization()
        }
    }

    func joinVehiclesChannel(navStackEntry entry: SheetNavigationStackEntry) {
        if case let .stopDetails(stopId: _, stopFilter: stopFilter, tripFilter: _) = entry, let stopFilter {
            joinVehiclesChannel(
                routeId: stopFilter.routeId,
                directionId: stopFilter.directionId
            )
        }
    }

    func joinVehiclesChannel(routeId: String, directionId: Int32) {
        leaveVehiclesChannel()
        vehiclesRepository.connect(routeId: routeId, directionId: directionId) { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                if let routeCardData = nearbyVM.routeCardData {
                    vehiclesData = Array(StopDetailsUtils.shared.filterVehiclesByUpcoming(
                        routeCardData: routeCardData,
                        vehicles: result.data
                    ).values)
                }
            }
        }
    }

    func leaveVehiclesChannel() {
        vehiclesRepository.disconnect()
    }

    func handleTapStopLayer(feature: FeaturesetFeature, _: InteractionContext) -> Bool {
        guard case let .string(stopId) = feature.properties[StopFeaturesBuilder.shared.propIdKey.key] else {
            let featureId = feature.id.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped, but had invalid stop id prop.
            """)
            return false
        }
        guard let stop = globalData?.getStop(stopId: stopId) else {
            let featureId = feature.id.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped but stopId=\(stopId) didn't exist in global stops.
            """)
            return false
        }
        analytics.tappedOnStop(stopId: stop.id)
        nearbyVM.popToEntrypoint()
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil))
        return true
    }

    func handleTapVehicle(_ vehicle: Vehicle) {
        guard let tripId = vehicle.tripId,
              case let .stopDetails(_, stopFilter, tripFilter) = nearbyVM.navigationStack.lastSafe(),
              stopFilter != nil || tripFilter?.tripId == tripId
        else { return }
        let routeCard = nearbyVM.routeCardData?.first(where: { $0.lineOrRoute.containsRoute(routeId: vehicle.routeId) })
        let upcoming = routeCard?
            .stopData
            .flatMap(\.data)
            .flatMap(\.upcomingTrips)
            .first { upcoming in upcoming.trip.id == tripId }
        let stopSequence = upcoming?.stopSequence
        let routeId = upcoming?.trip.routeId ?? vehicle.routeId ?? routeCard?.lineOrRoute.id

        if let routeId { analytics.tappedVehicle(routeId: routeId) }

        let newTripFilter = TripDetailsFilter(
            tripId: tripId,
            vehicleId: vehicle.id,
            stopSequence: stopSequence,
            selectionLock: true
        )
        nearbyVM.navigationStack.lastTripDetailsFilter = newTripFilter
    }
}
