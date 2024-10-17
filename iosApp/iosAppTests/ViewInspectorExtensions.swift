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
    func findAndCallOnChange(
        relation: ViewSearch.Relation = .child,
        newValue value: some Equatable,
        index: Int = 0
    ) throws {
        do {
            try callOnChange(newValue: value, index: index)
            return
        } catch {}
        _ = try find(relation: relation, where: { child in
            do {
                try child.callOnChange(newValue: value, index: index)
                return true
            } catch {
                return false
            }
        })
    }
}
