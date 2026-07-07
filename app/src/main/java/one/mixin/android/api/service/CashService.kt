package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.CashAccount
import retrofit2.http.GET

interface CashService {
    @GET("account")
    suspend fun account(): MixinResponse<CashAccount>
}
