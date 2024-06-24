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
    // Return true if the route type is a branching route, and there is only
    // a single route ID served at this stop, that means we're on a branch
    static let branchedRouteExp = Exp(.all) {
        Exp(.any) {
            Exp(.eq) { topRouteExp; MapStopRoute.green.name }
            Exp(.eq) { topRouteExp; MapStopRoute.silver.name }
        }
        singleRouteExp
    }

    // For the separate bus only stop and alert layers, this takes any arbitrary
    // string expression, and returns the expression or an empty string, depending
    // whether or not the stop is a bus stop and this is on the bus layer
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

    // Get the route string in the stop feature's route array at the provided index
    static func routeAt(_ index: Int) -> Exp {
        Exp(.string) { Exp(.at) { index; routesExp } }
    }

    // Get the array of MapStopRoute string values from a stop feature
    static let routesExp = Exp(.get) { StopSourceGenerator.propMapRoutesKey }

    // Returns true if you're currently on the stop page for this stop
    static let selectedExp = Exp(.boolean) { Exp(.get) { StopSourceGenerator.propIsSelectedKey } }

    // Returns the iconSize of the stop icon, interpolated by zoom level, and sized differently
    // depending on the route served by the stop.
    static let selectedSizeExp =
        Exp(.interpolate) {
            Exp(.exponential) { 1.5 }
            Exp(.zoom)
            MapDefaults.midZoomThreshold; withMultipliers(0.25, modeResize: [0.5, 2, 1.75])
            13; withMultipliers(0.625, modeResize: [1, 1.5, 1.5])
            14; withMultipliers(1)
        }

    // Returns true if there is only a single type of route served at this stop
    static let singleRouteTypeExp = Exp(.eq) { 1; Exp(.length) { routesExp }}

    // Returns true if there's both a single type of route, and only a single route ID
    static let singleRouteExp = Exp(.all) {
        singleRouteTypeExp
        Exp(.eq) { 1; Exp(.length) { Exp(.get) {
            topRouteExp
            Exp(.get) { StopSourceGenerator.propRouteIdsKey }
        } } }
    }

    // Get the label to display for this stop
    static func stopLabelTextExp(forBus: Bool = false) -> Exp {
        Exp(.step) {
            Exp(.zoom)
            // Above mid zoom, never display any labels
            ""
            // At mid zoom, only display labels for terminal rail stops
            MapDefaults.midZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
                ""
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
                ""
            }
            // At close zoom, display labels for all non-bus stops
            MapDefaults.closeZoomThreshold
            Exp(.switchCase) {
                Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
                ""
                busSwitchExp(forBus: forBus, Exp(.get) { StopSourceGenerator.propNameKey })
            }
        }
    }

    // Get the route type string ordered in the top position of the route type array,
    // or an empty string if there's nothing in the array
    static let topRouteExp = Exp(.string) {
        Exp(.switchCase) {
            Exp(.eq) { Exp(.length) { routesExp }; 0 }
            ""
            Exp(.at) { 0; routesExp }
        }
    }

    // Returns a different double value from a provided array depending on route type.
    // The resizeWith array is ordered by [bus, commuter rail, everything else].
    static func modeSizeMultiplierExp(resizeWith: [Double]) -> Exp {
        Exp(.switchCase) {
            Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
            resizeWith[0]
            Exp(.eq) { topRouteExp; MapStopRoute.commuter.name }
            resizeWith[1]
            resizeWith[2]
        }
    }

    // MapBox requires the value at the base of every case in this expression to be a literal
    // length 2 array of Doubles, since that's the type that iconOffset expects. This means
    // that we can't use expressions for each Double, which is why this function is so convoluted.
    static func offsetAlertExp(closeZoom: Bool, _ index: Int) -> Exp {
        let doubleRouteHeight: Double = closeZoom ? 13 : 8
        let tripleRouteHeight: Double = closeZoom ? 26 : 16
        return Exp(.step) {
            Exp(.length) { routesExp }
            // At stops only serving one type of route, the height doesn't need to be offset at all
            offsetAlertPairExp(closeZoom: closeZoom, height: 0)
            // At stops serving two different types of route, position the first upwards and the
            // second downwards. The third value is unused, so is set to 0.
            2
            [
                offsetAlertPairExp(closeZoom: closeZoom, height: -doubleRouteHeight),
                offsetAlertPairExp(closeZoom: closeZoom, height: doubleRouteHeight),
                xyExp([0, 0]),
            ][index]
            // At stops serving 3 routes, the first and third are moved up and down, and the center one
            // remains at the same height.
            3
            [
                offsetAlertPairExp(closeZoom: closeZoom, height: -tripleRouteHeight),
                offsetAlertPairExp(closeZoom: closeZoom, height: 0),
                offsetAlertPairExp(closeZoom: closeZoom, height: tripleRouteHeight),
            ][index]
        }
    }

    // The provided height is determined by the position in the route type array,
    // this function determines the width to offset the alert icon depending on
    // the width of the type of icon that the stop is being displayed with.
    static func offsetAlertPairExp(closeZoom: Bool, height: Double) -> Exp {
        let pillWidth: Double = 26
        let railStopWidth: Double = closeZoom ? pillWidth : 12
        let busStopWidth: Double = closeZoom ? 18 : 12
        let terminalRailStopWidth: Double = closeZoom ? pillWidth : 20
        let terminalFerryWidth: Double = closeZoom ? pillWidth : 15
        let branchPillWidth: Double = 35
        let branchTerminalWidth: Double = closeZoom ? branchPillWidth : 26
        let branchStopWidth: Double = closeZoom ? branchPillWidth : 13

        return Exp(.step) {
            Exp(.length) { routesExp }
            Exp(.switchCase) {
                // Branching routes need additional width because their pills are extra wide
                // from the additional branch indicator
                branchedRouteExp
                Exp(.switchCase) {
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                    xyExp([branchTerminalWidth, height])
                    xyExp([branchStopWidth, height])
                }
                // Ferry terminals have a special larger icon at the wide zoom level
                Exp(.all) {
                    Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                }
                xyExp([terminalFerryWidth, height])
                // Buses have an extra small dot at wide zoom, and at close zoom, the height
                // is slightly repositioned to center the alert on the tombstone, ignoring the
                // small pole rectangle at the bottom
                Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
                xyExp([busStopWidth, height - (closeZoom ? 2 : 0)])
                // Rail terminals at wide zoom have a special pill icon rather than the usual dot
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                xyExp([terminalRailStopWidth, height])
                // Regular rail stops, have a basic pill at close zoom and a dot at wide
                xyExp([railStopWidth, height])
            }
            // If the stop is a transfer stop, all routes are displayed with a basic pill and dot
            2
            xyExp([railStopWidth, height])
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

    static func offsetPinExp(closeZoom: Bool) -> Exp {
        let singleRouteOffset: Double = closeZoom ? 38 : 33
        let doubleRouteOffset: Double = closeZoom ? 52 : 42
        let tripleRouteOffset: Double = closeZoom ? 65 : 50
        return Exp(.step) {
            Exp(.length) { routesExp }
            Exp(.switchCase) {
                // Ferry terminals have a special larger icon at the wide zoom level
                Exp(.all) {
                    Exp(.eq) { topRouteExp; MapStopRoute.ferry.name }
                    Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                }
                xyExp([0, -singleRouteOffset - (closeZoom ? 0 : 2)])
                // Buses have an extra small dot at wide zoom, and at close zoom, the height
                // is slightly repositioned to center the alert on the tombstone, ignoring the
                // small pole rectangle at the bottom
                Exp(.eq) { topRouteExp; MapStopRoute.bus.name }
                xyExp([0, -singleRouteOffset - (closeZoom ? 2 : 0)])
                // Rail terminals at wide zoom have a special pill icon rather than the usual dot
                Exp(.get) { StopSourceGenerator.propIsTerminalKey }
                xyExp([0, -singleRouteOffset - (closeZoom ? 0 : 2)])
                // Regular rail stops, have a basic pill at close zoom and a dot at wide
                xyExp([0, -singleRouteOffset])
            }
            2
            xyExp([0, -doubleRouteOffset])
            3
            xyExp([0, -tripleRouteOffset])
        }
    }

    // Similar to offsetAlertExp, the labels are set to different height and width offsets
    // based on the type of icon that they're next to. I'm not certain what units these values
    // are in though, they definitely aren't pixels, and the docs don't specify.
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

    // Mapbox only accepts iconOffset values if they're wrapped in this array literal expression
    static func xyExp(_ pair: [Double]) -> Exp {
        Exp(.array) { "number"; 2; pair }
    }
}
