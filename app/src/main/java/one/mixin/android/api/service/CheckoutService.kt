package one.mixin.android.api.service

import one.mixin.android.vo.checkout.TraceRequest
import one.mixin.android.vo.checkout.TraceResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface CheckoutService {

    @POST
    suspend fun payment(@Body request: TraceRequest, @Url url: String = "https://wallet.touge.fun/checkout/payment"): TraceResponse
}