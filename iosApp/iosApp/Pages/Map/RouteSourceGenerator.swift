//
//  RouteSourceGenerator.swift
//  iosApp
//
//  Created by Simon, Emma on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Polyline
import shared
@_spi(Experimental) import MapboxMaps

class RouteLineData {
    let id: String
    let routeId: String
    let routePatternId: String
    let routeType: String?
    let line: LineString
    let stopIds: [String]
    let alertState: SegmentAlertState
    let color: String?
    let sortKey: Int32

    init(id: String, routeId: String, route: Route?, routePatternId: String, line: LineString, stopIds: [String],
         alertState: SegmentAlertState) {
        self.id = id
        self.routeId = routeId
        self.routePatternId = routePatternId
        routeType = route?.type.name
        self.line = line
        self.stopIds = stopIds
        self.alertState = alertState
        let hexFormattedColor: String? = route?.color == nil ? nil : "#\(route!.color)"
        color = hexFormattedColor

        sortKey = if let sortOrder = route?.sortOrder {
            // GTFS sort order has lower numbers displayed first,
            // reverse that so that Mapbox draws the route with the lowest sort order last.
            // https://docs.mapbox.com/style-spec/reference/layers/#layout-line-line-sort-key
            sortOrder * -1
        } else {
            Int32.min
        }
    }
}

enum RouteSourceGenerator {
    static let routeSourceId = "route-source"

    static let propRouteId = "routeId"
    static let propRouteType = "routeType"
    static let propRouteSortKey = "routeSortKey"
    static let propRouteColor = "routeColor"
    static let propAlertStateKey = "alertState"

    static func generateRouteLines(routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                                   routesById: [String: Route]?,
                                   stopsById: [String: Stop]?,
                                   alertsByStop: [String: AlertAssociatedStop]?) -> [RouteLineData] {
        routeData.flatMap { generateRouteLines(routeWithShapes: $0, route: routesById?[$0.routeId],
                                               stopsById: stopsById,
                                               alertsByStop: alertsByStop) }
    }

    static func generateSource(routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                               routesById: [String: Route]?,
                               stopsById: [String: Stop]?,
                               alertsByStop: [String: AlertAssociatedStop]?) -> GeoJSONSource {
        let routeLines: [RouteLineData] = generateRouteLines(routeData: routeData,
                                                             routesById: routesById,
                                                             stopsById: stopsById,
                                                             alertsByStop: alertsByStop)
        return generateSource(routeLines: routeLines)
    }

    static func generateSource(routeLines: [RouteLineData]) -> GeoJSONSource {
        let routeFeatures = routeLines.map { Self.lineToFeature(routeLineData: $0) }
        var source = GeoJSONSource(id: Self.routeSourceId)
        source.data = .featureCollection(FeatureCollection(features: routeFeatures))
        return source
    }

    static func shapesWithStopsToMapFriendly(_ shapesWithStops: [ShapeWithStops],
                                             _ stopsById: [String: Stop]?)
        -> [MapFriendlyRouteResponse.RouteWithSegmentedShapes] {
        shapesWithStops.compactMap { shapeWithStops in
            shapeWithStopsToMapFriendly(shapeWithStops, stopsById)
        }
    }

    static func shapeWithStopsToMapFriendly(_ shapeWithStops: ShapeWithStops,
                                            _ stopsById: [String: Stop]?) -> MapFriendlyRouteResponse
        .RouteWithSegmentedShapes? {
        if let shape = shapeWithStops.shape {
            let parentResolvedStops = shapeWithStops.stopIds.map { stopsById?[$0]?
                .resolveParent(stops: stopsById ?? [:]).id ?? $0
            }
            return MapFriendlyRouteResponse
                .RouteWithSegmentedShapes(routeId: shapeWithStops.routeId,
                                          segmentedShapes: [
                                              .init(sourceRoutePatternId: shapeWithStops.routePatternId,
                                                    sourceRouteId: shapeWithStops.routeId,
                                                    directionId: shapeWithStops.directionId,
                                                    routeSegments:
                                                    [
                                                        .init(id: shape.id,
                                                              sourceRoutePatternId: shapeWithStops
                                                                  .routePatternId,
                                                              sourceRouteId: shapeWithStops.routeId,
                                                              stopIds: parentResolvedStops,
                                                              otherPatternsByStopId: [:]),
                                                    ],
                                                    shape: shape),
                                          ])
        } else {
            return nil
        }
    }

    static func lineToFeature(routeLineData: RouteLineData) -> Feature {
        var feature = Feature(geometry: routeLineData.line)
        var featureProps = JSONObject()
        featureProps[Self.propRouteId] = JSONValue(String(routeLineData.routeId))
        if let routeType = routeLineData.routeType {
            featureProps[Self.propRouteType] = JSONValue(String(routeType))
        }
        if let color = routeLineData.color {
            featureProps[Self.propRouteColor] = JSONValue(String(color))
        }
        featureProps[Self.propRouteSortKey] = JSONValue(Int(routeLineData.sortKey))

        featureProps[Self.propAlertStateKey] = JSONValue(String(describing: routeLineData.alertState))
        feature.properties = featureProps
        return feature
    }

    static func generateRouteLines(routeWithShapes: MapFriendlyRouteResponse.RouteWithSegmentedShapes,
                                   route: Route?,
                                   stopsById: [String: Stop]?,
                                   alertsByStop: [String: AlertAssociatedStop]?)
        -> [RouteLineData] {
        routeWithShapes.segmentedShapes
            .flatMap { routePatternShape in
                routeShapeToLineData(routePatternShape: routePatternShape, route: route, stopsById: stopsById,
                                     alertsByStop: alertsByStop)
            }
    }

    private static func routeShapeToLineData(routePatternShape: SegmentedRouteShape,
                                             route: Route?,
                                             stopsById: [String: Stop]?,
                                             alertsByStop: [String: AlertAssociatedStop]?) -> [RouteLineData] {
        guard let polyline = routePatternShape.shape.polyline,
              let coordinates = Polyline(encodedPolyline: polyline).coordinates
        else {
            return []
        }

        let fullLineString = LineString(coordinates)
        let alertAwareSegments = routePatternShape.routeSegments.flatMap { segment in
            segment.splitAlertingSegments(alertsByStop: alertsByStop ?? [:])
        }
        return alertAwareSegments.compactMap { segment in

            routeSegmentToRouteLineData(segment: segment, route: route,
                                        fullLineString: fullLineString,
                                        stopsById: stopsById)
        }
    }

    private static func routeSegmentToRouteLineData(segment: AlertAwareRouteSegment, route: Route?,
                                                    fullLineString: LineString,
                                                    stopsById: [String: Stop]?) -> RouteLineData? {
        guard let firstStopId = segment.stopIds.first,
              let firstStop = stopsById?[firstStopId],
              let lastStopId = segment.stopIds.last,
              let lastStop = stopsById?[lastStopId],
              let lineSegment = fullLineString.sliced(from: firstStop.coordinate,
                                                      to: lastStop.coordinate)
        else {
            return nil
        }
        return RouteLineData(id: segment.id,
                             routeId: segment.sourceRouteId,
                             route: route,
                             routePatternId: segment.sourceRoutePatternId,
                             line: lineSegment,
                             stopIds: segment.stopIds, alertState: segment.alertState)
    }

    static func forRailAtStop(_ stopShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                              _ railShapes: [MapFriendlyRouteResponse.RouteWithSegmentedShapes],
                              _ routesById: [String: Route]?)
        -> [MapFriendlyRouteResponse.RouteWithSegmentedShapes] {
        let stopRailRouteIds: Set<String> = Set(stopShapes.filter { routeWithShape in
            let maybeRouteType = routesById?[routeWithShape.routeId]?.type
            if let routeType = maybeRouteType {
                return routeType == RouteType.heavyRail ||
                    routeType == RouteType.lightRail ||
                    routeType == RouteType.commuterRail
            } else {
                return false
            }
        }.map(\.routeId))
        return railShapes.filter { stopRailRouteIds.contains($0.routeId) }
    }
}
