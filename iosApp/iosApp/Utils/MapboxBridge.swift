//
//  MapboxBridge.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-07-16.
//  Copyright © 2024 MBTA. All rights reserved.
//

import MapboxMaps
import shared

extension GeojsonPosition {
    func toMapbox() -> LocationCoordinate2D {
        .init(latitude: latitude, longitude: longitude)
    }
}

extension [GeojsonPosition] {
    func toMapbox() -> [LocationCoordinate2D] {
        map { $0.toMapbox() }
    }
}

extension GeojsonGeometry {
    func toMapbox() -> Geometry {
        switch onEnum(of: self) {
        case let .geometryCollection(collection): .geometryCollection(GeometryCollection(geometries: collection
                    .geometries.map { $0.toMapbox() }))
        case let .point(point): .point(Point(point.coordinates.toMapbox()))
        case let .multiPoint(multiPoint): .multiPoint(MultiPoint(multiPoint.coordinates.toMapbox()))
        case let .lineString(lineString): .lineString(LineString(lineString.coordinates.toMapbox()))
        case let .multiLineString(multiLineString): .multiLineString(MultiLineString(multiLineString.coordinates
                    .map { $0.toMapbox() }))
        case let .polygon(polygon): .polygon(Polygon(polygon.coordinates.map { $0.toMapbox() }))
        case let .multiPolygon(multiPolygon): .multiPolygon(MultiPolygon(multiPolygon.coordinates
                    .map { polygonCoordinates in polygonCoordinates.map { $0.toMapbox() }}))
        }
    }
}

extension GeojsonLineString {
    func toMapbox() -> LineString {
        LineString(coordinates.map { LocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) })
    }
}

extension Kotlinx_serialization_jsonJsonElement {
    func toMapbox() -> JSONValue {
        // we know it's a valid json value
        // swiftlint:disable:next force_try
        try! JSONDecoder().decode(JSONValue.self, from: Data(description.utf8))
    }
}

extension [String: Kotlinx_serialization_jsonJsonElement] {
    func toMapbox() -> JSONObject {
        mapValues { $0.toMapbox() }
    }
}

extension GeojsonFeature {
    func toMapbox() -> Feature {
        var result = Feature(geometry: geometry?.toMapbox())
        if let id {
            result.identifier = .string(id)
        }
        result.properties = properties.toMapbox()
        return result
    }
}

extension GeojsonFeatureCollection {
    func toMapbox() -> FeatureCollection {
        FeatureCollection(features: features.map { $0.toMapbox() })
    }
}