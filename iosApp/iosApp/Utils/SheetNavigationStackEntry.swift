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
    case more(highlight: MoreSection.Category?)
    case nearby
    case routeDetails(SheetRoutes.RouteDetails)
    case routePicker(SheetRoutes.RoutePicker)
    case saveFavorite(routeId: LineOrRoute.Id, stopId: String, selectedDirection: Int32, context: EditFavoritesContext)
    case stopDetails(stopId: String, routeStopFilter: RouteStopFilter?, tripFilter: TripDetailsFilter?)
    case tripDetails(filter: TripDetailsPageFilter)

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
        toSheetRoute()?.allowTargeting ?? false
    }

    var showCurrentLocation: Bool {
        toSheetRoute()?.showCurrentLocation ?? true
    }

    var analyticsScreen: AnalyticsScreen? {
        switch self {
        case .favorites: .favorites
        case .more: .settings
        case .nearby: .nearbyTransit
        case .routeDetails: .routeDetails
        case let .stopDetails(_, routeStopFilter, _): routeStopFilter == nil ? .stopSummary : .routeStop
        case .tripDetails: .tripDetails
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
        case .saveFavorite: "saveFavorite"
        case .stopDetails: "stopDetails"
        case .tripDetails: "tripDetails"
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

    func vehicleId() -> String? {
        switch self {
        case let .stopDetails(_, _, tripFilter): tripFilter?.vehicleId
        case let .tripDetails(filter): filter.vehicleId
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

    /**
     Convert this SheetNavigationStackEntry into the shared SheetRoute type. SheetRoute represents only routes displayed
     in a MapSheetPage. Any other nav stack entries that are displayed as full screen modals (like alertDetails
     and more) do not have a corresponding SheetRoute, and therefore return nil.
     */
    func toSheetRoute() -> SheetRoutes? {
        switch self {
        case .nearby: SheetRoutes.NearbyTransit()
        case .favorites: SheetRoutes.Favorites()
        case .editFavorites: SheetRoutes.EditFavorites()
        case let .stopDetails(stopId, routeStopFilter, tripFilter): SheetRoutes.StopDetails(
                stopId: stopId,
                routeStopFilter: routeStopFilter,
                tripFilter: tripFilter
            )
        case let .routeDetails(sheetRoute): sheetRoute
        case let .routePicker(sheetRoute): sheetRoute
        case let .tripDetails(filter): SheetRoutes.TripDetails(filter: filter)
        case .alertDetails: nil
        case .more: nil
        case .saveFavorite: nil
        }
    }

    static func fromSheetRoute(_ sheetRoute: SheetRoutes) -> Self {
        switch onEnum(of: sheetRoute) {
        case .nearbyTransit: .nearby
        case .favorites: .favorites
        case .editFavorites: .editFavorites
        case let .stopDetails(sheetRoute): .stopDetails(
                stopId: sheetRoute.stopId,
                routeStopFilter: sheetRoute.routeStopFilter,
                tripFilter: sheetRoute.tripFilter
            )
        case let .routeDetails(sheetRoute): .routeDetails(sheetRoute)
        case let .routePicker(sheetRoute): .routePicker(sheetRoute)
        case let .tripDetails(sheetRoute): .tripDetails(filter: sheetRoute.filter)
        }
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
        case let .tripDetails(filter): filter.tripId
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
        case let .saveFavorite(routeId, stopId, selectedDirection, _): "\(routeId.idText)-\(stopId)-\(selectedDirection)"
        default: ""
        }
    }
}

extension [SheetNavigationStackEntry] {
    /// Retrieves and updates the bottom-most ``RouteStopFilter`` in the navigation stack.
    ///
    /// Implemented as an extension property so that
    /// [`Binding.subscript(dynamicMember:)`][binding-subscript]
    /// can automatically create a binding to the filter based on the binding to the stack.
    ///
    /// [binding-subscript]: https://developer.apple.com/documentation/swiftui/binding/subscript(dynamicmember:)
    var lastRouteStopFilter: RouteStopFilter? {
        get {
            switch self.last {
            case let .stopDetails(stopId: _, routeStopFilter: filter, tripFilter: _): filter
            case _: nil
            }
        }
        set {
            if case let .stopDetails(
                stopId: stopId,
                routeStopFilter: lastFilter,
                tripFilter: _
            ) = self.last {
                if RouteStopFilter.companion.shouldPopLastStopEntry(lastFilter: lastFilter, newFilter: newValue) {
                    _ = self.popLast()
                }
                if self.shouldSkipRouteStopFilterUpdate(newStop: stopId, newFilter: newValue) { return }
                self.append(.stopDetails(stopId: stopId, routeStopFilter: newValue, tripFilter: nil))
            }
        }
    }

    var lastTripDetailsFilter: TripDetailsFilter? {
        get {
            switch self.last {
            case let .stopDetails(stopId: _, routeStopFilter: _, tripFilter: filter): filter
            default: nil
            }
        }
        set {
            if case let .stopDetails(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: _) = self.last {
                _ = self.popLast()
                self.append(.stopDetails(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: newValue))
            }
        }
    }

    var lastStopDetailsPageFilters: StopDetailsPageFilters? {
        get {
            switch self.last {
            case let .stopDetails(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: tripFilter):
                .init(stopId: stopId, routeStopFilter: routeStopFilter, tripFilter: tripFilter)
            case _: nil
            }
        }
        set {
            if case let .stopDetails(
                stopId: stopId,
                routeStopFilter: lastRouteStopFilter,
                tripFilter: lastTripFilter,
            ) = self.last {
                let shouldPop = RouteStopFilter.companion.shouldPopLastStopEntry(
                    lastFilter: lastRouteStopFilter,
                    newFilter: newValue?.routeStopFilter
                )
                if shouldPop || lastTripFilter != newValue?.tripFilter {
                    _ = self.popLast()
                }
                if let newValue {
                    let shouldSkip = shouldSkipRouteStopFilterUpdate(
                        newStop: newValue.stopId,
                        newFilter: newValue.routeStopFilter
                    )
                    if shouldSkip, lastTripFilter == newValue.tripFilter { return }
                    self.append(.stopDetails(
                        stopId: newValue.stopId,
                        routeStopFilter: newValue.routeStopFilter,
                        tripFilter: newValue.tripFilter
                    ))
                }
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
        case let .stopDetails(stopId: id, routeStopFilter: _, tripFilter: _): id
        default: nil
        }
    }

    func lastSafe() -> SheetNavigationStackEntry {
        self.last ?? .nearby
    }

    func shouldSkipRouteStopFilterUpdate(newStop: String, newFilter: RouteStopFilter?) -> Bool {
        // If the new filter is nil and there is already a nil filter in the stack for the same stop ID,
        // we don't want a duplicate unfiltered entry, so skip appending a new one
        if case let .stopDetails(
            stopId: lastStop,
            routeStopFilter: lastFilter,
            tripFilter: _
        ) = self.last {
            lastStop == newStop && newFilter == nil && lastFilter == nil
        } else {
            false
        }
    }

    func hasFloatingBackButton() -> Bool {
        switch self.lastSafe() {
        case .alertDetails, .editFavorites, .favorites, .more, .nearby, .routeDetails, .routePicker,
             .saveFavorite: false
        case .stopDetails, .tripDetails: true
        }
    }
}
