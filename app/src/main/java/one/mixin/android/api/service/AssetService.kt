package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetPrecision
import one.mixin.android.vo.Chain
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Ticker
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AssetService {
    @GET("assets")
    fun assets(): Call<MixinResponse<List<Asset>>>

    @GET("assets")
    suspend fun fetchAllAssetSuspend(): MixinResponse<List<Asset>>

    @GET("assets/{id}")
    suspend fun getAssetByIdSuspend(
        @Path("id") id: String,
    ): MixinResponse<Asset>

    @GET("assets/{id}")
    suspend fun getAssetPrecisionById(
        @Path("id") id: String,
    ): MixinResponse<AssetPrecision>

    @GET("snapshots")
    suspend fun getSnapshots(
        @Query("asset") assetId: String,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = LIMIT,
        @Query("opponent") opponent: String? = null,
        @Query("destination") destination: String? = null,
        @Query("tag") tag: String? = null,
    ): MixinResponse<List<Snapshot>>

    @POST("transfers")
    suspend fun transfer(
        @Body request: TransferRequest,
    ): MixinResponse<Snapshot>

    @POST("payments")
    suspend fun paySuspend(
        @Body request: TransferRequest,
    ): MixinResponse<PaymentResponse>

    @POST("withdrawals")
    suspend fun withdrawals(
        @Body request: WithdrawalRequest,
    ): MixinResponse<Snapshot>

    @GET("snapshots/{id}")
    suspend fun getSnapshotById(
        @Path("id") id: String,
    ): MixinResponse<Snapshot>

    @GET("network/assets/search/{query}")
    suspend fun queryAssets(
        @Path("query") query: String,
    ): MixinResponse<List<Asset>>

    @GET("snapshots/trace/{id}")
    suspend fun getTrace(
        @Path("id") traceId: String,
    ): MixinResponse<Snapshot>

    @GET("network/ticker")
    suspend fun ticker(
        @Query("asset") assetId: String,
        @Query("offset") offset: String? = null,
    ): MixinResponse<Ticker>

    @GET("network/chains/{id}")
    suspend fun getChainById(
        @Path("id") id: String,
    ): MixinResponse<Chain>
}
