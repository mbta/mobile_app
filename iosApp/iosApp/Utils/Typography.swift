//
//  Typography.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-17.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

/// Presets for all fonts in the design system of the app. Any style not included here is not present in the design
/// system and is probably not supposed to be used.
///
/// For some reason, SwiftUI Fonts can only have a leading of Font.Leading.standard, .loose (+2pt), or .tight (-2pt).
/// Our designs call for more control than that.
struct Typography {
    let font: Font
    let lineSpacing: CGFloat

    private init(font: Font, lineSpacing: CGFloat) {
        self.font = font
        self.lineSpacing = lineSpacing
    }

    init(size: CGFloat, lineHeight: CGFloat, relativeTo: Font.TextStyle) {
        font = Font.custom("Inter", size: size, relativeTo: relativeTo).monospacedDigit()
        // Inter's intrinsic line height ratio appears to be 1.21x, and SwiftUI lineSpacing is additional on top of the
        // intrinsic value
        lineSpacing = lineHeight - (size * (40 / 33))
    }

    func weight(_ weight: Font.Weight) -> Self {
        .init(font: font.weight(weight), lineSpacing: lineSpacing)
    }

    func bold() -> Self { weight(.bold) }

    func italic() -> Self { .init(font: font.italic(), lineSpacing: lineSpacing) }

    static let largeTitle = Typography(size: 36, lineHeight: 40, relativeTo: .largeTitle)
    static let largeTileBold = largeTitle.bold()

    static let title1 = Typography(size: 32, lineHeight: 36, relativeTo: .title)
    static let title1Bold = title1.bold()

    static let title2 = Typography(size: 24, lineHeight: 32, relativeTo: .title2)
    static let title2Bold = title2.bold()

    static let title3 = Typography(size: 20, lineHeight: 28, relativeTo: .title3)
    static let title3Semibold = title3.weight(.semibold)
    static let title3Bold = title3.bold()

    static let headline = Typography(size: 17, lineHeight: 24, relativeTo: .headline)
    static let headlineSemibold = headline.weight(.semibold)
    static let headlineBold = headline.bold()
    static let headlineBoldItalic = headlineBold.italic()

    static let body = Typography(size: 17, lineHeight: 24, relativeTo: .body)
    static let bodySemibold = body.weight(.semibold)
    static let bodyItalic = body.italic()
    static let bodySemiboldItalic = bodySemibold.italic()

    static let callout = Typography(size: 16, lineHeight: 22, relativeTo: .callout)
    static let calloutSemibold = callout.weight(.semibold)
    static let calloutItalic = callout.italic()
    static let calloutSemiboldItalic = calloutSemibold.italic()

    static let subheadline = Typography(size: 15, lineHeight: 20, relativeTo: .subheadline)
    static let subheadlineSemibold = subheadline.weight(.semibold)
    static let subheadlineItalic = subheadline.italic()
    static let subheadlineSemiboldItalic = subheadlineSemibold.italic()

    static let footnote = Typography(size: 13, lineHeight: 18, relativeTo: .footnote)
    static let footnoteSemibold = footnote.weight(.semibold)
    static let footnoteItalic = footnote.italic()
    static let footnoteSemiboldItalic = footnoteSemibold.italic()

    static let caption = Typography(size: 12, lineHeight: 16, relativeTo: .caption)
    static let captionMedium = caption.weight(.medium)
    static let captionItalic = caption.italic()
    static let captionMediumItalic = captionMedium.italic()

    static let caption2 = Typography(size: 11, lineHeight: 13, relativeTo: .caption2)
    static let caption2Semibold = caption2.weight(.semibold)
    static let caption2Italic = caption2.italic()
    static let caption2SemiboldItalic = caption2Semibold.italic()
}

extension View {
    func font(_ typography: Typography) -> some View {
        font(typography.font).lineSpacing(typography.lineSpacing)
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 24) {
        Text(verbatim: "Samples").font(Typography.caption).textCase(.uppercase).tracking(1)
        Text(verbatim: "Long titles may wrap onto one or more lines")
            .font(Typography.largeTileBold)
        Text(verbatim: "Section title").font(Typography.title1Bold)
        Text(
            verbatim: "Body text empowers riders who rely on public transit to easily get to their destination in all service conditions."
        )
        .font(Typography.body)
        Text(verbatim: "Riders use the app to make decisions based on trustworthy, curated, MBTA-specific information.")
            .font(Typography.bodySemibold)
        Text(verbatim: """
        12:34—>
        10:45—>
        11:56—>
        """)
        .font(Typography.body)
        VStack(alignment: .leading) {
            Text(verbatim: "Tabular numerals are monospaced for better readability").font(Typography.caption)
            Text(verbatim: "All caps label, based on “Caption” style?").textCase(.uppercase).tracking(1)
                .font(Typography.caption)
        }
    }
    .multilineTextAlignment(.leading)
    .frame(width: 334)
    .padding(64)
}
