package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.response.TipEphemeral
import one.mixin.android.api.response.TipIdentity
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TipService {
    @GET("tip/identity")
    suspend fun tipIdentity(): MixinResponse<TipIdentity>

    @GET("tip/ephermerals")
    suspend fun tipEphemerals(): MixinResponse<List<TipEphemeral>>

    @POST("tip/ephermerals")
    suspend fun tipEphemeral(@Body request: TipRequest): MixinResponse<TipEphemeral>

    @POST("tip/secret")
    suspend fun tipSecret(@Body request: TipSecretRequest): MixinResponse<TipIdentity>
}
