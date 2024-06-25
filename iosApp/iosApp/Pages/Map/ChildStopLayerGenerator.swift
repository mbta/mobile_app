//
//  ChildStopLayerGenerator.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import MapboxMaps
import shared

class ChildStopLayerGenerator {
    let childStopLayer: SymbolLayer = createChildStopLayer()

    static let childStopLayerId = "child-stop-layer"

    static let annotationTextZoomThreshold = 19.0

    static func createChildStopLayer() -> SymbolLayer {
        var layer = SymbolLayer(id: Self.childStopLayerId, source: ChildStopSourceGenerator.childStopSourceId)
        layer.iconImage = .expression(Exp(.match) {
            Exp(.get) { ChildStopSourceGenerator.propLocationTypeKey }
            String(describing: LocationType.entranceExit)
            ChildStopIcons.entranceIcon
            [String(describing: LocationType.boardingArea), String(describing: LocationType.stop)]
            ChildStopIcons.platformIcon
            ""
        })
        layer.textField = .expression(Exp(.step) {
            Exp(.zoom)
            ""
            Self.annotationTextZoomThreshold
            Exp(.get) { ChildStopSourceGenerator.propNameKey }
        })

        layer.textColor = .constant(.init(.gray))
        layer.textFont = .constant(["Inter Italic"])
        layer.textHaloColor = .constant(.init(.fill3))
        layer.textHaloWidth = .constant(2.0)
        layer.textSize = .constant(12)
        layer.textVariableAnchor = .constant([.left, .right, .bottom, .top])
        layer.textJustify = .constant(.auto)
        layer.textOptional = .constant(true)
        layer.textRadialOffset = .constant(1)
        layer.iconAllowOverlap = .constant(true)
        layer.textAllowOverlap = .constant(false)
        layer.symbolSortKey = .expression(Exp(.get) { ChildStopSourceGenerator.propSortOrderKey })
        layer.minZoom = MapDefaults.closeZoomThreshold
        return layer
    }
}
