package one.mixin.android.api.service

import one.mixin.android.vo.checkout.TraceRequest
import one.mixin.android.vo.checkout.TraceResponse
import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

interface CheckoutService {

    @POST
    suspend fun payment(@Body request: TraceRequest, @Url url: String = "https://wallet.touge.fun/checkout/payment"): TraceResponse

    @POST
    suspend fun sumsubToken(@Body request: TokenRequest, @Url url: String = "https://wallet.touge.fun/kyc/token"): TokenResponse

    @GET("/state?id={id}")
    suspend fun paymentState(@Path("id") traceId: String): Any
}
