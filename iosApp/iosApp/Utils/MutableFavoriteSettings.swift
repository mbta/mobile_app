//
//  MutableFavoriteSettings.swift
//  iosApp
//
//  Created by Melody Horn on 12/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

class MutableFavoriteSettings: ObservableObject, Equatable, CustomDebugStringConvertible {
    @Published var notifications: Notifications

    init(notifications: Notifications) {
        self.notifications = notifications
    }

    init(_ favoriteSettings: FavoriteSettings) {
        notifications = .init(favoriteSettings.notifications)
    }

    func toShared() -> FavoriteSettings {
        .init(notifications: notifications.toShared())
    }

    var debugDescription: String {
        "MutableFavoriteSettings(notifications: \(notifications.debugDescription))"
    }

    static func == (lhs: MutableFavoriteSettings, rhs: MutableFavoriteSettings) -> Bool {
        lhs.notifications == rhs.notifications
    }

    class Notifications: ObservableObject, Equatable, CustomDebugStringConvertible {
        @Published var enabled: Bool
        @Published var windows: [Window]

        init(enabled: Bool, windows: [Window]) {
            self.enabled = enabled
            self.windows = windows
        }

        init(_ favoriteSettings: FavoriteSettings.Notifications) {
            enabled = favoriteSettings.enabled
            windows = favoriteSettings.windows.map { .init($0) }
        }

        func toShared() -> FavoriteSettings.Notifications {
            .init(enabled: enabled, windows: windows.map { $0.toShared() })
        }

        var debugDescription: String {
            "Notifications(enabled: \(enabled), windows: \(windows.debugDescription))"
        }

        static func == (lhs: Notifications, rhs: Notifications) -> Bool {
            lhs.enabled == rhs.enabled && lhs.windows == rhs.windows
        }

        class Window: Identifiable, ObservableObject, Equatable, CustomDebugStringConvertible {
            let id: UUID = .init()
            @Published var startTime: DateComponents
            @Published var endTime: DateComponents
            @Published var daysOfWeek: Set<Kotlinx_datetimeDayOfWeek>

            init(startTime: DateComponents, endTime: DateComponents, daysOfWeek: Set<Kotlinx_datetimeDayOfWeek>) {
                self.startTime = startTime
                self.endTime = endTime
                self.daysOfWeek = daysOfWeek
            }

            init(_ favoriteSettings: FavoriteSettings.NotificationsWindow) {
                startTime = .init(
                    hour: Int(favoriteSettings.startTime.hour),
                    minute: Int(favoriteSettings.startTime.minute)
                )
                endTime = .init(
                    hour: Int(favoriteSettings.endTime.hour),
                    minute: Int(favoriteSettings.endTime.minute)
                )
                daysOfWeek = favoriteSettings.daysOfWeek
            }

            func toShared() -> FavoriteSettings.NotificationsWindow {
                .init(
                    startTime: .init(
                        hour: Int32(startTime.hour ?? 0),
                        minute: Int32(startTime.minute ?? 0),
                        second: 0,
                        nanosecond: 0
                    ),
                    endTime: .init(
                        hour: Int32(endTime.hour ?? 0),
                        minute: Int32(endTime.minute ?? 0),
                        second: 0,
                        nanosecond: 0
                    ),
                    daysOfWeek: daysOfWeek
                )
            }

            /**
             The earliest possible end time for a given start time - one minute after start.
             */
            func minimumEndTime() -> DateComponents {
                if let startHour = startTime.hour, let startMinute = startTime.minute {
                    if startHour == 23, startMinute == 59 {
                        return startTime
                    }

                    if startMinute < 59 {
                        return .init(hour: startHour, minute: startMinute + 1, second: 0)
                    }
                    return .init(hour: startHour + 1, minute: 0, second: 0)
                }
                return .init(hour: 0, minute: 0, second: 0)
            }

            /**
             Based on the new startTime, update the end time to be safely after the start.
             If the new startTime is after endTime, push endTime to one hour after the start.
             */
            func setSafeEndTime(startTime: DateComponents) {
                if let startHour = startTime.hour, let endHour = endTime.hour,
                   let startMinute = startTime.minute, let endMinute = endTime.minute, startHour > endHour
                   || (startHour == endHour && startMinute >= endMinute) {
                    if startHour < 23 {
                        endTime = .init(hour: startHour + 1, minute: startMinute, second: 0)
                    } else {
                        endTime = .init(hour: 23, minute: 59, second: 0)
                    }
                }
            }

            var debugDescription: String {
                "Window(startTime: \(startTime), endTime: \(endTime), daysOfWeek: \(daysOfWeek))"
            }

            static func == (lhs: Window, rhs: Window) -> Bool {
                lhs.startTime == rhs.startTime && lhs.endTime == rhs.endTime && lhs.daysOfWeek == rhs.daysOfWeek
            }
        }
    }
}
