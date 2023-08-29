package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.vo.sumsub.ProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RouteService {
    @POST("/checkout/payments")
    suspend fun payment(@Body request: RoutePaymentRequest): MixinResponse<RoutePaymentResponse>

    @GET("/checkout/payments/{id}")
    suspend fun payment(@Path("id") id: String): MixinResponse<RoutePaymentResponse>

    @POST("/checkout/sessions")
    suspend fun createSession(@Body session: RouteSessionRequest): MixinResponse<RouteSessionResponse>

    @GET("/checkout/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): MixinResponse<RouteSessionResponse>

    @POST("/checkout/ticker")
    suspend fun ticker(@Body ticker: RouteTickerRequest): MixinResponse<RouteTickerResponse>

    @GET("/kyc/token")
    suspend fun sumsubToken(): MixinResponse<RouteTokenResponse>

    @GET("/profile")
    suspend fun profile(): MixinResponse<ProfileResponse>
}
