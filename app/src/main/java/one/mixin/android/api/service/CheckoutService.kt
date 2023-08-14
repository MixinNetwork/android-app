package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.api.response.CreateSessionResponse
import one.mixin.android.vo.checkout.PaymentRequest
import one.mixin.android.vo.checkout.TraceResponse
import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CheckoutService {
    @POST("/checkout/payment")
    suspend fun payment(@Body request: PaymentRequest): TraceResponse

    @POST("/kyc/token")
    suspend fun sumsubToken(@Body request: TokenRequest): TokenResponse

    @GET("/checkout/payment/state")
    suspend fun paymentState(@Query("id") traceId: String): String

    @POST("/checkout/sessions")
    suspend fun createSession(@Body session: CreateSessionRequest): MixinResponse<CreateSessionResponse>

    @GET("/checkout/seesions/{id}")
    suspend fun getSession(@Path("id") id: String): Any
}
