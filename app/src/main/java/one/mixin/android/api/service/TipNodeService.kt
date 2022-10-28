package one.mixin.android.api.service

import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.request.TipWatchRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.response.TipSignResponse
import one.mixin.android.api.response.signature.TipWatchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TipNodeService {
    @GET("config.json")
    suspend fun tipConfig(): TipConfig

    @POST
    suspend fun sign(@Body tipSignRequest: TipSignRequest, @Url baseUrl: String): Response<TipSignResponse>

    @POST
    suspend fun watch(@Body tipWatchRequest: TipWatchRequest, @Url baseUrl: String): TipWatchResponse

    @GET("")
    suspend fun get(@Url baseUrl: String): Any
}
