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
        lastNavEntry = nearbyVM.navigationStack.last
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

    func handleGlobalMapDataChange(now: Date) {
        guard let globalData = mapVM.globalData else { return }

        Task(priority: .high) {
            let newMapData = GlobalMapData(
                globalData: globalData,
                alerts: nearbyVM.alerts,
                filterAtTime: now.toEasternInstant()
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
        } else {
            leaveVehiclesChannel()
            vehiclesData = []
        }

        lastNavEntry = navigationStack.last
    }

    func checkOnboardingLoaded() {
        if contentVM.onboardingScreensPending == [] {
            locationDataManager.requestWhenInUseAuthorization()
        }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            mapVM.globalData = globalData
        }
    }

    func fetchGlobalData() {
        Task {
            await fetchApi(
                errorKey: "HomeMapView.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: fetchGlobalData
            )
        }
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        fetchGlobalData()
    }

    @MainActor
    func activateRouteShapeListener() async {
        for await railRouteShapes in railRouteShapeRepository.state {
            self.railRouteShapes = railRouteShapes
        }
    }

    func fetchRouteShapes() {
        Task {
            await fetchApi(
                errorKey: "HomeMapView.handleAppear",
                getData: { try await railRouteShapeRepository.getRailRouteShapes() },
                onRefreshAfterError: fetchRouteShapes
            )
        }
    }

    func loadRouteShapes() {
        Task(priority: .high) {
            await activateRouteShapeListener()
        }
        fetchRouteShapes()
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

    func handleLastNavChange(oldNavEntry: SheetNavigationStackEntry?, nextNavEntry: SheetNavigationStackEntry?) {
        if oldNavEntry == nil || oldNavEntry?.isEntrypoint == true {
            viewportProvider.saveNearbyTransitViewport()
        }
        if case let .stopDetails(stopId, stopFilter, _) = nextNavEntry {
            if oldNavEntry?.stopId() != stopId {
                if let stop = mapVM.globalData?.getStop(stopId: stopId) {
                    handleStopDetailsChange(stop, stopFilter)
                }
            }

            handleRouteFilterChange(stopFilter)
        }

        if nextNavEntry == nil || nextNavEntry?.isEntrypoint == true {
            clearSelectedStop()
            viewportProvider.restoreNearbyTransitViewport()
            mapVM.routeSourceData = mapVM.allRailSourceData
            if let layerManager = mapVM.layerManager {
                addLayers(layerManager)
            }
        }
    }

    func handleStopDetailsChange(_ stop: Stop, _ filter: StopDetailsFilter?) {
        mapVM.stopLayerState = .init(selectedStopId: stop.id, stopFilter: filter)
        viewportProvider.animateTo(coordinates: stop.coordinate)

        Task {
            if case let .ok(result) = try await onEnum(of: stopRepository.getStopMapData(stopId: stop.id)) {
                stopMapData = result.data
                if let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.routeCardData)
                }
            }
        }
    }

    func handleRouteFilterChange(_ filter: StopDetailsFilter?) {
        if let stopMapData {
            updateStopDetailsLayers(stopMapData, filter, nearbyVM.routeCardData)
        }
    }

    func clearSelectedStop() {
        stopMapData = nil
        resetDefaultSources()
    }

    func handleTapStopLayer(feature: FeaturesetFeature, _: InteractionContext) -> Bool {
        guard case let .string(stopId) = feature.properties[StopFeaturesBuilder.shared.propIdKey.key] else {
            let featureId = feature.id.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped, but had invalid stop id prop.
            """)
            return false
        }
        guard let stop = mapVM.globalData?.getStop(stopId: stopId) else {
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
        guard let tripId = vehicle.tripId else { return }
        guard case .stopDetails = nearbyVM.navigationStack.lastSafe() else { return }

        let routeCardData = nearbyVM.routeCardData
        let route = routeCardData?.first(where: { $0.lineOrRoute.containsRoute(routeId: vehicle.routeId) })
        let stop = route?.stopData.first
        let allTrips = stop?.data.flatMap(\.upcomingTrips)
        let trip = allTrips?.first(where: { upcoming in
            upcoming.trip.id == tripId
        })
        let stopSequence = trip?.stopSequence
        let routeId = trip?.trip.routeId ?? vehicle.routeId ?? route?.lineOrRoute.id

        if let routeId { analytics.tappedVehicle(routeId: routeId) }

        nearbyVM.navigationStack.lastTripDetailsFilter = .init(
            tripId: tripId,
            vehicleId: vehicle.id,
            stopSequence: stopSequence,
            selectionLock: true
        )
    }

    func handleSelectedVehicleChange(_ previousVehicle: Vehicle?, _ nextVehicle: Vehicle?) {
        guard let globalData = mapVM.globalData else { return }
        guard let stop = nearbyVM.getTargetStop(global: globalData) else { return }
        guard let nextVehicle else {
            viewportProvider.animateTo(coordinates: stop.coordinate)
            return
        }
        if previousVehicle == nil || previousVehicle?.id != nextVehicle.id {
            viewportProvider.vehicleOverview(vehicle: nextVehicle, stop: stop)
        }
    }
}
