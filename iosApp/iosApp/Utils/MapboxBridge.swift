//
//  MapboxBridge.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-07-16.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared

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
        case let .geometryCollection(collection): .geometryCollection(toMapbox(collection: collection))
        case let .singleGeometry(geometry): toMapbox(singleGeometry: geometry)
        case let .multiGeometry(geometry): toMapbox(multiGeometry: geometry)
        case let .lineStringGeometry(geometry): toMapbox(lineStringGeometry: geometry)
        case let .pointGeometry(geometry): toMapbox(pointGeometry: geometry)
        case let .polygonGeometry(geometry): toMapbox(polygonGeometry: geometry)
        }
    }

    private func toMapbox(collection: GeojsonGeometryCollection<AnyObject>) -> GeometryCollection {
        .init(geometries: collection.geometries.compactMap { ($0 as? GeojsonGeometry)?.toMapbox() })
    }

    private func toMapbox(lineString: GeojsonLineString) -> LineString {
        .init(lineString.coordinates.toMapbox())
    }

    private func toMapbox(point: GeojsonPoint) -> Point {
        .init(point.coordinates.toMapbox())
    }

    private func toMapbox(polygon: GeojsonPolygon) -> Polygon {
        .init(polygon.coordinates.toMapbox())
    }

    private func toMapbox(multiLineString: GeojsonMultiLineString) -> MultiLineString {
        .init(multiLineString.coordinates.toMapbox())
    }

    private func toMapbox(multiPoint: GeojsonMultiPoint) -> MultiPoint {
        .init(multiPoint.coordinates.toMapbox())
    }

    private func toMapbox(multiPolygon: GeojsonMultiPolygon) -> MultiPolygon {
        .init(multiPolygon.coordinates.toMapbox())
    }

    private func toMapbox(singleGeometry: GeojsonSingleGeometry) -> Geometry {
        switch onEnum(of: singleGeometry) {
        case let .lineString(lineString): .lineString(toMapbox(lineString: lineString))
        case let .point(point): .point(toMapbox(point: point))
        case let .polygon(polygon): .polygon(toMapbox(polygon: polygon))
        }
    }

    private func toMapbox(multiGeometry: GeojsonMultiGeometry) -> Geometry {
        switch onEnum(of: multiGeometry) {
        case let .multiLineString(multiLineString): .multiLineString(toMapbox(multiLineString: multiLineString))
        case let .multiPoint(multiPoint): .multiPoint(toMapbox(multiPoint: multiPoint))
        case let .multiPolygon(multiPolygon): .multiPolygon(toMapbox(multiPolygon: multiPolygon))
        }
    }

    private func toMapbox(lineStringGeometry: GeojsonLineStringGeometry) -> Geometry {
        switch onEnum(of: lineStringGeometry) {
        case let .lineString(lineString): .lineString(toMapbox(lineString: lineString))
        case let .multiLineString(multiLineString): .multiLineString(toMapbox(multiLineString: multiLineString))
        }
    }

    private func toMapbox(pointGeometry: GeojsonPointGeometry) -> Geometry {
        switch onEnum(of: pointGeometry) {
        case let .point(point): .point(toMapbox(point: point))
        case let .multiPoint(multiPoint): .multiPoint(toMapbox(multiPoint: multiPoint))
        }
    }

    private func toMapbox(polygonGeometry: GeojsonPolygonGeometry) -> Geometry {
        switch onEnum(of: polygonGeometry) {
        case let .polygon(polygon): .polygon(toMapbox(polygon: polygon))
        case let .multiPolygon(multiPolygon): .multiPolygon(toMapbox(multiPolygon: multiPolygon))
        }
    }
}

extension GeojsonLineString {
    func toMapbox() -> LineString {
        LineString(coordinates.map { LocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) })
    }
}

extension Shared.JSONValue {
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

extension Shared.Feature {
    func toMapbox() -> MapboxMaps.Feature {
        var result = Feature(geometry: geometry.toMapbox())
        if let id {
            result.identifier = .string(id)
        }
        result.properties = properties.data.mapValues { $0.toMapbox() }
        return result
    }
}

extension Shared.FeatureCollection {
    func toMapbox() -> MapboxMaps.FeatureCollection {
        MapboxMaps.FeatureCollection(features: features.map { $0.toMapbox() })
    }
}

extension Shared.Exp {
    func toMapbox() -> MapboxMaps.Exp {
        bridgeStyleObject(self)
    }
}

extension Shared.LineLayer {
    func toMapbox() -> MapboxMaps.LineLayer {
        bridgeStyleObject(self)
    }
}

extension Shared.SymbolLayer {
    func toMapbox() -> MapboxMaps.SymbolLayer {
        bridgeStyleObject(self)
    }
}
