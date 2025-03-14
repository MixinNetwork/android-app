package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.Web3Account
import one.mixin.android.api.response.web3.PriorityFeeResponse
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.vo.ChainDapp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Web3Service {

    @GET("dapps")
    suspend fun dapps(): MixinResponse<List<ChainDapp>>

    @GET("tokens")
    suspend fun web3Tokens(
        @Query("chain") chain: String,
        @Query("addresses") addresses: String?,
    ): MixinResponse<List<Web3Token>>

}
