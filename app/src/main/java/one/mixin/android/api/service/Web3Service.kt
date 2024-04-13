package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import retrofit2.http.Path
import one.mixin.android.api.response.Web3Account
import retrofit2.http.GET

interface Web3Service {
    @GET("account/{account}")
    suspend fun web3Account(
        @Path("account") account: String,
    ): MixinResponse<Web3Account>
}
