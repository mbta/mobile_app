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
    static let branchedRouteExp = Exp(.all) {
        Exp(.any) {
            Exp(.eq) { topRouteExp; MapStopRoute.green.name }
            Exp(.eq) { topRouteExp; MapStopRoute.silver.name }
        }
        singleRouteExp
    }

    static let branchingRouteSuffixExp = Exp(.switchCase) {
        branchedRouteExp
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

    static func busSwitchExp(forBus: Bool, _ resultExpression: Exp) -> Exp {
        Exp(.switchCase) {
            Exp(.all) {
                MapExp.singleRouteTypeExp
                Exp(.eq) { MapExp.topRouteExp; MapStopRoute.bus.name }
            }
            forBus ? resultExpression : Exp(.string) { "" }
            !forBus ? resultExpression : Exp(.string) { "" }
        }
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
    static let singleRouteExp = Exp(.all) {
        singleRouteTypeExp
        Exp(.eq) { 1; Exp(.length) { Exp(.get) {
            topRouteExp
            Exp(.get) { StopSourceGenerator.propRouteIdsKey }
        } } }
    }

    static func stopLabelTextExp(forBus: Bool = false) -> Exp {
        Exp(.step) {
            Exp(.zoom)
            ""
            MapDefaults.midZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
                ""
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
                ""
            }
            MapDefaults.closeZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
                ""
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
            }
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

    static func negExp(_ numberExp: Exp) -> Exp {
        Exp(.subtract) { numberExp }
    }

    static func numExp(_ num: Double) -> Exp {
        Exp(.number) { num }
    }

    // MapBox _requires_ that the value ultimately returned by the expression here is specifically
    // length 2 array containing doubles. This means that you're not allowed to have an array of two
    // expressions, or a number and an expression, each possible return value must be expressed with
    // a pair of doubles. And the zoom expression must always be the top level one. So, that's why
    // this is implemented in such a convoluted way, thank you MapBox gods.
    static func offsetAlertExp(closeZoom: Bool, _ index: Int) -> Exp {
        let doubleRouteHeight: Double = closeZoom ? 13 : 8
        let tripleRouteHeight: Double = closeZoom ? 26 : 16
        return Exp(.step) {
            Exp(.length) { routesExp }
            offsetAlertPairExp(closeZoom: closeZoom, height: 0)
            2
            [
                offsetAlertPairExp(closeZoom: closeZoom, height: -doubleRouteHeight),
                offsetAlertPairExp(closeZoom: closeZoom, height: doubleRouteHeight),
                offsetAlertPairExp(closeZoom: closeZoom, height: 0),
            ][index]
            3
            [
                offsetAlertPairExp(closeZoom: closeZoom, height: -tripleRouteHeight),
                offsetAlertPairExp(closeZoom: closeZoom, height: 0),
                offsetAlertPairExp(closeZoom: closeZoom, height: tripleRouteHeight),
            ][index]
        }
    }

    static func offsetAlertPairExp(closeZoom: Bool, height: Double) -> Exp {
        Exp(.step) {
            Exp(.length) { routesExp }
            Exp(.switchCase) {
                branchedRouteExp
                Exp(.switchCase) {
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                    closeZoom ? xyExp([35, height]) : xyExp([26, height])
                    closeZoom ? xyExp([35, height]) : xyExp([13, height])
                }
                Exp(.all) {
                    Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                }
                closeZoom ? xyExp([26, height]) : xyExp([15, height])
                Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
                closeZoom ? xyExp([18, height - 2]) : xyExp([12, height])
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                closeZoom ? xyExp([26, height]) : xyExp([20, height])
                closeZoom ? xyExp([26, height]) : xyExp([12, height])
            }
            2
            closeZoom ? xyExp([26, height]) : xyExp([12, height])
        }
    }

    static func offsetTransferExp(closeZoom: Bool, _ index: Int) -> Exp {
        let doubleRouteOffset: Double = closeZoom ? 13 : 8
        let tripleRouteOffset: Double = closeZoom ? 26 : 16
        return Exp(.step) {
            Exp(.length) { routesExp }
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
                    branchedRouteExp
                    xyExp([1.15, 0.75])
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
                Exp(.switchCase) {
                    branchedRouteExp
                    xyExp([2.5, 1.5])
                    xyExp([2, 1.5])
                }
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
}
