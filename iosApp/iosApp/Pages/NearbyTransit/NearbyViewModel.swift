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
            do {
                let response = try await nearbyRepository.getNearby(
                    global: global,
                    location: location.coordinateKt
                )
                try Task.checkCancellation()
                nearbyState.nearbyByRouteAndStop = response
                nearbyState.loadedLocation = location
                nearbyState.error = nil
            } catch is CancellationError {
                // Do nothing when cancelled
                return
            } catch let error as NSError {
                withUnsafeCurrentTask { thisTask in
                    if self.fetchNearbyTask?.hashValue == thisTask?.hashValue {
                        self.nearbyState.error = self.nearbyErrorText(error: error)
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

    func nearbyErrorText(error: NSError) -> String {
        switch error.kotlinException {
        case is Ktor_client_coreHttpRequestTimeoutException:
            "Couldn't load nearby transit, no response from the server"
        case is Ktor_ioIOException:
            "Couldn't load nearby transit, connection was interrupted"
        case is Ktor_serializationJsonConvertException:
            "Couldn't load nearby transit, unable to parse response"
        case is Ktor_client_coreResponseException:
            "Couldn't load nearby transit, invalid response"
        default:
            "Couldn't load nearby transit, something went wrong"
        }
    }

    func joinAlertsChannel() {
        alertsRepository.connect { outcome in
            DispatchQueue.main.async { [weak self] in
                if let data = outcome.data {
                    self?.alerts = data
                }
            }
        }
    }

    func leaveAlertsChannel() {
        alertsRepository.disconnect()
    }
}

class NearbyNavigationStack: ObservableObject {}
