package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AssetFee
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.PendingDeposit
import one.mixin.android.vo.Snapshot
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AssetService {

    @GET("assets")
    fun assets(): Call<MixinResponse<List<Asset>>>

    @GET("assets/{id}")
    fun asset(@Path("id") id: String): Call<MixinResponse<Asset>>

    @GET("assets/{id}/snapshots")
    fun snapshots(@Path("id") id: String): Call<MixinResponse<List<Snapshot>>>

    @POST("transfers")
    fun transfer(@Body request: TransferRequest): Observable<MixinResponse<Asset>>

    @POST("payments")
    fun pay(@Body request: TransferRequest): Observable<MixinResponse<PaymentResponse>>

    @GET("assets/{id}/fee")
    fun assetsFee(@Path("id") id: String): Observable<MixinResponse<AssetFee>>

    @POST("withdrawals")
    fun withdrawals(@Body request: WithdrawalRequest): Call<MixinResponse<Snapshot>>

    @GET("assets/{id}/addresses")
    fun addresses(@Path("id") id: String): Call<MixinResponse<List<Address>>>

    @GET("snapshots")
    fun allSnapshots(): Call<MixinResponse<List<Snapshot>>>

    @GET("mutual_snapshots/{id}")
    fun mutualSnapshots(@Path("id") id: String): Call<MixinResponse<List<Snapshot>>>

    @GET("external/transactions/{key}")
    fun pendingDeposits( @Path("key") key: String,@Query("asset") asset: String): Observable<MixinResponse<List<PendingDeposit>>>
}