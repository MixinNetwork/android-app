package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.request.TipSecretReadRequest
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.response.TipEphemeral
import one.mixin.android.api.response.TipIdentity
import one.mixin.android.api.response.TipSecretResponse
import one.mixin.android.tip.wc.internal.TipGas
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TipService {
    @GET("tip/identity")
    suspend fun tipIdentity(): MixinResponse<TipIdentity>

    @GET("tip/ephemerals")
    suspend fun tipEphemerals(): MixinResponse<List<TipEphemeral>>

    @POST("tip/ephemerals")
    suspend fun tipEphemeral(
        @Body request: TipRequest,
    ): MixinResponse<Unit>

    @POST("tip/secret")
    suspend fun readTipSecret(
        @Body request: TipSecretReadRequest,
    ): MixinResponse<TipSecretResponse>

    @POST("tip/secret")
    suspend fun updateTipSecret(
        @Body request: TipSecretRequest,
    ): MixinResponse<Unit>

}
