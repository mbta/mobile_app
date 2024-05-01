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
    let isAlerting: Bool
    let color: String?
    let sortKey: Int32

    init(id: String, routeId: String, route: Route?, routePatternId: String, line: LineString, stopIds: [String],
         isAlerting: Bool) {
        self.id = id
        self.routeId = routeId
        self.routePatternId = routePatternId
        routeType = route?.type.name
        self.line = line
        self.stopIds = stopIds
        self.isAlerting = isAlerting
        let hexFormattedColor: String? = route?.color == nil ? nil : "#\(route!.color)"
        color = hexFormattedColor
        sortKey = if let sortOrder = route?.sortOrder {
            sortOrder * -1
        } else {
            Int32.min
        }
    }
}

class RouteSourceGenerator {
    let routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes]
    let routeLines: [RouteLineData]
    let routeSource: GeoJSONSource

    static let routeSourceId = "route-source"

    static let propRouteId = "routeId"
    static let propRouteType = "routeType"
    static let propRouteSortKey = "routeSortKey"
    static let propRouteColor = "routeColor"
    static let propIsAlertingKey = "isAlerting"

    init(routeData: [MapFriendlyRouteResponse.RouteWithSegmentedShapes], routesById: [String: Route],
         stopsById: [String: Stop], alertsByStop: [String: AlertAssociatedStop]) {
        self.routeData = routeData
        routeLines = routeData.flatMap { Self.generateRouteLines(routeWithShapes: $0, route: routesById[$0.routeId],
                                                                 stopsById: stopsById,
                                                                 alertsByStop: alertsByStop) }

        let routeFeatures = routeLines.map { Self.lineToFeature(routeLineData: $0) }
        var source = GeoJSONSource(id: Self.routeSourceId)
        source.data = .featureCollection(FeatureCollection(features: routeFeatures))

        routeSource = source
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

        featureProps[Self.propIsAlertingKey] = JSONValue(Bool(routeLineData.isAlerting))
        feature.properties = featureProps
        return feature
    }

    static func generateRouteLines(routeWithShapes: MapFriendlyRouteResponse.RouteWithSegmentedShapes,
                                   route: Route?,
                                   stopsById: [String: Stop],
                                   alertsByStop: [String: AlertAssociatedStop])
        -> [RouteLineData] {
        routeWithShapes.segmentedShapes
            .flatMap { routePatternShape in
                routeShapeToLineData(routePatternShape: routePatternShape, route: route, stopsById: stopsById,
                                     alertsByStop: alertsByStop)
            }
    }

    private static func routeShapeToLineData(routePatternShape: SegmentedRouteShape,
                                             route: Route?,
                                             stopsById: [String: Stop],
                                             alertsByStop: [String: AlertAssociatedStop]) -> [RouteLineData] {
        guard let polyline = routePatternShape.shape.polyline,
              let coordinates = Polyline(encodedPolyline: polyline).coordinates
        else {
            return []
        }

        let fullLineString = LineString(coordinates)
        let alertAwareSegments = routePatternShape.routeSegments.flatMap { segment in
            segment.splitAlertingSegments(alertsByStop: alertsByStop)
        }
        return alertAwareSegments.compactMap { segment in

            routeSegmentToRouteLineData(segment: segment, route: route,
                                        fullLineString: fullLineString,
                                        stopsById: stopsById)
        }
    }

    private static func routeSegmentToRouteLineData(segment: AlertAwareRouteSegment, route: Route?,
                                                    fullLineString: LineString,
                                                    stopsById: [String: Stop]) -> RouteLineData? {
        guard let firstStopId = segment.stopIds.first,
              let firstStop = stopsById[firstStopId],
              let lastStopId = segment.stopIds.last,
              let lastStop = stopsById[lastStopId],
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
                             stopIds: segment.stopIds, isAlerting: segment.isAlerting)
    }
}
