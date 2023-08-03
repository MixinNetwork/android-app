package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.api.request.PayTokenRequest
import one.mixin.android.api.response.PayTokenResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CheckoutPayService {

    @POST("tokens")
    suspend fun token(
        @Body request: PayTokenRequest,
        @Header("Authorization") authorization: String = BuildConfig.CHCEKOUT_ID
    ): PayTokenResponse
}
