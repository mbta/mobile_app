//
//  HomeMapViewHandlerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import shared
import SwiftUI

/*
 Functions for handling interactions with the map, like prop change, navigation, and tapping.
 */
extension HomeMapView {
    func handleAppear(location: LocationManager?, map: MapboxMap?) {
        handleTryLayerInit(map: map)
        lastNavEntry = nearbyVM.navigationStack.last
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "HomeMapView.handleAppear",
                getData: { try await railRouteShapeRepository.getRailRouteShapes() },
                onSuccess: { result in
                    railRouteShapes = result
                },
                onRefreshAfterError: { handleAppear(location: location, map: map) }
            )
        }

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

    func handleGlobalMapDataChange(now: Date) {
        guard let globalData else { return }

        Task(priority: .high) {
            let newMapData = GlobalMapData(
                globalData: globalData,
                alertsByStop: GlobalMapData.companion.getAlertsByStop(
                    globalData: globalData,
                    alerts: nearbyVM.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            )
            DispatchQueue.main.async { globalMapData = newMapData }
        }
    }

    func handleCameraChange(_ change: CameraChanged) {
        viewportProvider.updateCameraState(change.cameraState)
    }

    func handleNavStackChange(navigationStack: [SheetNavigationStackEntry]) {
        if let filter = navigationStack.lastStopDetailsFilter {
            joinVehiclesChannel(routeId: filter.routeId, directionId: filter.directionId)
        } else if case let .tripDetails(
            tripId: _, vehicleId: _, target: _,
            routeId: routeId, directionId: directionId
        ) = navigationStack.last {
            joinVehiclesChannel(routeId: routeId, directionId: directionId)
        } else {
            leaveVehiclesChannel()
        }

        lastNavEntry = navigationStack.last
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "HomeMapView.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobalData
            )
        }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            self.globalData = globalData
        }
    }

    func joinVehiclesChannel(navStackEntry entry: SheetNavigationStackEntry) {
        if case let .stopDetails(_, filter) = entry, let filter {
            joinVehiclesChannel(routeId: filter.routeId,
                                directionId: filter.directionId)
        }
        if case let .tripDetails(tripId: _, vehicleId: _, target: _, routeId: routeId,
                                 directionId: directionId) = entry {
            joinVehiclesChannel(routeId: routeId,
                                directionId: directionId)
        }
    }

    func joinVehiclesChannel(routeId: String, directionId: Int32) {
        leaveVehiclesChannel()
        vehiclesRepository.connect(routeId: routeId, directionId: directionId) { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                if let departures = nearbyVM.departures {
                    vehiclesData = Array(departures.filterVehiclesByUpcoming(vehicles: result.data).values)
                }
            }
        }
    }

    func leaveVehiclesChannel() {
        vehiclesRepository.disconnect()
        vehiclesData = []
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
        }

        if case let .tripDetails(tripId: tripId,
                                 vehicleId: _,
                                 target: target,
                                 routeId: _,
                                 directionId: _) = nextNavEntry {
            handleTripDetailsChange(tripId, target?.stopId)
        }
        if nextNavEntry == nil {
            clearSelectedStop()
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    func handleTripDetailsChange(_ tripId: String, _ targetStopId: String?) {
        Task {
            let dataErrorKey = "HomeMapView.handleTripDetailsChange"
            do {
                let response: ApiResult<TripShape> = try await RepositoryDI().trip.getTripShape(tripId: tripId)
                let getTripShapeErrorKey = "\(dataErrorKey)/getTripShape"
                var shapesWithStops: [ShapeWithStops] = []
                switch onEnum(of: response) {
                case let .ok(okResponse):
                    errorBannerRepository.clearDataError(key: getTripShapeErrorKey)
                    shapesWithStops = [okResponse.data.shapeWithStops]
                case .error:
                    errorBannerRepository.setDataError(key: getTripShapeErrorKey) {
                        handleTripDetailsChange(tripId, targetStopId)
                    }
                }
                mapVM.routeSourceData = RouteFeaturesBuilder.shared.shapesWithStopsToMapFriendly(
                    shapesWithStops: shapesWithStops,
                    stopsById: globalData?.stops
                )

                let filteredStopIds = shapesWithStops.flatMap(\.stopIds).map { stopId in
                    globalData?.stops[stopId]?.resolveParent(stops: globalData?.stops ?? [:]).id ?? stopId
                }

                mapVM.stopSourceData = .init(filteredStopIds: filteredStopIds, selectedStopId: targetStopId)

                errorBannerRepository.clearDataError(key: dataErrorKey)
            } catch {
                errorBannerRepository.setDataError(key: dataErrorKey) {
                    handleTripDetailsChange(tripId, targetStopId)
                }
            }
        }
    }

    func handleStopDetailsChange(_ stop: Stop, _ filter: StopDetailsFilter?) {
        mapVM.stopSourceData = .init(selectedStopId: stop.id)
        viewportProvider.animateTo(coordinates: stop.coordinate)

        Task {
            if case let .ok(result) = try await onEnum(of: stopRepository.getStopMapData(stopId: stop.id)) {
                stopMapData = result.data
                if let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
                }
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
        guard case let .string(stopId) = feature.feature.properties?[StopFeaturesBuilder.shared.propIdKey.key] else {
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

        let departures = nearbyVM.departures
        let patterns = departures?.routes.first(where: { patterns in
            patterns.routes.contains { $0.id == vehicle.routeId }
        })
        let trip = patterns?.allUpcomingTrips().first(where: { upcoming in
            upcoming.trip.id == tripId
        })
        let stopSequence = trip?.stopSequence?.intValue

        guard let routeId = trip?.trip.routeId ?? vehicle.routeId else {
            // TODO: figure out something to do if this is nil
            return
        }

        // If we're missing the stop ID or stop sequence, we can still navigate to the trip details
        // page, but we won't be able to tell what the target stop was.
        nearbyVM.navigationStack.append(.tripDetails(
            tripId: tripId,
            vehicleId: vehicle.id,
            target: patterns != nil ? .init(
                stopId: patterns!.stop.id,
                stopSequence: stopSequence
            ) : nil,
            routeId: routeId,
            directionId: vehicle.directionId
        ))
    }

    func handleSelectedVehicleChange(_ previousVehicle: Vehicle?, _ nextVehicle: Vehicle?) {
        guard let globalData else { return }
        guard let nextVehicle else {
            viewportProvider.updateFollowedVehicle(vehicle: nil)
            return
        }

        if previousVehicle == nil || previousVehicle?.id != nextVehicle.id {
            viewportProvider.followVehicle(vehicle: nextVehicle, target: nearbyVM.getTargetStop(global: globalData))
        } else {
            viewportProvider.updateFollowedVehicle(vehicle: nextVehicle)
        }
    }
}
