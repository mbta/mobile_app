//
//  AttributedStringExtension.swift
//  iosApp
//
//  Created by Kayla Brady on 4/14/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

extension AttributedString {
    static func tryMarkdown(_ stringWithMarkdown: String) -> AttributedString {
        do {
            return try AttributedString(markdown: stringWithMarkdown)
        } catch {
            return AttributedString(stringWithMarkdown.filter { $0 != "*" })
        }
    }
}
