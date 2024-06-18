//
//  Typography.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

enum Typography {
    static let largeTitle = Font.custom("Inter", size: 36, relativeTo: .largeTitle).monospacedDigit()
    static let largeTitleBold = largeTitle.bold()

    static let title1 = Font.custom("Inter", size: 32, relativeTo: .title).monospacedDigit()
    static let title1Bold = title1.bold()

    static let title2 = Font.custom("Inter", size: 24, relativeTo: .title2).monospacedDigit()
    static let title2Bold = title2.bold()

    static let title3 = Font.custom("Inter", size: 20, relativeTo: .title3).monospacedDigit()
    static let title3Semibold = title3.weight(.semibold)

    static let headlineBold = Font.custom("Inter", size: 17, relativeTo: .headline).monospacedDigit().bold()
    static let headlineBoldItalic = headlineBold.italic()

    static let body = Font.custom("Inter", size: 17, relativeTo: .body).monospacedDigit()
    static let bodySemibold = body.weight(.semibold)
    static let bodyItalic = body.italic()
    static let bodySemiboldItalic = bodySemibold.italic()

    static let callout = Font.custom("Inter", size: 16, relativeTo: .callout).monospacedDigit()
    static let calloutSemibold = callout.weight(.semibold)
    static let calloutItalic = callout.italic()
    static let calloutSemiboldItalic = calloutSemibold.italic()

    static let subheadline = Font.custom("Inter", size: 15, relativeTo: .subheadline).monospacedDigit()
    static let subheadlineSemibold = subheadline.weight(.semibold)
    static let subheadlineItalic = subheadline.italic()
    static let subheadlineSemiboldItalic = subheadlineSemibold.italic()

    static let footnote = Font.custom("Inter", size: 13, relativeTo: .footnote).monospacedDigit()
    static let footnoteSemibold = footnote.weight(.semibold)
    static let footnoteItalic = footnote.italic()
    static let footnoteSemiboldItalic = footnoteSemibold.italic()

    static let caption = Font.custom("Inter", size: 12, relativeTo: .caption).monospacedDigit()
    static let captionMedium = caption.weight(.medium)
    static let captionItalic = caption.italic()
    static let captionMediumItalic = captionMedium.italic()

    static let caption2 = Font.custom("Inter", size: 11, relativeTo: .caption2).monospacedDigit()
    static let caption2Semibold = caption2.weight(.semibold)
    static let caption2Italic = caption2.italic()
    static let caption2SemiboldItalic = caption2Semibold.italic()
}
