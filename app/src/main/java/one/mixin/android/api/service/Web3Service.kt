package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.web3.PriorityFeeRequest
import one.mixin.android.api.response.Web3Account
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.api.response.web3.PriorityFeeResponse
import one.mixin.android.vo.ChainDapp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Web3Service {
    @GET("accounts/{account}")
    suspend fun web3Account(
        @Path("account") account: String,
    ): MixinResponse<Web3Account>

    @GET("transactions/{address}")
    suspend fun transactions(
        @Path("address") address: String,
        @Query("chain_id") chainId: String,
        @Query("fungible_id") fungibleId: String,
        @Query("asset_key") assetKey: String,
        @Query("limit") limit: Int = 100,
    ): MixinResponse<List<Web3Transaction>>

    @GET("dapps")
    suspend fun dapps(): MixinResponse<List<ChainDapp>>

    @GET("tokens")
    suspend fun web3Tokens(
        @Query("chain") chain: String,
        @Query("addresses") addresses: String?,
        @Query("fungibleIds") fungibleIds: String?,
    ): MixinResponse<List<Web3Token>>

    @POST("estimate-priority-fees")
    suspend fun getPriorityFee(
        @Body priorityFeeRequest: PriorityFeeRequest,
    ): MixinResponse<PriorityFeeResponse>
}
