//
//  NonNilModifier.swift
//  iosApp
//
//  Created by Simon, Emma on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct NonNilReplaceModifier<Value, NonNilContent>: ViewModifier where NonNilContent: View {
    var value: Value?
    var contentBuilder: (Value) -> NonNilContent

    func body(content: Content) -> some View {
        if value != nil {
            contentBuilder(value!)
        } else {
            content
        }
    }
}

struct NonNilAboveModifier<Value, NonNilContent>: ViewModifier where NonNilContent: View {
    var value: Value?
    var contentBuilder: (Value) -> NonNilContent

    func body(content: Content) -> some View {
        VStack {
            if value != nil {
                contentBuilder(value!)
            }
            content
        }
    }
}

struct NonNilBelowModifier<Value, NonNilContent>: ViewModifier where NonNilContent: View {
    var value: Value?
    var contentBuilder: (Value) -> NonNilContent

    func body(content: Content) -> some View {
        VStack {
            content
            if value != nil {
                contentBuilder(value!)
            }
        }
    }
}

public extension View {
    func replaceWhen<Value>(
        _ value: Value?,
        _ nonNilContentBuilder: @escaping (_ value: Value) -> some View
    ) -> some View {
        modifier(NonNilReplaceModifier(value: value, contentBuilder: nonNilContentBuilder))
    }

    func putAboveWhen<Value>(
        _ value: Value?,
        _ nonNilContentBuilder: @escaping (_ value: Value) -> some View
    ) -> some View {
        modifier(NonNilAboveModifier(value: value, contentBuilder: nonNilContentBuilder))
    }

    func putBelowWhen<Value>(
        _ value: Value?,
        _ nonNilContentBuilder: @escaping (_ value: Value) -> some View
    ) -> some View {
        modifier(NonNilBelowModifier(value: value, contentBuilder: nonNilContentBuilder))
    }
}
