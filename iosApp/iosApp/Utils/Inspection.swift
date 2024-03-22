//
//  Inspection.swift
//  iosApp
//
//  This is used with ViewInspector as recommended in their docs.
//  See https://github.com/nalexn/ViewInspector/blob/0.9.11/guide.md#approach-2
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import SwiftUI

final class Inspection<V> {
    let notice = PassthroughSubject<UInt, Never>()
    var callbacks = [UInt: (V) -> Void]()

    func visit(_ view: V, _ line: UInt) {
        if let callback = callbacks.removeValue(forKey: line) {
            callback(view)
        }
    }
}
