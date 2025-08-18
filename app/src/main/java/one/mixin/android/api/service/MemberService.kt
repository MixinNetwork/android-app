package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberPlan
import one.mixin.android.api.response.MembershipOrder
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MemberService {
    @GET("safe/membership/plans")
    suspend fun getPlans(): MixinResponse<MemberPlan>

    @POST("safe/membership/orders")
    suspend fun createOrder(@Body request: MemberOrderRequest): MixinResponse<MembershipOrder>

    @GET("safe/membership/orders")
    suspend fun getOrders(): MixinResponse<List<MembershipOrder>>

    @GET("safe/membership/orders/{id}")
    suspend fun getOrder(@Path("id") id: String): MixinResponse<MembershipOrder>

    @POST("safe/membership/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): MixinResponse<MembershipOrder>
}
