//
//  SheetNavigationStackEntry.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-04.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

struct StopDetailsFilter: Hashable {
    let routeId: String
    let directionId: Int32
}

struct TripDetailsTarget: Hashable {
    let stopId: String
    let stopSequence: Int
}

enum SheetNavigationStackEntry: Hashable, Identifiable {
    case stopDetails(Stop, StopDetailsFilter?)
    case tripDetails(tripId: String, vehicleId: String, target: TripDetailsTarget?, routeId: String, directionId: Int32)
    case nearby

    var id: Int {
        hashValue
    }

    func stop() -> Stop? {
        switch self {
        case let .stopDetails(stop, _): stop
        case _: nil
        }
    }

    func sheetItemIdentifiable() -> NearbySheetItem {
        NearbySheetItem(stackEntry: self)
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
        case let .stopDetails(stop, _): stop.id
        case let .tripDetails(tripId, _, _, _, _): tripId
        case .nearby: "nearby"
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
            case let .stopDetails(_, filter): filter
            case _: nil
            }
        }
        set {
            if case let .stopDetails(stop, _) = self.last {
                _ = self.popLast()
                self.append(.stopDetails(stop, newValue))
            }
        }
    }

    func lastSafe() -> SheetNavigationStackEntry {
        self.last ?? .nearby
    }
}
