//
//  IconCard.swift
//  iosApp
//
//  Created by Simon, Emma on 2/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct IconCard: View {
    var icon: Image = .init(systemName: "exclamationmark.triangle")
    var details: Text
    var button: (() -> AnyView)?

    var body: some View {
        HStack {
            icon.resizable().scaledToFit().frame(width: 40)
            details.padding(.horizontal, 5.0)
            if button != nil {
                button!()
            }
        }
        .padding()
        .background(.gray.opacity(0.1))
        .clipShape(.rect(cornerRadius: 15.0))
        .frame(maxWidth: .infinity)
        .padding(5)
    }
}

extension IconCard {
    func refreshable(_ loading: Bool = false, action: @escaping () -> Void) -> IconCard {
        var card = self
        card.button = { AnyView(Button(
            action: action,
            label: {
                Group {
                    if loading {
                        ProgressView().progressViewStyle(.circular)
                    } else {
                        Label("Refresh", systemImage: "arrow.clockwise").labelStyle(.iconOnly)
                    }
                }.frame(width: 20)
            }
        )) }
        return card
    }

    func symbol(_ systemName: String) -> IconCard {
        var card = self
        card.icon = .init(systemName: systemName)
        return card
    }
}

struct IconCard_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            IconCard(
                icon: Image(systemName: "network.slash"),
                details: Text(verbatim: "Could not connect to the network, please try again"),
                button: { AnyView(Button(action: {}, label: {
                    Label("Refresh", systemImage: "arrow.clockwise").labelStyle(.iconOnly)
                })) }
            )
            IconCard(
                icon: Image(systemName: "tram"),
                details: Text(verbatim: "Failed to load trains, sorry")
            )
        }
    }
}
