package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.response.ApiResult
import kotlinx.serialization.Serializable

@Serializable data class Token(var token: String)

interface IAppCheckRepository {
    suspend fun getToken(): ApiResult<Token>
}

class MockAppCheckRepository(result: ApiResult<Token>) : IAppCheckRepository {
    private var result = result

    override suspend fun getToken(): ApiResult<Token> {
        return result
    }

    constructor() : this(ApiResult.Ok(Token("fake_app_check_token")))
}
