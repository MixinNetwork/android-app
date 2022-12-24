package one.mixin.android.api.service

import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.request.TipWatchRequest
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.signature.TipWatchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TipNodeService {
    @POST("/{path}")
    suspend fun sign(@Body tipSignRequest: TipSignRequest, @Path(value = "path", encoded = true) path: String): Response<TipSignResponse>

    @POST("/{path}")
    suspend fun watch(@Body tipWatchRequest: TipWatchRequest, @Path(value = "path", encoded = true) path: String): TipWatchResponse

    @GET("/{path}")
    suspend fun get(@Path(value = "path", encoded = true) path: String): Any
}
