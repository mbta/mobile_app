//
//  SaveFavoriteHeader.swift
//  iosApp
//
//  Created by esimon on 11/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct SaveFavoriteHeader: View {
    var isFavorite: Bool
    var onCancel: () -> Void
    var onSave: () -> Void

    let cancelText = NSLocalizedString(
        "Cancel", comment: "Button to cancel editing a favorite and return to previous screen"
    )
    let saveText = NSLocalizedString(
        "Save", comment: "Button to save a favorite that’s being added or edited"
    )
    var titleText: String {
        if isFavorite {
            NSLocalizedString(
                "Edit Favorite",
                comment: "Title for editing an individual favorite’s settings"
            )
        } else {
            NSLocalizedString("Add Favorite", comment: "Title for creating a favorite")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 6) {
                NavTextButton(
                    string: cancelText,
                    backgroundColor: Color.clear,
                    textColor: Color.key,
                    action: onCancel
                ).padding(.leading, 4)
                Spacer()
                NavTextButton(string: saveText, backgroundColor: Color.key, textColor: Color.fill3, action: onSave)
            }
            Text(titleText).font(Typography.title1Bold).padding(.leading, 16)
        }
        .padding([.bottom, .trailing], 16)
        .foregroundStyle(Color.text)
        .background(Color.fill3)
    }
}
