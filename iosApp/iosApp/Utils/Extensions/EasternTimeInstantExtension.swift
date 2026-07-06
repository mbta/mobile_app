//
//  EasternTimeInstantExtension.swift
//  iosApp
//
//  Created by Melody Horn on 7/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

extension EasternTimeInstant {
    static func now() -> EasternTimeInstant {
        companion.now()
    }

    func formatted(date: Date.FormatStyle.DateStyle, time: Date.FormatStyle.TimeStyle) -> String {
        formatted(.init(date: date, time: time))
    }

    func formatted(_ style: FormatStyle) -> String {
        toNSDateLosingTimeZone().formatted(style.inner)
    }

    struct FormatStyle {
        let inner: Date.FormatStyle

        private init(_ inner: Date.FormatStyle) {
            self.inner = inner
        }

        init() {
            inner = .init(timeZone: .eastern)
        }

        init(date: Date.FormatStyle.DateStyle, time: Date.FormatStyle.TimeStyle) {
            inner = .init(date: date, time: time, timeZone: .eastern)
        }

        func day() -> Self { Self(inner.day()) }
        func day(_ format: Date.FormatStyle.Symbol.Day) -> Self { Self(inner.day(format)) }

        func month() -> Self { Self(inner.month()) }
        func month(_ format: Date.FormatStyle.Symbol.Month) -> Self { Self(inner.month(format)) }

        func weekday() -> Self { Self(inner.weekday()) }
        func weekday(_ format: Date.FormatStyle.Symbol.Weekday) -> Self { Self(inner.weekday(format)) }
    }
}
