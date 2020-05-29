package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AssetFee
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.PendingDeposit
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.TopAsset
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
    suspend fun getAssetByIdSuspend(@Path("id") id: String): MixinResponse<Asset>

    @GET("assets/{id}/snapshots")
    suspend fun getSnapshotsByAssetId(
        @Path("id") id: String,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = LIMIT
    ): MixinResponse<List<Snapshot>>

    @GET("snapshots")
    suspend fun getAllSnapshots(
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = LIMIT,
        @Query("opponent") opponent: String? = null
    ): MixinResponse<List<Snapshot>>

    @GET("snapshots")
    suspend fun getSnapshots(
        @Query("asset") assetId: String,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = LIMIT,
        @Query("opponent") opponent: String? = null,
        @Query("destination") destination: String? = null,
        @Query("tag") tag: String? = null
    ): MixinResponse<List<Snapshot>>

    @POST("transfers")
    suspend fun transfer(@Body request: TransferRequest): MixinResponse<Void>

    @POST("payments")
    fun pay(@Body request: TransferRequest): Observable<MixinResponse<PaymentResponse>>

    @POST("payments")
    suspend fun paySuspend(@Body request: TransferRequest): MixinResponse<PaymentResponse>

    @GET("assets/{id}/fee")
    fun assetsFee(@Path("id") id: String): Observable<MixinResponse<AssetFee>>

    @POST("withdrawals")
    suspend fun withdrawals(@Body request: WithdrawalRequest): MixinResponse<Void>

    @GET("assets/{id}/addresses")
    fun addresses(@Path("id") id: String): Call<MixinResponse<List<Address>>>

    @GET("snapshots/{id}")
    suspend fun getSnapshotById(@Path("id") id: String): MixinResponse<Snapshot>

    @GET("transfers/trace/{id}")
    suspend fun getSnapshotByTraceId(@Path("id") traceId: String): MixinResponse<Snapshot>

    @GET("external/transactions")
    suspend fun pendingDeposits(
        @Query("asset") asset: String,
        @Query("destination") key: String? = null,
        @Query("tag") tag: String? = null
    ): MixinResponse<List<PendingDeposit>>

    @GET("network/assets/search/{query}")
    suspend fun queryAssets(@Path("query") query: String): MixinResponse<List<Asset>>

    @GET("network/assets/top")
    fun topAssets(): Call<MixinResponse<List<TopAsset>>>
}
