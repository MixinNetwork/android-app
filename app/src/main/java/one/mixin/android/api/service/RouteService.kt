package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.response.RouteCreateTokenResponse
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.vo.Card
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RouteService {
    @POST("checkout/payments")
    suspend fun payment(
        @Body request: RoutePaymentRequest,
    ): MixinResponse<RoutePaymentResponse>

    @GET("checkout/payments/{id}")
    suspend fun payment(
        @Path("id") id: String,
    ): MixinResponse<RoutePaymentResponse>

    @GET("checkout/payments")
    suspend fun payments(): MixinResponse<List<RoutePaymentResponse>>

    @POST("checkout/sessions")
    suspend fun createSession(
        @Body session: RouteSessionRequest,
    ): MixinResponse<RouteSessionResponse>

    @POST("checkout/instruments")
    suspend fun createInstrument(
        @Body session: RouteInstrumentRequest,
    ): MixinResponse<Card>

    @GET("checkout/instruments")
    suspend fun instruments(): MixinResponse<List<Card>>

    @DELETE("/checkout/instruments/{id}")
    suspend fun deleteInstruments(
        @Path("id") id: String,
    ): MixinResponse<Void>

    @GET("checkout/sessions/{id}")
    suspend fun getSession(
        @Path("id") id: String,
    ): MixinResponse<RouteSessionResponse>

    @POST("checkout/tokens")
    suspend fun token(
        @Body tokenRequest: RouteTokenRequest,
    ): MixinResponse<RouteCreateTokenResponse>

    @POST("quote")
    suspend fun ticker(
        @Body ticker: RouteTickerRequest,
    ): MixinResponse<RouteTickerResponse>

    @GET("kyc/token")
    suspend fun sumsubToken(): MixinResponse<RouteTokenResponse>

    @GET("kyc/token")
    fun callSumsubToken(): Call<MixinResponse<RouteTokenResponse>>

    @GET("profile")
    suspend fun profile(
        @Query("version") version: String,
    ): MixinResponse<ProfileResponse>
}
