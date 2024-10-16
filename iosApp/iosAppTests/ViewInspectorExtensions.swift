//
//  ViewInspectorExtensions.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-16.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI
import ViewInspector

extension InspectableView {
    func findAndCallOnChange<E: Equatable>(
        relation: ViewSearch.Relation = .child,
        newValue value: E,
        index: Int = 0
    ) throws {
        _ = try find(relation: relation, where: { child in
            do {
                try child.callOnChange(newValue: value, index: index)
                print("found onChange<\(E.self)> at", child.pathToRoot)
                return true
            } catch {
                return false
            }
        })
    }
}
