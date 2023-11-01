package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.DepositEntryRequest
import one.mixin.android.api.request.GhostKeyRequest
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.response.GhostKey
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.vo.Account
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.Output
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UtxoService {
    @GET("/safe/outputs")
    suspend fun getOutputs(
        @Query("members") members: String,
        @Query("threshold") threshold: Int,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Int = 500,
        @Query("state") state: String? = null,
    ): MixinResponse<List<Output>>

    @POST("/safe/deposit/entries")
    suspend fun createDeposit(
        @Body depositEntryRequest: DepositEntryRequest
    ): MixinResponse<List<DepositEntry>>

    @POST("/safe/users")
    suspend fun registerPublicKey(
        @Body registerRequest: RegisterRequest
    ): MixinResponse<Account>

    @POST("/safe/keys")
    suspend fun ghostKey(@Body ghostKeyRequest: List<GhostKeyRequest>): MixinResponse<List<GhostKey>>

    @POST("/safe/transaction/requests")
    suspend fun transactionRequest(@Body transactionRequest: TransactionRequest): MixinResponse<TransactionResponse>

    @POST("/safe/transactions")
    suspend fun transactions(@Body transactionRequest: TransactionRequest): MixinResponse<TransactionResponse>

    @GET("/safe/transactions/{id}")
    suspend fun getTransactionsById(@Path("id") id: String): MixinResponse<TransactionResponse>
}

