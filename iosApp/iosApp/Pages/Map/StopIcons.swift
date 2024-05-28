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
    // This is a single transparent pixel, used to create a layer just for tap target padding
    static let stopDummyIcon = "\(stopIconPrefix)dummy-tap-pixel"

    static let all: [String] = MapStopRoute.allCases.flatMap { routeType in atZooms(stopIconPrefix, routeType.name) }
        + ["2", "3"].flatMap { memberCount in atZooms(stopContainerPrefix, memberCount) }
        + [stopDummyIcon]

    static func atZooms(_ pre: String, _ post: String) -> [String] {
        [stopZoomClosePrefix, stopZoomWidePrefix].map { zoom in "\(pre)\(zoom)\(post)" }
    }

    private static func getRouteIconName(_ zoomPrefix: String, _ index: Int) -> Expression {
        Exp(.concat) {
            stopIconPrefix
            zoomPrefix
            Exp(.string) { Exp(.at) { index; Exp(.get) { StopSourceGenerator.propMapRoutesKey } } }
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
            StopLayerGenerator.closeZoomThreshold
            getStopIconName(stopZoomClosePrefix)
        })
    }

    static func getTransferIconName(_ zoomPrefix: String, _ index: Int) -> Expression {
        Exp(.step) {
            Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey }}
            ""
            2
            getRouteIconName(zoomPrefix, index)
        }
    }

    static func getTransferLayerIcon(_ index: Int) -> Value<ResolvedImage> {
        .expression(Exp(.step) {
            Exp(.zoom)
            getTransferIconName(stopZoomWidePrefix, index)
            StopLayerGenerator.closeZoomThreshold
            getTransferIconName(stopZoomClosePrefix, index)
        })
    }
}
