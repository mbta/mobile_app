package com.mbta.tid.mbta_app.android.repositories

import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAppCheckRepository
import com.mbta.tid.mbta_app.repositories.Token

class AppCheckRepository : IAppCheckRepository {
    override suspend fun getToken(): ApiResult<Token> {
        return ApiResult.Error(message = "TODO: Implement")
    }
}
