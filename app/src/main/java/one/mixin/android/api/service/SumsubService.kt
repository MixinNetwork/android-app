package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SumsubService {

    @POST("/kyc/token")
    suspend fun token(@Body request: TokenRequest): TokenResponse
}