//
//  NearbyViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import os
import Shared
import SwiftUI

class NearbyViewModel: ObservableObject {
    private static let logger = Logger()
    struct NearbyTransitState: Equatable {
        var loadedLocation: CLLocationCoordinate2D?
        var loading: Bool = false
        var stopIds: [String]?
    }

    @Published var navigationStack: [SheetNavigationStackEntry] = [] {
        didSet { Task {
            let navEntry = navigationStack.lastSafe()
            do {
                switch navEntry {
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

    @Published var alerts: AlertsStreamDataResponse?
    @Published var nearbyState = NearbyTransitState()
    @Published var routeCardData: [RouteCardData]?

    @Published var isTargeting = false
    // Distinct from nearbyState.loadedLocation because it's also set when nearby favorites load
    @Published var lastLoadedLocation: CLLocationCoordinate2D?

    private let alertsUsecase: AlertsUsecase
    private let errorBannerRepository: IErrorBannerStateRepository
    private let nearbyRepository: INearbyRepository
    private let visitHistoryUsecase: VisitHistoryUsecase
    private var fetchNearbyTask: Task<Void, Never>?
    private var analytics: Analytics

    init(
        routeCardData: [RouteCardData]? = nil,
        navigationStack: [SheetNavigationStackEntry] = [],
        alertsUsecase: AlertsUsecase = UsecaseDI().alertsUsecase,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        nearbyRepository: INearbyRepository = RepositoryDI().nearby,
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
        analytics: Analytics = AnalyticsProvider.shared
    ) {
        self.routeCardData = routeCardData
        self.navigationStack = navigationStack

        self.alertsUsecase = alertsUsecase
        self.errorBannerRepository = errorBannerRepository
        self.nearbyRepository = nearbyRepository
        self.visitHistoryUsecase = visitHistoryUsecase
        self.analytics = analytics
    }

    func clearNearbyData() {
        nearbyState = .init()
        routeCardData = nil
    }

    /**
     Set the route card data from the given stop if it is the last stop in the stack.
     */
    func setRouteCardData(_ stopId: String, _ newRouteCardData: [RouteCardData]?) {
        if stopId == navigationStack.lastStopId {
            routeCardData = newRouteCardData
        }
    }

    func isNearbyVisible() -> Bool {
        navigationStack.lastSafe() == .nearby
    }

    func isFavoritesVisible() -> Bool {
        navigationStack.lastSafe() == .favorites
    }

    /*
     Directly append the given entry to the stack without considering the previous entries.
     This should be done with caution as it can result in multiple entries of the same type within the stack,
     typically `pushNavEntry` should be used instead.
     */
    func appendNavEntry(_ entry: SheetNavigationStackEntry) {
        if entry != navigationStack.lastSafe() {
            navigationStack.append(entry)
        }
    }

    func popToEntrypoint() {
        while !navigationStack.lastSafe().isEntrypoint {
            navigationStack.popLast()
        }
    }

    // Adding a second bool argument here is a hack until we can remove the feature flag and set the new stop details
    // entry directly, until then, we need a way to distinguish between entries coming from the map or not.
    /**
     Updates the stack so that the given entry is the last entry.
     Optionally pops the previous entry to prevent the stack from building too deep. For example, when pushing
     a `stopDetails` entry for a new stop on top of a `stopDetails` entry for a different stop, the previous entry
     would be popped to ensure there is only one `stopDetails` entry in the stack.
     */
    func pushNavEntry(_ entry: SheetNavigationStackEntry, mapSelection _: Bool = false) {
        if entry.isEntrypoint {
            navigationStack.removeAll()
        }
        let currentEntry = navigationStack.lastSafe()
        if case let .stopDetails(
            stopId: targetStop,
            stopFilter: newFilter,
            tripFilter: _
        ) = entry,
            case let .stopDetails(
                stopId: lastStop,
                stopFilter: lastFilter,
                tripFilter: _
            ) = currentEntry,
            targetStop == lastStop {
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the unfiltered (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if StopDetailsFilter.companion.shouldPopLastStopEntry(lastFilter: lastFilter, newFilter: newFilter) {
                _ = navigationStack.popLast()
            }
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

    func getNearbyStops(global: GlobalResponse, location: CLLocationCoordinate2D) {
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

            let stopIds = nearbyRepository.getStopIdsNearby(
                global: global,
                location: location.positionKt
            )
            lastLoadedLocation = location
            nearbyState.stopIds = stopIds
            nearbyState.loadedLocation = location
            nearbyState.loading = false
            isTargeting = false
        }
    }

    func loadRouteCardData(
        state: NearbyTransitState,
        global: GlobalResponse?,
        schedules: ScheduleResponse?,
        predictions: PredictionsByStopJoinResponse?,
        alerts: AlertsStreamDataResponse?,
        now: Date,
    ) {
        Task {
            guard let global, let stopIds = state.stopIds, let location = state.loadedLocation else {
                Task { @MainActor in routeCardData = nil }
                return
            }
            let cardData = try await RouteCardData.companion.routeCardsForStopList(
                stopIds: stopIds,
                globalData: global,
                sortByDistanceFrom: location.positionKt,
                schedules: schedules,
                predictions: predictions?.toPredictionsStreamDataResponse(),
                alerts: alerts,
                now: now.toEasternInstant(),
                context: .nearbyTransit
            )
            Task { @MainActor in routeCardData = cardData }
        }
    }

    func getTargetStop(global: GlobalResponse) -> Stop? {
        switch navigationStack.last {
        case .nearby: nil
        case let .stopDetails(stopId: stopId, _, _): global.getStop(stopId: stopId)
        default: nil
        }
    }

    func joinAlertsChannel() {
        alertsUsecase.connect { outcome in
            DispatchQueue.main.async { [weak self] in
                if case let .ok(result) = onEnum(of: outcome) {
                    self?.alerts = result.data
                }
            }
        }
    }

    func leaveAlertsChannel() {
        alertsUsecase.disconnect()
    }
}

class NearbyNavigationStack: ObservableObject {}
