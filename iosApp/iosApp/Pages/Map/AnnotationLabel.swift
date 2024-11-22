//
//  AnnotationLabel.swift
//  iosApp
//
//  Created by Simon, Emma on 5/1/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import SwiftUI

extension View {
    func annotationLabel<Content: View>(_ content: Content) -> ModifiedContent<Self, FloatingLabel<Content>> {
        modifier(FloatingLabel(labelContent: content))
    }
}

struct FloatingLabel<LabelContent: View>: ViewModifier {
    let labelContent: LabelContent

    func body(content: Content) -> some View {
        content.overlay(
            GeometryReader { proxy in
                Rectangle().fill(Color.clear).overlay(
                    labelContent
                        .frame(minWidth: 150, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .offset(x: proxy.size.width + 4, y: 0),
                    alignment: .leading
                )
            },
            alignment: .leading
        )
    }
}
