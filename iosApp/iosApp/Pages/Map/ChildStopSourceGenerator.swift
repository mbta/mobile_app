//
//  ChildStopSourceGenerator.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import MapboxMaps
import shared

enum ChildStopSourceGenerator {
    static let childStopSourceId = "child-stop-source"

    static let propNameKey = "name"
    static let propLocationTypeKey = "locationType"
    static let propSortOrderKey = "sortOrder"

    static func generateChildStopSource(childStops: [String: Stop]?) -> GeoJSONSource {
        var source = GeoJSONSource(id: childStopSourceId)
        if let childStops {
            let stopsInOrder = childStops.values.sorted(by: { $0.id < $1.id })
            let features = stopsInOrder.enumerated().compactMap { Self.generateChildStopFeature(
                childStop: $0.element,
                index: $0.offset
            ) }
            source.data = .featureCollection(.init(features: features))
        } else {
            source.data = .featureCollection(.init(features: []))
        }
        return source
    }

    static func generateChildStopFeature(childStop: Stop, index: Int) -> Feature? {
        var feature = Feature(geometry: Point(childStop.coordinate))
        feature.identifier = .string(childStop.id)
        guard let properties = generateChildStopProperties(childStop: childStop, index: index) else { return nil }
        feature.properties = properties
        return feature
    }

    static func generateChildStopProperties(childStop: Stop, index: Int) -> JSONObject? {
        var properties = JSONObject()

        switch childStop.locationType {
        case .entranceExit: properties[Self.propNameKey] =
            .string(String(childStop.name.split(separator: " - ").last ?? ""))
        case .boardingArea, .stop: properties[Self.propNameKey] = .string(childStop.platformName ?? childStop.name)
        default: return nil
        }

        properties[Self.propLocationTypeKey] = .string(String(describing: childStop.locationType))
        properties[Self.propSortOrderKey] = .number(Double(index))
        return properties
    }
}
