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

    /// Adding a second bool argument here is a hack until we can remove the feature flag and set the new stop details
    /// entry directly, until then, we need a way to distinguish between entries coming from the map or not.
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
        } else if case .tripDetails = currentEntry, case .tripDetails = entry {
            _ = navigationStack.popLast()
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
    func setLastStopDetailsPageFilter(_ filters: StopDetailsPageFilters) {
        if filters.stopId == navigationStack.lastStopId,
           navigationStack.lastStopDetailsFilter != filters.stopFilter || navigationStack
           .lastTripDetailsFilter != filters.tripFilter {
            navigationStack.lastStopDetailsPageFilters = filters
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

    func getTargetStop(global: GlobalResponse) -> Stop? {
        switch navigationStack.last {
        case .nearby: nil
        case let .stopDetails(stopId: stopId, _, _): global.getStop(stopId: stopId)
        default: nil
        }
    }
}
