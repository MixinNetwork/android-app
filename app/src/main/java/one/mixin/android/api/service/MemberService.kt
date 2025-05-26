package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.api.response.MemberPlan
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MemberService {
    @GET("safe/membership/plans")
    suspend fun getPlans(): MixinResponse<MemberPlan>

    @POST("safe/membership/orders")
    suspend fun createOrder(@Body request: MemberOrderRequest): MixinResponse<MemberOrder>
}
