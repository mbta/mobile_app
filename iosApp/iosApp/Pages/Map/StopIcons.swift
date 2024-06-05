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

    static let all: [String] = basicStopIcons + specialCaseStopIcons + stopContainerIcons + [stopDummyIcon]

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

    static func atZooms(_ pre: String, _ post: String) -> [String] {
        [stopZoomClosePrefix, stopZoomWidePrefix].map { zoom in "\(pre)\(zoom)\(post)" }
    }

    private static func getRouteIconName(_ zoomPrefix: String, _ index: Int) -> Expression {
        Exp(.concat) {
            stopIconPrefix
            zoomPrefix
            MapExp.routeAt(index)
            Exp(.switchCase) {
                // If at wide zoom, give terminal stops with no connections distinct icons
                Exp(.all) {
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                    MapExp.singleRouteTypeExp
                    Exp(.boolean) { zoomPrefix == stopZoomWidePrefix }
                }
                Exp(.concat) { stopTerminalSuffix; MapExp.branchingRouteSuffixExp }
                // If at close zoom, give stops on a branch their own distinct icons
                Exp(.boolean) { zoomPrefix == stopZoomClosePrefix }
                MapExp.branchingRouteSuffixExp
                ""
            }
        }
    }

    static func getStopIconName(_ zoomPrefix: String) -> Expression {
        Exp(.step) {
            Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey }}
            getRouteIconName(zoomPrefix, 0)
            2
            Exp(.concat) { stopContainerPrefix; zoomPrefix; "2" }
            3
            Exp(.concat) { stopContainerPrefix; zoomPrefix; "3" }
        }
    }

    static func getStopLayerIcon() -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getStopIconName(stopZoomWidePrefix)
            MapDefaults.closeZoomThreshold
            getStopIconName(stopZoomClosePrefix)
        })
    }

    static func getTransferIconName(_ zoomPrefix: String, _ index: Int) -> Expression {
        Exp(.step) {
            Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey }}
            ""
            2
            Exp(.concat) {
                getRouteIconName(zoomPrefix, index)
                Exp(.switchCase) {
                    // Regular non-transfer bus icons are different than the other modes,
                    // and don't fit in the transfer stop containers, this adds a suffix
                    // to use a different icon when a bus is included in a transfer stack.
                    Exp(.eq) { MapExp.routeAt(index); MapStopRoute.bus.name }
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
            MapDefaults.closeZoomThreshold
            getTransferIconName(stopZoomClosePrefix, index)
        })
    }
}
