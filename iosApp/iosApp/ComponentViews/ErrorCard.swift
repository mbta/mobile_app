//
//  ErrorCard.swift
//  iosApp
//
//  Created by Simon, Emma on 2/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct ErrorCard<Content: View>: View {
    @ViewBuilder let details: Content
    var button: (() -> AnyView)?

    init(_ details: () -> Content) {
        self.details = details()
        button = nil
    }

    init(details: Content, button: (() -> AnyView)? = nil) {
        self.details = details
        self.button = button
    }

    var body: some View {
        HStack {
            details.padding(.horizontal, 5.0)
            if let button {
                Spacer()
                button()
            }
        }
        .padding(16)
        .background(Color.fill3)
        .withRoundedBorder()
        .frame(maxWidth: .infinity)
    }
}

extension ErrorCard {
    func refreshable(
        _ loading: Bool = false,
        label: String = NSLocalizedString("Refresh", comment: "Refresh button label"),
        action: @escaping () -> Void
    ) -> ErrorCard {
        var card = self
        card.button = {
            AnyView(Button(
                action: action,
                label: {
                    Group {
                        if loading {
                            ProgressView().progressViewStyle(.circular)
                        } else {
                            Label(label, systemImage: "arrow.clockwise")
                                .tint(Color.text)
                                .labelStyle(.iconOnly)
                        }
                    }.frame(width: 20)
                }
            ).disabled(loading))
        }
        return card
    }
}

struct ErrorCard_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            ErrorCard(
                details: Text(verbatim: "Could not connect to the network, please try again"),
                button: { AnyView(Button(action: {}, label: {
                    Label("Refresh", systemImage: "arrow.clockwise").labelStyle(.iconOnly)
                })) }
            )
            ErrorCard(
                details: Text(verbatim: "Failed to load trains, sorry")
            )
        }.font(Typography.body)
    }
}
