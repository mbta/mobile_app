//
//  SheetNavigationStackEntry.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-04.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

struct TripDetailsTarget: Hashable {
    let stopId: String
    let stopSequence: Int?
}

enum SheetNavigationStackEntry: Hashable, Identifiable {
    case stopDetails(stopId: String, stopFilter: StopDetailsFilter?, tripFilter: TripDetailsFilter?)
    case nearby
    case alertDetails(alertId: String, line: Line?, routes: [Route]?, stop: Stop?)

    var id: Int {
        hashValue
    }

    private var caseId: String {
        switch self {
        case .stopDetails: "stopDetails"
        case .nearby: "nearby"
        case .alertDetails: "alertDetails"
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

    func sheetItemIdentifiable() -> NearbySheetItem? {
        let item = NearbySheetItem(stackEntry: self)
        return item.id == "" ? nil : item
    }

    func coverItemIdentifiable() -> NearbyCoverItem? {
        let item = NearbyCoverItem(stackEntry: self)
        return item.id == "" ? nil : item
    }
}

/// Struct that holds a SheetNavigationStackEntry. To be used with `sheet(item:onDismiss:content:)`
/// the ids of two `NearbySheetItem`s will differ only if a new sheet should be opened when switching from one to
/// the other.
/// For example, if switching between two `.stopDetails` entries, a new sheet should only be opened if the stop ID
/// changes.
struct NearbySheetItem: Identifiable {
    let stackEntry: SheetNavigationStackEntry

    var id: String {
        switch stackEntry {
        case let .stopDetails(stopId, _, _): stopId
        case .nearby: "nearby"
        default: ""
        }
    }
}

struct NearbyCoverItem: Identifiable {
    let stackEntry: SheetNavigationStackEntry

    var id: String {
        switch stackEntry {
        case let .alertDetails(alertId, _, _, _): alertId
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
