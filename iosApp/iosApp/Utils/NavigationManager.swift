//
//  NavigationManager.swift
//  iosApp
//
//  Created by esimon on 6/16/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import CoreLocation
import os
import Shared
import SwiftUI

class NavigationManager: ObservableObject {
    private static let logger = Logger()

    @Published var navigationStack: [SheetNavigationStackEntry] = [] {
        didSet { Task {
            let navEntry = navigationStack.lastSafe()
            do {
                switch navEntry {
                case let .stopOverview(stopId: stopId):
                    try await visitHistoryUsecase.addVisit(visit: .StopVisit(stopId: stopId))
                case let .routeStop(stopId: stopId, routeStopFilter: _, tripFilter: _):
                    try await visitHistoryUsecase.addVisit(visit: .StopVisit(stopId: stopId))
                default: break
                }
            } catch {
                let navLabel = navEntry.sheetItemIdentifiable().debugDescription
                Self.logger.warning("Failed to add to visit history for nav \(navLabel), \(error)")
            }
        }}
    }

    private let visitHistoryUsecase: VisitHistoryUsecase

    init(
        navigationStack: [SheetNavigationStackEntry] = [],
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
    ) {
        self.navigationStack = navigationStack
        self.visitHistoryUsecase = visitHistoryUsecase
    }

    func isNearbyVisible() -> Bool {
        navigationStack.lastSafe() == .nearby
    }

    func isFavoritesVisible() -> Bool {
        navigationStack.lastSafe() == .favorites
    }

    /**
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

    /**
     Updates the stack so that the given entry is the last entry.
     Optionally pops the previous entry to prevent the stack from building too deep. For example, when pushing
     a `stopOverview`/`routeStop` entry for a new stop on top of an entry for a different stop, the previous entry
     would be popped to ensure there is only one `stopOverview`/`routeStop` entry in the stack.
     */
    func pushNavEntry(_ entry: SheetNavigationStackEntry) {
        if entry.isEntrypoint {
            navigationStack.removeAll()
        }
        let currentEntry = navigationStack.lastSafe()
        switch (entry, currentEntry) {
        case let (.routeStop(_, newFilter, _), .routeStop(_, lastFilter, _)):
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the overview (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if RouteStopFilter.companion.shouldPopLastStopEntry(lastFilter: lastFilter, newFilter: newFilter) {
                _ = navigationStack.popLast()
            }
            navigationStack.append(entry)
        case let (.routeStop(_, newFilter, _), .stopOverview(_)):
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the overview (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if RouteStopFilter.companion.shouldPopLastStopEntry(lastFilter: nil, newFilter: newFilter) {
                _ = navigationStack.popLast()
            }
            navigationStack.append(entry)
        case let (.stopOverview(_), .routeStop(_, lastFilter, _)):
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the overview (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if RouteStopFilter.companion.shouldPopLastStopEntry(lastFilter: lastFilter, newFilter: nil) {
                _ = navigationStack.popLast()
            }
            navigationStack.append(entry)
        case let (.stopOverview(targetStop), .stopOverview(lastStop)):
            // When the stop filter changes, we want a new entry to be added (i.e. no pop) only when
            // you're on the overview (lastFilter == nil) page, but if there is already a filter,
            // the entry with the old filter should be popped and replaced with the new value.
            if RouteStopFilter.companion.shouldPopLastStopEntry(lastFilter: nil, newFilter: nil) {
                _ = navigationStack.popLast()
            }
            if targetStop == lastStop { return }
            navigationStack.append(entry)
        case (.tripDetails, .tripDetails):
            _ = navigationStack.popLast()
            navigationStack.append(entry)
        default:
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
    func setLastStopOverviewPageFilter(_ filters: StopOverviewPageFilters) {
        if filters.stopId == navigationStack.lastStopId,
           navigationStack.lastRouteStopFilter != nil || navigationStack
           .lastTripDetailsFilter != nil {
            navigationStack.lastStopOverviewPageFilters = filters
        }
    }

    /**
     set the filter for the given stop if it is the last stop in the stack
     */
    func setLastRouteStopPageFilter(_ filters: RouteStopPageFilters) {
        if filters.stopId == navigationStack.lastStopId,
           navigationStack.lastRouteStopFilter != filters.routeStopFilter || navigationStack
           .lastTripDetailsFilter != filters.tripFilter {
            navigationStack.lastRouteStopPageFilters = filters
        }
    }

    /**
     set the filter for the given stop if it is the last stop in the stack
     */
    func setLastRouteStopFilter(_ stopId: String, _ filter: RouteStopFilter?) {
        if stopId == navigationStack.lastStopId, navigationStack.lastRouteStopFilter != filter {
            navigationStack.lastRouteStopFilter = filter
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

    func getTargetStop(global: GlobalResponse) -> Stop? {
        switch navigationStack.last {
        case .nearby: nil
        case let .stopOverview(stopId: stopId): global.getStop(stopId: stopId)
        case let .routeStop(stopId: stopId, _, _): global.getStop(stopId: stopId)
        default: nil
        }
    }
}
