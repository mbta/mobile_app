//
//  RouteCardDataManageVMModifier.swift
//  iosApp
//
//  Created by esimon on 9/10/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardDataManageVMModifier: ViewModifier {
    var viewModel: IRouteCardDataViewModel
    @Binding var vmState: RouteCardDataViewModel.State?

    func body(content: Content) -> some View {
        content
            .task {
                for await models in viewModel.models {
                    vmState = models
                }
            }
    }
}

public extension View {
    func manageVM(
        _ viewModel: IRouteCardDataViewModel,
        _ state: Binding<RouteCardDataViewModel.State?>,
    ) -> some View {
        modifier(RouteCardDataManageVMModifier(viewModel: viewModel, vmState: state))
    }
}
