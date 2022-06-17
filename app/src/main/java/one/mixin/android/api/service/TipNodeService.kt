package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TipSignRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.response.TipSignResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TipNodeService {
    @GET("config.json")
    suspend fun tipConfig(): MixinResponse<TipConfig>

    @POST("/")
    suspend fun postSign(@Body tipSignRequest: TipSignRequest, @Url baseUrl: String): MixinResponse<TipSignResponse>
}
