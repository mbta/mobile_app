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
                case let .stopDetails(stopId: stopId, stopFilter: _, tripFilter: _):
                    try await visitHistoryUsecase.addVisit(visit: .StopVisit(stopId: stopId))
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
    private var analytics: Analytics
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
        analytics: Analytics = AnalyticsProvider.shared,
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
        if stopId == navigationStack.lastStopId {
            departures = newDepartures
        }
    }

    func isNearbyVisible() -> Bool {
        navigationStack.lastSafe() == .nearby
    }

    // Adding a second bool argument here is a hack until we can remove the feature flag and set the new stop details
    // entry directly, until then, we need a way to distinguish between entries coming from the map or not.
    func pushNavEntry(_ entry: SheetNavigationStackEntry, mapSelection: Bool = false) {
        let currentEntry = navigationStack.lastSafe()
        if case let .legacyStopDetails(stop, filter) = entry, combinedStopAndTrip {
            pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: filter, tripFilter: nil))
        } else if case let .tripDetails(tripId, vehicleId, target, _, _) = entry,
                  combinedStopAndTrip,
                  let stopId = navigationStack.lastStopId,
                  let stopFilter = navigationStack.lastStopDetailsFilter {
            let stopSequence: KotlinInt? = if let sequence = target?.stopSequence { KotlinInt(int: Int32(sequence)) }
            else { nil }
            let tripFilter: TripDetailsFilter = .init(
                tripId: tripId,
                vehicleId: vehicleId,
                stopSequence: stopSequence,
                selectionLock: mapSelection
            )
            pushNavEntry(.stopDetails(stopId: stopId, stopFilter: stopFilter, tripFilter: tripFilter))
        } else if case let .legacyStopDetails(targetStop, _) = entry,
                  case let .legacyStopDetails(lastStop, _) = navigationStack.last,
                  targetStop == lastStop {
            _ = navigationStack.popLast()
            navigationStack.append(entry)
        } else if
            case let .stopDetails(
                stopId: targetStop,
                stopFilter: newFilter,
                tripFilter: _
            ) = entry,
            case let .stopDetails(
                stopId: lastStop,
                stopFilter: lastFilter,
                tripFilter: _
            ) = navigationStack.last,
            targetStop == lastStop {
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the unfiltered (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if lastFilter != nil { _ = navigationStack.popLast() }
            if navigationStack.shouldSkipStopFilterUpdate(newStop: targetStop, newFilter: newFilter) { return }
            navigationStack.append(entry)
        } else {
            navigationStack.append(entry)
        }

        if !entry.hasSamePage(as: currentEntry) {
            if #available(iOS 17, *) {
                AccessibilityNotification.ScreenChanged().post()
            } else {
                UIAccessibility.post(
                    notification: .screenChanged,
                    argument: nil
                )
            }
        }
    }

    /**
     set the filter for the given stop if it is the last stop in the stack
     */
    func setLastStopDetailsFilter(_ stopId: String, _ filter: StopDetailsFilter?) {
        if stopId == navigationStack.lastStopId, navigationStack.lastStopDetailsFilter != filter {
            navigationStack.lastStopDetailsFilter = filter
        }
    }

    /**
     set the filter for the given stop if it is the last stop in the stack
     */
    func setLastTripDetailsFilter(_ stopId: String, _ filter: TripDetailsFilter?) {
        if stopId == navigationStack.lastStopId, navigationStack.lastTripDetailsFilter != filter {
            navigationStack.lastTripDetailsFilter = filter
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
                getData: { try await self.nearbyRepository.getNearby(global: global, location: location.positionKt) },
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
        case let .stopDetails(stopId: stopId, _, _):
            global.stops[stopId]
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
