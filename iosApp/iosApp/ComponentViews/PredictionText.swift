//
//  PredictionText.swift
//  iosApp
//
//  Created by Simon, Emma on 5/13/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct PredictionText: View {
    var minutes: Int32

    var minutesFormat: MinutesFormat { MinutesFormat.companion.from(minutes: minutes) }

    var predictionKey: String {
        switch onEnum(of: minutesFormat) {
        case let .hour(format): String(format: NSLocalizedString(
                "**%ld** hr",
                comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr\""
            ), format.hours)
        case let .hourMinute(format): String(format: NSLocalizedString(
                "**%ld** hr **%ld** min",
                comment: "Shorthand displayed number of hours and minutes until arrival, ex \"1 hr 32 min\""
            ), format.hours, format.minutes)
        case let .minute(format): String(format: NSLocalizedString(
                "**%ld** min",
                comment: "Shorthand displayed number of minutes until arrival, ex \"12 min\""
            ), format.minutes)
        }
    }

    var predictionString: AttributedString { AttributedString.tryMarkdown(predictionKey) }

    var body: some View { Text(predictionString) }
}
