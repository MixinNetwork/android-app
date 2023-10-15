package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.DepositEntryRequest
import one.mixin.android.session.Session
import one.mixin.android.vo.Deposit
import one.mixin.android.vo.Output
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UtxoService {
    @GET("/outputs")
    suspend fun getOutputs(
        @Query("members") members: String,
        @Query("threshold") threshold: Int,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = 500,
        @Query("state") state: String? = null,
        @Query("user") user: String? = Session.getAccountId(),
    ): MixinResponse<List<Output>>

    @POST("/deposit_entries")
    suspend fun createDeposit(
        @Body depositEntryRequest: DepositEntryRequest
    ): MixinResponse<List<Deposit>>

}
