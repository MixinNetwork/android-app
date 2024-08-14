package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.OrderRequest
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RoutePriceRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.web3.ParseTxRequest
import one.mixin.android.api.request.web3.PostTxRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.RouteCreateTokenResponse
import one.mixin.android.api.response.RouteOrderResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.vo.Card
import one.mixin.android.vo.market.HistoryPrice
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RouteService {
    @POST("orders/{id}/payment")
    suspend fun order(
        @Path("id") id: String,
        @Body request: RoutePaymentRequest,
    ): MixinResponse<RouteOrderResponse>

    @GET("orders/{id}")
    suspend fun order(
        @Path("id") id: String,
    ): MixinResponse<RouteOrderResponse>

    @GET("orders")
    suspend fun payments(): MixinResponse<List<RouteOrderResponse>>

    @POST("orders")
    suspend fun createOrder(
        @Body orderRequest: OrderRequest,
    ): MixinResponse<RouteOrderResponse>

    @GET("orders/{id}")
    suspend fun getOrder(
        @Path("id") id: String,
    ): MixinResponse<RouteOrderResponse>

    @POST("orders/{id}/price")
    suspend fun updateOrderPrice(
        @Path("id") id: String,
        @Body request: RoutePriceRequest,
    ): MixinResponse<RouteOrderResponse>

    @POST("checkout/instruments")
    suspend fun createInstrument(
        @Body session: RouteInstrumentRequest,
    ): MixinResponse<Card>

    @GET("checkout/instruments")
    suspend fun instruments(): MixinResponse<List<Card>>

    @DELETE("/checkout/instruments/{id}")
    suspend fun deleteInstruments(
        @Path("id") id: String,
    ): MixinResponse<Void>

    @POST("checkout/tokens")
    suspend fun token(
        @Body tokenRequest: RouteTokenRequest,
    ): MixinResponse<RouteCreateTokenResponse>

    @POST("quote")
    suspend fun ticker(
        @Body ticker: RouteTickerRequest,
    ): MixinResponse<RouteTickerResponse>

    @GET("kyc/token")
    suspend fun sumsubToken(): MixinResponse<RouteTokenResponse>

    @GET("kyc/token")
    fun callSumsubToken(): Call<MixinResponse<RouteTokenResponse>>

    @GET("profile")
    suspend fun profile(
        @Query("version") version: String,
    ): MixinResponse<ProfileResponse>

    @GET("web3/tokens")
    suspend fun web3Tokens(
        @Query("source") source: String,
        @Query("version") version: String = BuildConfig.VERSION_NAME,
    ): MixinResponse<List<SwapToken>>

    @GET("web3/quote")
    suspend fun web3Quote(
        @Query("inputMint") inputMint: String,
        @Query("outputMint") outputMint: String,
        @Query("amount") amount: String,
        @Query("slippage") slippage: String,
        @Query("source") source: String,
    ): MixinResponse<QuoteResponse>

    @POST("web3/swap")
    suspend fun web3Swap(
        @Body swapRequest: SwapRequest,
    ): MixinResponse<SwapResponse>

    @GET("web3/transactions/{txhash}")
    suspend fun getWeb3Tx(
        @Path("txhash") txhash: String,
    ): MixinResponse<Tx>

    @POST("web3/transactions")
    suspend fun postWeb3Tx(
        @Body rawTx: PostTxRequest,
    ): MixinResponse<Unit>

    @POST("web3/transactions/parse")
    suspend fun parseWeb3Tx(
        @Body parseTxRequest: ParseTxRequest,
    ): MixinResponse<ParsedTx>

    @GET("web3/tokens/{address}")
    suspend fun getSwapToken(
        @Path("address") address: String,
    ): MixinResponse<SwapToken?>

    @GET("web3/tokens/search/{query}")
    suspend fun searchTokens(
        @Path("query") query: String,
    ): MixinResponse<List<SwapToken>>

    @GET("markets/{id}/price-history")
    suspend fun priceHistory(
        @Path("id") assetId: String,
        @Query("type") type: String,
    ): MixinResponse<HistoryPrice>

    @GET("markets/{id}")
    suspend fun market(
        @Path("id") assetId: String,
    ): MixinResponse<Market>
}
