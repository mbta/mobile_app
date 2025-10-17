//
//  ObjectCollectionBuilderExtension.swift
//  iosApp
//
//  Created by Melody Horn on 10/16/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension ObjectCollectionBuilder {
    convenience init(fileID: String = #fileID, line: Int = #line) {
        self.init(namespace: "\(fileID):\(line)")
    }

    func clone(fileID: String = #fileID, line: Int = #line) -> ObjectCollectionBuilder {
        clone(namespace: "\(fileID):\(line)")
    }
}
