//
//  ArrayExtension.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation

extension Array where Element: Hashable {
    /// Removes duplicates while maintaining order
    func removingDuplicates() -> [Element] {
        var addedDict = [Element: Bool]()
        return filter { addedDict.updateValue(true, forKey: $0) == nil }
    }
}
