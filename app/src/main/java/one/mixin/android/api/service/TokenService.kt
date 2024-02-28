package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.AssetPrecision
import one.mixin.android.vo.Chain
import one.mixin.android.vo.Ticker
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.safe.PendingDeposit
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TokenService {
    @GET("safe/assets")
    fun assets(): Call<MixinResponse<List<Token>>>

    @GET("safe/assets")
    suspend fun fetchAllTokenSuspend(): MixinResponse<List<Token>>

    @POST("safe/assets/fetch")
    suspend fun fetchTokenSuspend(
        @Body id: List<String>,
    ): MixinResponse<List<Token>>

    @GET("safe/assets/{id}")
    suspend fun getAssetByIdSuspend(
        @Path("id") id: String,
    ): MixinResponse<Token>

    @GET("safe/assets/{id}")
    suspend fun getAssetPrecisionById(
        @Path("id") id: String,
    ): MixinResponse<AssetPrecision>

    @GET("safe/assets/{id}/fees")
    suspend fun getFees(
        @Path("id") id: String,
        @Query("destination") destination: String,
    ): MixinResponse<List<WithdrawalResponse>>

    @GET("safe/snapshots")
    suspend fun getSnapshots(
        @Query("asset") assetId: String? = null,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = LIMIT,
        @Query("opponent") opponent: String? = null,
        @Query("destination") destination: String? = null,
        @Query("tag") tag: String? = null,
    ): MixinResponse<List<SafeSnapshot>>

    @GET("safe/snapshots/{id}")
    suspend fun getSnapshotById(
        @Path("id") id: String,
    ): MixinResponse<SafeSnapshot>

    @POST("payments")
    suspend fun paySuspend(
        @Body request: TransferRequest,
    ): MixinResponse<PaymentResponse>

    @GET("safe/deposits")
    suspend fun allPendingDeposits(): MixinResponse<List<PendingDeposit>>

    @GET("safe/deposits")
    suspend fun pendingDeposits(
        @Query("asset") asset: String,
        @Query("destination") key: String,
        @Query("tag") tag: String? = null,
    ): MixinResponse<List<PendingDeposit>>

    @GET("network/assets/search/{query}")
    suspend fun queryAssets(
        @Path("query") query: String,
    ): MixinResponse<List<Token>>

    @GET("network/assets/top")
    fun topAssets(
        @Query("kind") kind: String = "NORMAL",
        @Query("limit") limit: Int = 100,
    ): Call<MixinResponse<List<TopAsset>>>

    @GET("network/ticker")
    suspend fun ticker(
        @Query("asset") assetId: String,
        @Query("offset") offset: String? = null,
    ): MixinResponse<Ticker>

    @GET("network/chains")
    suspend fun getChains(): MixinResponse<List<Chain>>

    @GET("network/chains/{id}")
    suspend fun getChainById(
        @Path("id") id: String,
    ): MixinResponse<Chain>
}
