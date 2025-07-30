//
//  ToastView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

typealias ToastState = ToastViewModel.Toast

struct ToastView: View {
    @Environment(\.colorScheme) var colorScheme

    var state: ToastState
    var onDismiss: () -> Void

    var body: some View {
        HStack {
            Group {
                Text(state.message)
                    .font(Typography.body)
                    .foregroundColor(Color.textContrast)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 16)
                    .padding(.trailing, 8)
                if let onClose = state.onClose {
                    ActionButton(
                        kind: .close,
                        circleColor: Color.contrast,
                        iconColor: Color.textContrast
                    ) {
                        onClose()
                        onDismiss()
                    }
                    .overlay(Circle().stroke(Color.haloContrast, lineWidth: 2).frame(width: 34, height: 34))
                    .padding(.trailing, 16)
                }
                if let label = state.actionLabel, let onAction = state.onAction {
                    NavTextButton(
                        string: label,
                        backgroundColor: Color.contrast,
                        textColor: Color.textContrast
                    ) {
                        onAction()
                        onDismiss()
                    }
                    .withRoundedBorder(radius: 80, color: Color.haloContrast, width: 2)
                    .padding(.trailing, 16)
                }
            }
            .padding(.vertical, 16)
        }
        .frame(maxWidth: .infinity)
        .background(Color.contrast)
        .cornerRadius(8)
        .padding(.horizontal, 8)
        .padding(.vertical, 16)
    }
}

#Preview {
    let textOnly = ToastState(
        message: "This is a text only toast",
        duration: ToastViewModel.Duration.indefinite,
        onClose: nil,
        actionLabel: nil,
        onAction: nil,
    )
    let close = ToastState(
        message: "This is a toast with a close button",
        duration: ToastViewModel.Duration.indefinite,
        onClose: {},
        actionLabel: nil,
        onAction: nil,
    )
    let action = ToastState(
        message: "This is a toast with an action button",
        duration: ToastViewModel.Duration.indefinite,
        onClose: nil,
        actionLabel: "Action",
        onAction: {},
    )
    VStack {
        ToastView(state: textOnly, onDismiss: {})
        ToastView(state: close, onDismiss: {})
        ToastView(state: action, onDismiss: {})
    }
}
