//
//  NearbyViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Foundation
import os
import shared
import SwiftUI

class NearbyViewModel: ObservableObject {
    private static let logger = Logger()
    struct NearbyTransitState: Equatable {
        var loadedLocation: CLLocationCoordinate2D?
        var loading: Bool = false
        var nearbyByRouteAndStop: NearbyStaticData?
    }

    @Published var departures: StopDetailsDepartures?
    @Published var navigationStack: [SheetNavigationStackEntry] = [] {
        didSet { Task {
            let navEntry = navigationStack.lastSafe()
            do {
                switch navEntry {
                case let .legacyStopDetails(stop, _):
                    try await visitHistoryUsecase.addVisit(visit: .StopVisit(stopId: stop.id))
                default: break
                }
            } catch {
                let navLabel = navEntry.sheetItemIdentifiable().debugDescription
                Self.logger.warning("Failed to add to visit history for nav \(navLabel), \(error)")
            }
        }}
    }

    @Published var showDebugMessages: Bool = false
    @Published var combinedStopAndTrip: Bool = false

    @Published var alerts: AlertsStreamDataResponse?
    @Published var nearbyState = NearbyTransitState()
    @Published var selectingLocation = false
    @Published var tripHeadsignsEnabled = false

    private let alertsRepository: IAlertsRepository
    private let errorBannerRepository: IErrorBannerStateRepository
    private let nearbyRepository: INearbyRepository
    private let visitHistoryUsecase: VisitHistoryUsecase
    private var fetchNearbyTask: Task<Void, Never>?
    private var analytics: NearbyTransitAnalytics
    private let settingsRepository: ISettingsRepository

    init(
        departures: StopDetailsDepartures? = nil,
        navigationStack: [SheetNavigationStackEntry] = [],
        showDebugMessages: Bool = false,
        combinedStopAndTrip: Bool = false,
        alertsRepository: IAlertsRepository = RepositoryDI().alerts,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        nearbyRepository: INearbyRepository = RepositoryDI().nearby,
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
        analytics: NearbyTransitAnalytics = AnalyticsProvider.shared,
        settingsRepository: ISettingsRepository = RepositoryDI().settings
    ) {
        self.departures = departures
        self.navigationStack = navigationStack
        self.showDebugMessages = showDebugMessages
        self.combinedStopAndTrip = combinedStopAndTrip

        self.alertsRepository = alertsRepository
        self.errorBannerRepository = errorBannerRepository
        self.nearbyRepository = nearbyRepository
        self.visitHistoryUsecase = visitHistoryUsecase
        self.analytics = analytics
        self.settingsRepository = settingsRepository
    }

    @MainActor
    func loadDebugSetting() async {
        showDebugMessages = await (try? settingsRepository.getSettings()[.devDebugMode]?.boolValue) ?? false
        combinedStopAndTrip = await (try? settingsRepository.getSettings()[.combinedStopAndTrip]?.boolValue) ?? false
    }

    /**
     Set the departures from the given stop if it is the last stop in the stack.
     */
    func setDepartures(_ stopId: String, _ newDepartures: StopDetailsDepartures?) {
        if stopId == navigationStack.lastStop?.id {
            departures = newDepartures
        }
    }

    func isNearbyVisible() -> Bool {
        navigationStack.lastSafe() == .nearby
    }

    func pushNavEntry(_ entry: SheetNavigationStackEntry) {
        if case let .legacyStopDetails(stop, filter) = entry, combinedStopAndTrip {
            pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: filter, tripFilter: nil))
        } else if case let .legacyStopDetails(targetStop, _) = entry,
                  case let .legacyStopDetails(currentStop, _) = navigationStack.last,
                  targetStop == currentStop {
            _ = navigationStack.popLast()
            navigationStack.append(entry)
        } else if case let .stopDetails(stopId: targetStop, stopFilter: _, tripFilter: _) = entry,
                  case let .stopDetails(stopId: currentStop, stopFilter: _, tripFilter: _) = navigationStack.last,
                  targetStop == currentStop {
            _ = navigationStack.popLast()
            navigationStack.append(entry)
        } else {
            navigationStack.append(entry)
        }
    }

    /**
     set the filter for the given stop if it is the last stop in the stack
     */
    func setLastStopDetailsFilter(_ stopId: String, _ filter: StopDetailsFilter?) {
        if stopId == navigationStack.lastStopId {
            navigationStack.lastStopDetailsFilter = filter
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
            await fetchApi(
                errorBannerRepository,
                errorKey: "NearbyViewModel.getNearby",
                getData: { try await self.nearbyRepository.getNearby(global: global, location: location.coordinateKt) },
                onSuccess: {
                    self.nearbyState.nearbyByRouteAndStop = $0
                    self.nearbyState.loadedLocation = location
                },
                onRefreshAfterError: { self.getNearby(global: global, location: location) }
            )
        }
    }

    func getTargetStop(global: GlobalResponse) -> Stop? {
        switch navigationStack.last {
        case .nearby:
            nil
        case let .legacyStopDetails(stop, _):
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

    func loadTripHeadsigns() {
        Task {
            let result = try await settingsRepository.getSettings()[.tripHeadsigns]?.boolValue ?? false
            DispatchQueue.main.async { self.tripHeadsignsEnabled = result }
        }
    }
}

class NearbyNavigationStack: ObservableObject {}
