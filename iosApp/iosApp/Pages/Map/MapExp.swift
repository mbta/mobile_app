//
//  MapExp.swift
//  iosApp
//
//  Created by Simon, Emma on 6/3/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

enum MapExp {
    static let branchingRouteSuffixExp = Exp(.switchCase) {
        Exp(.all) {
            Exp(.any) {
                Exp(.eq) { topRouteExp; MapStopRoute.green.name }
                Exp(.eq) { topRouteExp; MapStopRoute.silver.name }
            }
            singleRouteTypeExp
            singleBranchRouteExp
        }
        Exp(.concat) {
            "-"
            Exp(.at) {
                0
                Exp(.get) {
                    topRouteExp
                    Exp(.get) { StopSourceGenerator.propRouteIdsKey }
                }
            }
        }
        ""
    }

    static func routeAt(_ index: Int) -> Exp {
        Exp(.string) { Exp(.at) { index; Exp(.get) { StopSourceGenerator.propMapRoutesKey } } }
    }

    static let routesExp = Exp(.get) { StopSourceGenerator.propMapRoutesKey }

    static let selectedExp = Exp(.boolean) { Exp(.get) { StopSourceGenerator.propIsSelectedKey } }
    static let selectedSizeExp: Exp =
        .init(.interpolate) {
            Exp(.exponential) { 1.5 }
            Exp(.zoom)
            MapDefaults.midZoomThreshold; withMultipliers(0.25, modeResize: [0.5, 2, 1.75])
            13; withMultipliers(0.625, modeResize: [1, 1.5, 1.5])
            14; withMultipliers(1)
        }

    static let singleRouteTypeExp = Exp(.eq) { 1; Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } } }
    static let singleBranchRouteExp = Exp(.eq) { 1; Exp(.length) { Exp(.get) {
        topRouteExp
        Exp(.get) { StopSourceGenerator.propRouteIdsKey }
    } } }

    static let stopLabelTextExp = Exp(.step) {
        Exp(.zoom)
        ""
        MapDefaults.midZoomThreshold
        Exp(.switchCase) {
            Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
            ""
            Exp(.get) { StopSourceGenerator.propIsTerminalKey }
            Exp(.get) { StopSourceGenerator.propNameKey }
            ""
        }
        MapDefaults.closeZoomThreshold
        Exp(.switchCase) {
            Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
            ""
            Exp(.get) { StopSourceGenerator.propNameKey }
        }
    }

    static let topRouteExp = Exp(.string) {
        Exp(.switchCase) {
            Exp(.eq) { Exp(.length) { routesExp }; 0 }
            ""
            Exp(.at) { 0; routesExp }
        }
    }

    static func modeSizeMultiplierExp(resizeWith: [Double]) -> Exp {
        Exp(.switchCase) {
            Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
            resizeWith[0]
            Exp(.eq) { topRouteExp; MapStopRoute.commuter.name }
            resizeWith[1]
            resizeWith[2]
        }
    }

    static func transferOffsetExp(closeZoom: Bool, _ index: Int) -> Exp {
        let doubleRouteOffset: Double = closeZoom ? 13 : 8
        let tripleRouteOffset: Double = closeZoom ? 26 : 16
        return Exp(.step) {
            Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } }
            xyExp([0, 0])
            2
            xyExp([[0, -doubleRouteOffset], [0, doubleRouteOffset], [0, 0]][index])
            3
            xyExp([[0, -tripleRouteOffset], [0, 0], [0, tripleRouteOffset]][index])
        }
    }

    static let labelOffsetExp: Exp =
        .init(.interpolate) {
            Exp(.exponential) { 1.5 }
            Exp(.zoom)
            MapDefaults.midZoomThreshold
            Exp(.step) {
                Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } }
                Exp(.switchCase) {
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                    xyExp([1, 0.75])
                    xyExp([0.75, 0.5])
                }
                2
                xyExp([0.5, 1.25])
                3
                xyExp([0.5, 1.5])
            }
            MapDefaults.closeZoomThreshold
            Exp(.step) {
                Exp(.length) { Exp(.get) { StopSourceGenerator.propMapRoutesKey } }
                xyExp([2, 1.5])
                2
                xyExp([2, 2])
                3
                xyExp([2, 2.5])
            }
        }

    // The modeResize array must contain 3 entries for [BUS, COMMUTER, fallback]
    static func withMultipliers(_ base: Double, modeResize: [Double] = [1, 1, 1]) -> Exp {
        Exp(.product) {
            base
            modeSizeMultiplierExp(resizeWith: modeResize)
            // TODO: We actually want to give the icon a halo rather than resize,
            // but that is only supported for SDFs, which can only be one color.
            // Alternates of stop icon SVGs with halo applied?
            Exp(.switchCase) { selectedExp; 1.25; 1 }
        }
    }

    static func xyExp(_ pair: [Double]) -> Exp {
        Exp(.array) { "number"; 2; pair }
    }

    static func xyExp(_ pair: [Exp]) -> Exp {
        Exp(.array) { "number"; 2; pair }
    }
}
