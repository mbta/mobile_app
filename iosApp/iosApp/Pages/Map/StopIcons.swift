//
//  StopIcons.swift
//  iosApp
//
//  Created by Simon, Emma on 4/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

enum StopIcons {
    static let stopIconPrefix = "map-stop-"
    static let stopZoomClosePrefix = "close-"
    static let stopZoomWidePrefix = "wide-"

    static let stopContainerPrefix = "\(stopIconPrefix)container-"
    static let stopTransferSuffix = "-transfer"
    static let stopTerminalSuffix = "-terminal"

    // This is a single transparent pixel, used to create a layer just for tap target padding
    static let stopDummyIcon = "\(stopIconPrefix)dummy-tap-pixel"

    static let stopPinIcon = "\(stopIconPrefix)pin"

    static let all: [String] = basicStopIcons + specialCaseStopIcons + stopContainerIcons + [stopDummyIcon, stopPinIcon]

    static let basicStopIcons = MapStopRoute.allCases.flatMap { routeType in atZooms(stopIconPrefix, routeType.name) }
    static let specialCaseStopIcons = terminalIcons + branchIcons + atZooms(
        stopIconPrefix,
        MapStopRoute.bus.name + stopTransferSuffix
    )
    static let stopContainerIcons = ["2", "3"].flatMap { memberCount in atZooms(stopContainerPrefix, memberCount) }

    static let terminalIcons: [String] = MapStopRoute.allCases.filter { !$0.hasBranchingTerminals }.map { routeType in
        "\(stopIconPrefix)\(stopZoomWidePrefix)\(routeType.name)\(stopTerminalSuffix)"
    } + MapStopRoute.allCases.filter(\.hasBranchingTerminals).flatMap { routeType in
        routeType.branchingRoutes.map { routeId in
            "\(stopIconPrefix)\(stopZoomWidePrefix)\(routeType.name)\(stopTerminalSuffix)-\(routeId)"
        }
    } + ["\(stopIconPrefix)\(stopZoomWidePrefix)\(MapStopRoute.silver.name)\(stopTerminalSuffix)"]

    static let branchIcons: [String] = MapStopRoute.allCases.filter(\.hasBranchingTerminals).flatMap { routeType in
        routeType.branchingRoutes.map { routeId in
            "\(stopIconPrefix)\(stopZoomClosePrefix)\(routeType.name)-\(routeId)"
        }
    }

    // If this is a branching route, return a suffix to specify a distinct icon for it
    private static let branchingRouteSuffixExp = Exp(.switchCase) {
        MapExp.shared.branchedRouteExp.toMapbox()
        Exp(.concat) {
            "-"
            Exp(.at) {
                0
                Exp(.get) {
                    MapExp.shared.topRouteExp.toMapbox()
                    Exp(.get) { StopFeaturesBuilder.shared.propRouteIdsKey }
                }
            }
        }
        ""
    }

    static func atZooms(_ pre: String, _ post: String) -> [String] {
        [stopZoomClosePrefix, stopZoomWidePrefix].map { zoom in "\(pre)\(zoom)\(post)" }
    }

    // Combine prefix and suffix strings to get the icon to display for the given zoom and index
    private static func getRouteIconName(_ zoomPrefix: String, _ index: Int) -> MapboxMaps.Exp {
        Exp(.concat) {
            stopIconPrefix
            zoomPrefix
            MapExp.shared.routeAt(index: Int32(index)).toMapbox()
            Exp(.switchCase) {
                // If at wide zoom, give terminal stops with no transfers distinct icons
                Exp(.all) {
                    Exp(.get) { StopFeaturesBuilder.shared.propIsTerminalKey }
                    MapExp.shared.singleRouteTypeExp.toMapbox()
                    Exp(.boolean) { zoomPrefix == stopZoomWidePrefix }
                }
                Exp(.concat) { stopTerminalSuffix; branchingRouteSuffixExp }
                // If at close zoom, give stops on a branch their own distinct icons
                Exp(.boolean) { zoomPrefix == stopZoomClosePrefix }
                branchingRouteSuffixExp
                ""
            }
        }
    }

    // If the stop serves a single type of route, display a regular stop icon,
    // if it serves 2 or 3, display a container icon, and the stop icons in it
    // will be displayed by the transfer layers.
    static func getStopIconName(_ zoomPrefix: String, forBus: Bool) -> MapboxMaps.Exp {
        MapExp.shared.busSwitchExp(forBus: forBus, Exp(.step) {
            Exp(.length) { MapExp.shared.routesExp.toMapbox() }
            getRouteIconName(zoomPrefix, 0)
            2
            Exp(.concat) { stopContainerPrefix; zoomPrefix; "2" }
            3
            Exp(.concat) { stopContainerPrefix; zoomPrefix; "3" }
        })
    }

    static func getStopLayerIcon(forBus: Bool = false) -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getStopIconName(stopZoomWidePrefix, forBus: forBus)
            MapDefaults.shared.closeZoomThreshold
            getStopIconName(stopZoomClosePrefix, forBus: forBus)
        })
    }

    static func getTransferIconName(_ zoomPrefix: String, _ index: Int) -> MapboxMaps.Exp {
        Exp(.step) {
            Exp(.length) { MapExp.shared.routesExp.toMapbox() }
            ""
            2
            Exp(.concat) {
                getRouteIconName(zoomPrefix, index)
                Exp(.switchCase) {
                    // Regular non-transfer bus icons are different than the other modes,
                    // and don't fit in the transfer stop containers, this adds a suffix
                    // to use a different icon when a bus is included in a transfer stack.
                    Exp(.eq) { MapExp.shared.routeAt(index: Int32(index)).toMapbox(); MapStopRoute.bus.name }
                    stopTransferSuffix
                    ""
                }
            }
        }
    }

    static func getTransferLayerIcon(_ index: Int) -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getTransferIconName(stopZoomWidePrefix, index)
            MapDefaults.shared.closeZoomThreshold
            getTransferIconName(stopZoomClosePrefix, index)
        })
    }
}
