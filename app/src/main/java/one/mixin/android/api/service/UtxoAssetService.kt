package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.vo.Token
import retrofit2.http.GET
import retrofit2.http.Path

interface UtxoAssetService {
    @GET("network/assets/{id}")
    suspend fun getAssetByMixinIdSuspend(@Path("id") mixinId: String): MixinResponse<Token>

}
