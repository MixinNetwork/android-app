package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.TipConfig
import retrofit2.http.GET

interface TipNodeService {
    @GET("config.json")
    suspend fun tipConfig(): MixinResponse<TipConfig>
}
