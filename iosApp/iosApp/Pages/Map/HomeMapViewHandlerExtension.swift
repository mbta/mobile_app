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

/**
 Functions for handling interactions with the map, like prop change, navigation, and tapping.
 */
extension HomeMapView {
    @MainActor
    func handleAppear(location: LocationManager?, map: MapboxMap?) async throws {
        try await handleTryLayerInit(map: map)

        // Set MapBox to use the current location to display puck
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        // If location data is provided, follow the user's location
        if locationDataManager.currentLocation != nil {
            viewportProvider.follow(instantly: true)
        }
        didAppear?(self)
    }

    func handleCameraChange(_ change: CameraChanged) {
        viewportProvider.updateCameraState(change.cameraState)
    }

    func handleNavStackChange(entry: SheetNavigationStackEntry) {
        let filter: RouteStopFilter? = switch entry {
        case let .stopDetails(_, routeStopFilter, _):
            routeStopFilter
        case let .tripDetails(tripPageFilter):
            tripPageFilter.routeStopFilter
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

    func joinVehiclesChannel(routeId: LineOrRoute.Id, directionId: Int32) {
        leaveVehiclesChannel()
        vehiclesRepository.connect(
            routeId: routeId,
            directionId: directionId,
            errorKey: .companion.fromSheetTypes(
                sheetTypes: [.stopDetails, .tripDetails],
                id: "HomeMapView.joinVehiclesChannel"
            ),
        ) { outcome in
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
        navManager.popToEntrypoint()
        navManager.pushNavEntry(.stopDetails(stopId: stop.id, routeStopFilter: nil, tripFilter: nil))
        return true
    }

    func handleTapVehicle(_ vehicle: Vehicle) {
        guard let tripId = vehicle.tripId else { return }
        let currentNavEntry = navManager.navigationStack.lastSafe()
        let (stopId, routeStopFilter, tripFilter): (String?, RouteStopFilter?,
                                                    TripDetailsFilter?) = switch currentNavEntry {
        case let .stopDetails(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: tripFilter): (
                stopId,
                routeStopFilter,
                tripFilter
            )
        case let .tripDetails(filter: filter): (filter.stopId, filter.routeStopFilter, filter.tripDetailsFilter)
        default: (nil, nil, nil)
        }
        guard let stopId, let routeStopFilter else { return }
        let routeCard = routeCardDataState?.data?
            .first(where: { $0.lineOrRoute.containsRoute(routeId: vehicle.routeId) })
        let upcoming = routeCard?
            .stopData
            .flatMap(\.data)
            .flatMap(\.upcomingTrips)
            .first { upcoming in upcoming.trip.id == tripId }
        let stopSequence = upcoming?.stopSequence ?? tripFilter?.stopSequence
        let routeId = upcoming?.routeId ?? vehicle.routeId ?? routeCard?.lineOrRoute.id ?? routeStopFilter.routeId

        analytics.tappedVehicle(routeId: routeId)

        let newTripFilter = TripDetailsFilter(
            tripId: tripId,
            vehicleId: vehicle.id,
            stopSequence: stopSequence,
            selectionLock: true
        )
        navManager.pushNavEntry(.tripDetails(filter: .init(
            tripId: tripId,
            vehicleId: vehicle.id,
            routeId: routeId,
            directionId: routeStopFilter.directionId,
            stopId: stopId,
            stopSequence: stopSequence
        )))
    }
}
