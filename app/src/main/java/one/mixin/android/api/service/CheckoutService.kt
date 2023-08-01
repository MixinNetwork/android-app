package one.mixin.android.api.service

import one.mixin.android.vo.checkout.TraceRequest
import one.mixin.android.vo.checkout.TraceResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckoutService {

    @POST("/checkout/payment")
    suspend fun payment(@Body request: TraceRequest): TraceResponse
}