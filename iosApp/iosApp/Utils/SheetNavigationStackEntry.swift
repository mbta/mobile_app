//
//  SheetNavigationStackEntry.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-04.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

struct TripDetailsTarget: Hashable {
    let stopId: String
    let stopSequence: Int?
}

enum SheetNavigationStackEntry: Hashable, Identifiable {
    case alertDetails(alertId: String, line: Line?, routes: [Route]?, stop: Stop?)
    case editFavorites
    case favorites
    case more
    case nearby
    case routeDetails(SheetRoutes.RouteDetails)
    case routePicker(SheetRoutes.RoutePicker)
    case stopDetails(stopId: String, stopFilter: StopDetailsFilter?, tripFilter: TripDetailsFilter?)

    var id: Int {
        hashValue
    }

    var isEntrypoint: Bool {
        switch self {
        case .favorites, .more, .nearby: true
        default: false
        }
    }

    var allowTargeting: Bool {
        switch self {
        case .favorites, .nearby: true
        default: false
        }
    }

    var analyticsScreen: AnalyticsScreen? {
        switch self {
        case .favorites: .favorites
        case .more: .settings
        case .nearby: .nearbyTransit
        case .routeDetails: .routeDetails
        case let .stopDetails(_, stopFilter, _): stopFilter == nil ? .stopDetailsUnfiltered : .stopDetailsFiltered
        default: nil
        }
    }

    private var caseId: String {
        switch self {
        case .alertDetails: "alertDetails"
        case .editFavorites: "editFavorites"
        case .favorites: "favorites"
        case .more: "more"
        case .nearby: "nearby"
        case .routeDetails: "routeDetails"
        case .routePicker: "routePicker"
        case .stopDetails: "stopDetails"
        }
    }

    func hasSamePage(as otherEntry: Self) -> Bool {
        caseId == otherEntry.caseId
    }

    func stopId() -> String? {
        switch self {
        case let .stopDetails(stopId, _, _): stopId
        case _: nil
        }
    }

    func sheetItemIdentifiable() -> SheetItem? {
        let item = SheetItem(stackEntry: self)
        return item.id == "" ? nil : item
    }

    func coverItemIdentifiable() -> NearbyCoverItem? {
        let item = NearbyCoverItem(stackEntry: self)
        return item.id == "" ? nil : item
    }
}

/// Struct that holds a SheetNavigationStackEntry. To be used with `sheet(item:onDismiss:content:)`
/// the ids of two `SheetItem`s will differ only if a new sheet should be opened when switching from one to
/// the other.
/// For example, if switching between two `.stopDetails` entries, a new sheet should only be opened if the stop ID
/// changes.
struct SheetItem: Identifiable {
    let stackEntry: SheetNavigationStackEntry

    var id: String {
        switch stackEntry {
        case .favorites: "favorites"
        case .editFavorites: "editFavorites"
        case .nearby: "nearby"
        case .routeDetails: "routeDetails"
        case .routePicker: "routePicker"
        case let .stopDetails(stopId, _, _): stopId
        default: ""
        }
    }
}

struct NearbyCoverItem: Identifiable {
    let stackEntry: SheetNavigationStackEntry

    var id: String {
        switch stackEntry {
        case let .alertDetails(alertId, _, _, _): alertId
        case .more: "more"
        default: ""
        }
    }
}

extension [SheetNavigationStackEntry] {
    /// Retrieves and updates the bottom-most ``StopDetailsFilter`` in the navigation stack.
    ///
    /// Implemented as an extension property so that
    /// [`Binding.subscript(dynamicMember:)`][binding-subscript]
    /// can automatically create a binding to the filter based on the binding to the stack.
    ///
    /// [binding-subscript]: https://developer.apple.com/documentation/swiftui/binding/subscript(dynamicmember:)
    var lastStopDetailsFilter: StopDetailsFilter? {
        get {
            switch self.last {
            case let .stopDetails(stopId: _, stopFilter: filter, tripFilter: _): filter
            case _: nil
            }
        }
        set {
            if case let .stopDetails(
                stopId: stopId,
                stopFilter: lastFilter,
                tripFilter: _
            ) = self.last {
                if StopDetailsFilter.companion.shouldPopLastStopEntry(lastFilter: lastFilter, newFilter: newValue) {
                    _ = self.popLast()
                }
                if self.shouldSkipStopFilterUpdate(newStop: stopId, newFilter: newValue) { return }
                self.append(.stopDetails(stopId: stopId, stopFilter: newValue, tripFilter: nil))
            }
        }
    }

    var lastTripDetailsFilter: TripDetailsFilter? {
        get {
            switch self.last {
            case let .stopDetails(stopId: _, stopFilter: _, tripFilter: filter): filter
            default: nil
            }
        }
        set {
            if case let .stopDetails(stopId: stopId, stopFilter: stopFilter, tripFilter: _) = self.last {
                _ = self.popLast()
                self.append(.stopDetails(stopId: stopId, stopFilter: stopFilter, tripFilter: newValue))
            }
        }
    }

    var lastStopId: String? {
        let lastStopEntry: SheetNavigationStackEntry? = self.last { entry in
            switch entry {
            case .stopDetails: true
            default: false
            }
        }
        return switch lastStopEntry {
        case let .stopDetails(stopId: id, stopFilter: _, tripFilter: _): id
        default: nil
        }
    }

    func lastSafe() -> SheetNavigationStackEntry {
        self.last ?? .nearby
    }

    func shouldSkipStopFilterUpdate(newStop: String, newFilter: StopDetailsFilter?) -> Bool {
        // If the new filter is nil and there is already a nil filter in the stack for the same stop ID,
        // we don't want a duplicate unfiltered entry, so skip appending a new one
        if case let .stopDetails(
            stopId: lastStop,
            stopFilter: lastFilter,
            tripFilter: _
        ) = self.last {
            lastStop == newStop && newFilter == nil && lastFilter == nil
        } else {
            false
        }
    }
}
