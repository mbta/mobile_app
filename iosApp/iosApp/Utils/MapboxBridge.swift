//
//  MapboxBridge.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-07-16.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import shared

private func bridgeStyleObject<T: Decodable>(_ object: MapboxStyleObject) -> T {
    // the json is supposed to be valid
    // swiftlint:disable:next force_try
    try! JSONDecoder().decode(T.self, from: Data(object.toJsonString().utf8))
}

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

extension [[GeojsonPosition]] {
    func toMapbox() -> [[LocationCoordinate2D]] {
        map { $0.toMapbox() }
    }
}

extension [[[GeojsonPosition]]] {
    func toMapbox() -> [[[LocationCoordinate2D]]] {
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
        case let .polygon(polygon): .polygon(Polygon(polygon.coordinates.toMapbox()))
        case let .multiPolygon(multiPolygon): .multiPolygon(MultiPolygon(multiPolygon.coordinates.toMapbox()))
        }
    }
}

extension GeojsonLineString {
    func toMapbox() -> LineString {
        LineString(coordinates.map { LocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) })
    }
}

extension shared.JSONValue {
    func toMapbox() -> MapboxMaps.JSONValue {
        switch onEnum(of: self) {
        case let .array(data): .array(data.data.map { $0.toMapbox() })
        case let .boolean(data): .boolean(data.data)
        case let .number(data): .number(data.data)
        case let .object(data): .object(data.data.mapValues { $0.toMapbox() })
        case let .string(data): .string(data.data)
        }
    }
}

extension shared.Feature {
    func toMapbox() -> MapboxMaps.Feature {
        var result = Feature(geometry: geometry.toMapbox())
        if let id {
            result.identifier = .string(id)
        }
        result.properties = properties.data.mapValues { $0.toMapbox() }
        return result
    }
}

extension shared.FeatureCollection {
    func toMapbox() -> MapboxMaps.FeatureCollection {
        MapboxMaps.FeatureCollection(features: features.map { $0.toMapbox() })
    }
}

extension shared.Exp {
    func toMapbox() -> MapboxMaps.Exp {
        bridgeStyleObject(self)
    }
}

extension shared.LineLayer {
    func toMapbox() -> MapboxMaps.LineLayer {
        bridgeStyleObject(self)
    }
}

extension shared.SymbolLayer {
    func toMapbox() -> MapboxMaps.SymbolLayer {
        bridgeStyleObject(self)
    }
}
