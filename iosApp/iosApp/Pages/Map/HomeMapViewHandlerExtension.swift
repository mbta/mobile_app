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

    func handleNavStackChange(entry: SheetNavigationStackEntry) {
        let filter: StopDetailsFilter? = switch entry {
        case let .stopDetails(_, stopFilter, _):
            stopFilter
        case let .tripDetails(tripPageFilter):
            tripPageFilter.stopFilter
        default:
            nil
        }
        guard let filter else {
            leaveVehiclesChannel()
            vehiclesData = []
            return
        }
        joinVehiclesChannel(
            routeId: filter.routeId,
            directionId: filter.directionId
        )
    }

    func checkOnboardingLoaded() {
        if contentVM.onboardingScreensPending == [] {
            locationDataManager.requestWhenInUseAuthorization()
        }
    }

    func joinVehiclesChannel(routeId: String, directionId: Int32) {
        leaveVehiclesChannel()
        vehiclesRepository.connect(routeId: routeId, directionId: directionId) { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                if let routeCardData = routeCardDataState?.data {
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
        let trackThisTrip = settingsCache.get(.trackThisTrip)
        guard let tripId = vehicle.tripId else { return }
        let currentNavEntry = nearbyVM.navigationStack.lastSafe()
        let (stopId, stopFilter, tripFilter): (String?, StopDetailsFilter?,
                                               TripDetailsFilter?) = switch currentNavEntry {
        case let .stopDetails(stopId: stopId, stopFilter: stopFilter, tripFilter: tripFilter): (
                stopId,
                stopFilter,
                tripFilter
            )
        case let .tripDetails(filter: filter): (filter.stopId, filter.stopFilter, filter.tripDetailsFilter)
        default: (nil, nil, nil)
        }
        guard let stopId, let stopFilter, (tripFilter?.tripId != tripId || trackThisTrip) else { return }
        let routeCard = routeCardDataState?.data?
            .first(where: { $0.lineOrRoute.containsRoute(routeId: vehicle.routeId) })
        let upcoming = routeCard?
            .stopData
            .flatMap(\.data)
            .flatMap(\.upcomingTrips)
            .first { upcoming in upcoming.trip.id == tripId }
        let stopSequence = upcoming?.stopSequence ?? tripFilter?.stopSequence
        let routeId = upcoming?.trip.routeId ?? vehicle.routeId ?? routeCard?.lineOrRoute.id ?? stopFilter.routeId

        analytics.tappedVehicle(routeId: routeId)

        let newTripFilter = TripDetailsFilter(
            tripId: tripId,
            vehicleId: vehicle.id,
            stopSequence: stopSequence,
            selectionLock: true
        )
        if settingsCache.get(.trackThisTrip) {
            nearbyVM.pushNavEntry(.tripDetails(filter: .init(
                tripId: tripId,
                vehicleId: vehicle.id,
                routeId: routeId,
                directionId: stopFilter.directionId,
                stopId: stopId,
                stopSequence: stopSequence
            )))
        } else {
            nearbyVM.navigationStack.lastTripDetailsFilter = newTripFilter
            mapVM.selectedTrip(
                stopFilter: stopFilter,
                stop: globalData?.getStop(stopId: stopId),
                tripFilter: newTripFilter,
                vehicle: vehicle,
                follow: false
            )
        }
    }
}
