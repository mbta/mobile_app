//
//  NearbyViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Foundation
import shared
import SwiftUI

class NearbyViewModel: ObservableObject {
    struct NearbyTransitState: Equatable {
        var error: String?
        var loadedLocation: CLLocationCoordinate2D?
        var loading: Bool = false
        var nearbyByRouteAndStop: NearbyStaticData?
    }

    @Published var departures: StopDetailsDepartures?
    @Published var navigationStack: [SheetNavigationStackEntry] = []
    @Published var alerts: AlertsStreamDataResponse?
    @Published var nearbyState = NearbyTransitState()
    @Published var selectingLocation = false
    private let alertsRepository: IAlertsRepository
    private let nearbyRepository: INearbyRepository
    private var fetchNearbyTask: Task<Void, Never>?
    private var analytics: NearbyTransitAnalytics

    init(
        departures: StopDetailsDepartures? = nil,
        navigationStack: [SheetNavigationStackEntry] = [],
        alertsRepository: IAlertsRepository = RepositoryDI().alerts,
        nearbyRepository: INearbyRepository = RepositoryDI().nearby,
        analytics: NearbyTransitAnalytics = AnalyticsProvider.shared
    ) {
        self.departures = departures
        self.navigationStack = navigationStack
        self.alertsRepository = alertsRepository
        self.nearbyRepository = nearbyRepository
        self.analytics = analytics
    }

    func setDepartures(_ newDepartures: StopDetailsDepartures?) {
        departures = newDepartures
    }

    func isNearbyVisible() -> Bool {
        navigationStack.lastSafe() == .nearby
    }

    func pushNavEntry(_ entry: SheetNavigationStackEntry) {
        if case let .stopDetails(targetStop, _) = entry,
           case let .stopDetails(currentStop, _) = navigationStack.last,
           targetStop == currentStop {
            _ = navigationStack.popLast()
            navigationStack.append(entry)
        } else {
            navigationStack.append(entry)
        }
    }

    func goBack() {
        _ = navigationStack.popLast()
    }

    func getNearby(global: GlobalResponse, location: CLLocationCoordinate2D) {
        guard !location.isRoughlyEqualTo(nearbyState.loadedLocation) else {
            return
        }
        if nearbyState.loading, let fetchNearbyTask, !fetchNearbyTask.isCancelled {
            fetchNearbyTask.cancel()
        }
        fetchNearbyTask = Task { @MainActor [weak self] in
            guard let self else { return }
            if nearbyState.loadedLocation != nil {
                analytics.refetchedNearbyTransit()
            }
            nearbyState.loading = true
            defer {
                self.nearbyState.loading = false
                self.selectingLocation = false
            }
            switch await callApi({
                try await nearbyRepository.getNearby(global: global, location: location.coordinateKt) }) {
            case let .ok(data):
                if Task.isCancelled { return }
                nearbyState.nearbyByRouteAndStop = data.data
                nearbyState.loadedLocation = location
                nearbyState.error = nil
            case let .error(error):
                withUnsafeCurrentTask { thisTask in
                    if self.fetchNearbyTask?.hashValue == thisTask?.hashValue {
                        self.nearbyState.error = error.message
                    }
                }
            }
        }
    }

    func getTargetStop(global: GlobalResponse) -> Stop? {
        switch navigationStack.last {
        case .nearby:
            nil
        case let .stopDetails(stop, _):
            stop
        case let .tripDetails(tripId: _, vehicleId: _, target: target, routeId: _, directionId: _):
            target != nil ? global.stops[target!.stopId] : nil
        default:
            nil
        }
    }

    func joinAlertsChannel() {
        alertsRepository.connect { outcome in
            DispatchQueue.main.async { [weak self] in
                if case let .ok(result) = onEnum(of: outcome) {
                    self?.alerts = result.data
                }
            }
        }
    }

    func leaveAlertsChannel() {
        alertsRepository.disconnect()
    }
}

class NearbyNavigationStack: ObservableObject {}
