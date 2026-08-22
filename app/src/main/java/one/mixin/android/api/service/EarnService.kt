package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.WealthProduct
import retrofit2.http.GET

interface EarnService {
    @GET("productions")
    suspend fun wealthAccounts(): MixinResponse<List<WealthProduct>>
}
