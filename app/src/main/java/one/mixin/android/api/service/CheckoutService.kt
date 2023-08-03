package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.api.request.PayTokenRequest
import one.mixin.android.api.response.PayTokenResponse
import one.mixin.android.vo.checkout.TraceRequest
import one.mixin.android.vo.checkout.TraceResponse
import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface CheckoutService {

    @POST
    suspend fun payment(@Body request: TraceRequest, @Url url: String = "https://wallet.touge.fun/checkout/payment"): TraceResponse

    @POST
    suspend fun sumsubToken(request: TokenRequest, @Url url: String = "https://wallet.touge.fun/kyc/token"): TokenResponse

    /**
     if (BuildConfig.DEBUG) {
     "https://api.sandbox.checkout.com/"
     } else {
     "https://api.checkout.com/"
     }
     */
    @POST("tokens")
    suspend fun token(@Body request: PayTokenRequest, @Url url: String = "https://api.sandbox.checkout.com/", @Header("Authorization") authorization: String = BuildConfig.CHCEKOUT_ID): PayTokenResponse
}
