//
//  HomeMapViewHandlerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

/*
 Functions for handling interactions with the map, like prop change, navigation, and tapping.
 */
extension HomeMapView {
    func handleAppear(location: LocationManager?, map _: MapboxMap?) {
        lastNavEntry = nearbyVM.navigationStack.last
        Task {
            try await railRouteShapeFetcher.getRailRouteShapes()
        }

        // Set MapBox to use the current location to display puck
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        // If location data is provided, follow the user's location
        if locationDataManager.currentLocation != nil {
            viewportProvider.follow(animation: .easeInOut(duration: .zero))
        }

        didAppear?(self)
    }

    func handleGlobalMapDataChange(now: Date) {
        guard let globalData else { return }

        globalMapData = GlobalMapData(
            globalData: globalData,
            alertsByStop: GlobalMapData.companion.getAlertsByStop(
                globalData: globalData,
                alerts: nearbyVM.alerts,
                filterAtTime: now.toKotlinInstant()
            )
        )
    }

    func handleCameraChange(_ change: CameraChanged) {
        viewportProvider.updateCameraState(change.cameraState)
    }

    func handleNavStackChange(navigationStack: [SheetNavigationStackEntry]) {
        if let filter = navigationStack.lastStopDetailsFilter {
            vehiclesFetcher.leave()
            vehiclesFetcher.run(routeId: filter.routeId, directionId: Int(filter.directionId))
        } else if case let .tripDetails(tripId: _, vehicleId: _, target: _, routeId: routeId,
                                        directionId: directionId) = navigationStack.last {
            vehiclesFetcher.leave()
            vehiclesFetcher.run(routeId: routeId, directionId: Int(directionId))
        } else {
            vehiclesFetcher.leave()
        }

        lastNavEntry = navigationStack.last
    }

    func handleLastNavChange(oldNavEntry: SheetNavigationStackEntry?, nextNavEntry: SheetNavigationStackEntry?) {
        if oldNavEntry == nil {
            viewportProvider.saveNearbyTransitViewport()
        }
        if case let .stopDetails(stop, filter) = nextNavEntry {
            if oldNavEntry?.stop()?.id == stop.id {
                handleRouteFilterChange(filter)
            } else {
                handleStopDetailsChange(stop, filter)
            }
        } else {
            clearSelectedStop()
        }

        if case let .tripDetails(tripId: tripId, vehicleId: _, target: _, routeId: _, directionId: _) = nextNavEntry {
            handleTripDetailsChange(tripId)
        }
        if nextNavEntry == nil {
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    func handleTripDetailsChange(_ tripId: String) {
        Task {
            do {
                let response: ApiResult<TripShape> = try await RepositoryDI().trip.getTripShape(tripId: tripId)
                let shapesWithStops: [ShapeWithStops] = switch onEnum(of: response) {
                case let .ok(okResponse): [okResponse.data.shapeWithStops]
                case .error:
                    []
                }
                mapVM.routeSourceData = RouteSourceGenerator.shapesWithStopsToMapFriendly(
                    shapesWithStops,
                    globalData?.stops
                )

            } catch {
                debugPrint(error)
            }
        }
    }

    func handleStopDetailsChange(_ stop: Stop, _ filter: StopDetailsFilter?) {
        mapVM.stopSourceData = .init(selectedStopId: stop.id)
        viewportProvider.animateTo(coordinates: stop.coordinate, zoom: 17.0)

        Task {
            stopMapData = try await stopRepository.getStopMapData(stopId: stop.id)
            if let stopMapData {
                updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
            }
        }
    }

    func handleRouteFilterChange(_ filter: StopDetailsFilter?) {
        if let stopMapData {
            updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
        }
    }

    func clearSelectedStop() {
        stopMapData = nil
        resetDefaultSources()
    }

    func handleTapStopLayer(feature: QueriedFeature, _: MapContentGestureContext) -> Bool {
        guard case let .string(stopId) = feature.feature.properties?[StopSourceGenerator.propIdKey] else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped, but had invalid stop id prop. sourceId=\(feature.source)
            """)
            return false
        }
        guard let stop = globalData?.stops[stopId] else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped but stopId=\(stopId) didn't exist in global stops.
            """)
            return false
        }
        analytics.tappedOnStop(stopId: stop.id)
        nearbyVM.navigationStack.removeAll()
        nearbyVM.navigationStack.append(.stopDetails(stop, nil))
        return true
    }

    func handleTapVehicle(_ vehicle: Vehicle) {
        guard let tripId = vehicle.tripId else { return }

        if case .tripDetails = nearbyVM.navigationStack.last {
            // If a trip details page is already on the stack, replace it with this one
            _ = nearbyVM.navigationStack.popLast()
        }

        guard let departures = nearbyVM.departures,
              let patterns = departures.routes.first(where: { patterns in
                  patterns.routes.contains { $0.id == vehicle.routeId }
              }),
              let trip = patterns.allUpcomingTrips().first(where: { upcoming in
                  upcoming.trip.id == tripId
              }),
              let stopSequence = trip.stopSequence?.intValue
        else {
            // If we're missing the stop ID or stop sequence, we can still navigate to the trip details
            // page, but we won't be able to tell what the target stop was.
            nearbyVM.navigationStack.append(.tripDetails(
                tripId: tripId,
                vehicleId: vehicle.id,
                target: nil,
                // TODO: figure out something to do if this is nil
                routeId: vehicle.routeId!,
                directionId: vehicle.directionId
            ))
            return
        }

        nearbyVM.navigationStack.append(.tripDetails(
            tripId: tripId,
            vehicleId: vehicle.id,
            target: .init(
                stopId: patterns.stop.id,
                stopSequence: stopSequence
            ),
            routeId: trip.trip.routeId,
            directionId: trip.trip.directionId
        ))
    }
}
