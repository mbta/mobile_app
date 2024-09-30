//
//  AppCheckRepository.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAppCheck
import Foundation
import shared

class AppCheckRepository: IAppCheckRepository {
    // swiftlint:disable:next identifier_name
    func __getToken() async -> ApiResult<shared.Token> {
        do {
            let token = try await AppCheck.appCheck().token(forcingRefresh: false)
            return ApiResultOk(data: Token(token: token.token))
        } catch {
            return ApiResultError(code: nil, message: "\(error)")
        }
    }
}
