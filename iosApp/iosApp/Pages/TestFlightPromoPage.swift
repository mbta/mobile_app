//
//  TestFlightPromoPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-11-18.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct TestFlightPromoPage: View {
    let onDismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Spacer()
            Image(.appStoreListing)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .shadow(color: Color.text.opacity(0.25), radius: 4, x: 0, y: 2)
            Spacer()
            Text("MBTA Go has officially launched!")
                .font(Typography.title1Bold)
            Text("Thank you for being a beta tester and giving us valuable feedback along the way.")
                .font(Typography.title3)
            Spacer()
            Link(destination: .init(string: "https://example.com")!) {
                Text("Download from the App Store")
                    .font(Typography.bodySemibold)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .foregroundStyle(Color.fill3)
                    .background(Color.key)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            Button(action: onDismiss) {
                Text("Remind me next time")
                    .font(Typography.body)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .foregroundStyle(Color.key)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.key))
            }
            Text("This TestFlight app will no longer be available after December 20.")
                .font(Typography.body)
            Spacer()
        }
        .padding(.horizontal, 32)
        .background(Color.fill2)
    }
}

#Preview {
    TestFlightPromoPage(onDismiss: {})
}
