package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CheckoutSessionRequest
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.api.request.TickerRequest
import one.mixin.android.api.response.CheckoutPaymentResponse
import one.mixin.android.api.response.CreateSessionResponse
import one.mixin.android.api.response.TickerResponse
import one.mixin.android.vo.checkout.PaymentRequest
import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CheckoutService {
    @POST("/checkout/payments")
    suspend fun payment(@Body request: PaymentRequest): MixinResponse<CheckoutPaymentResponse>
    @GET("/checkout/payments/{id}")
    suspend fun payment(@Path("id") id: String): MixinResponse<CheckoutPaymentResponse>

    @POST("/checkout/sessions")
    suspend fun createSession(@Body session: CreateSessionRequest): MixinResponse<CreateSessionResponse>
    @POST("/checkout/ticker")
    suspend fun ticker(@Body ticker: TickerRequest): MixinResponse<TickerResponse>

    @GET("/checkout/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): MixinResponse<CheckoutSessionRequest>

    @POST("/kyc/token")
    suspend fun sumsubToken(@Body request: TokenRequest): MixinResponse<TokenResponse>
}
