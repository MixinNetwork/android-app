package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.OrderRequest
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RoutePriceRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.EstimateFeeResponse
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.request.web3.ParseTxRequest
import one.mixin.android.api.request.web3.PostTxRequest
import one.mixin.android.api.request.web3.RpcRequest
import one.mixin.android.api.request.web3.StakeRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.response.RouteCreateTokenResponse
import one.mixin.android.api.response.RouteOrderResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.StakeResponse
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.api.response.web3.Web3WalletResponse
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import one.mixin.android.vo.Card
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.HistoryPrice
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.route.SwapOrder
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
    ): MixinResponse<QuoteResult>

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
        @Query("source") source: String?
    ): MixinResponse<List<SwapToken>>

    @POST("web3/stake")
    suspend fun stakeSol(
        @Body stakeRequest: StakeRequest,
    ): MixinResponse<StakeResponse>

    @GET("web3/stake/{account}")
    suspend fun getStakeAccounts(
        @Path("account") account: String,
    ): MixinResponse<List<StakeAccount>>

    @GET("web3/stake/activation")
    suspend fun getStakeAccountActivations(
        @Query("accounts") accounts: String,
    ): MixinResponse<List<StakeAccountActivation>>

    @GET("web3/stake/validators")
    suspend fun getStakeValidators(
        @Query("votePubkeys") votePubkeys: String? = null,
    ): MixinResponse<List<Validator>>

    @GET("web3/stake/validators/search/{query}")
    suspend fun searchStakeValidators(
        @Path("query") query: String,
    ): MixinResponse<List<Validator>>

    @GET("web3/swap/orders")
    suspend fun orders(
        @Query("offset") offset: String?,
        @Query("limit") limit: Int
    ) : MixinResponse<List<SwapOrder>>

    @GET("markets/{id}/price-history")
    suspend fun priceHistory(
        @Path("id") assetId: String,
        @Query("type") type: String,
    ): MixinResponse<HistoryPrice>

    @GET("markets/{id}")
    suspend fun market(
        @Path("id") assetId: String,
    ): MixinResponse<Market>

    @GET("markets")
    suspend fun markets(@Query("category") category: String? = null, @Query("limit") limit: Int? = null, @Query("sort") sort: String? = null, @Query("offset") offset: Int? = null): MixinResponse<List<Market>>

    @POST("markets/fetch")
    suspend fun fetchMarket(@Body ids: List<String>): MixinResponse<List<Market>>

    @GET("markets/search/{query}")
    suspend fun searchMarket(@Path("query") query: String):MixinResponse<List<Market>>

    @GET("markets/globals")
    suspend fun globalMarket():MixinResponse<GlobalMarket>

    @POST("markets/{id}/favorite")
    suspend fun favorite(@Path("id") coinId: String): MixinResponse<Unit>

    @POST("markets/{id}/unfavorite")
    suspend fun unfavorite(@Path("id") coinId: String): MixinResponse<Unit>

    @POST("prices/alerts")
    suspend fun addAlert(@Body alert: AlertRequest):MixinResponse<Alert>

    @GET("prices/alerts")
    suspend fun alerts():MixinResponse<List<Alert>>

    @POST("prices/alerts/{id}")
    suspend fun updateAlert(@Path("id") alertId: String, @Body request: AlertUpdateRequest): MixinResponse<Alert>

    @POST("web3/estimate-fee")
    suspend fun estimateFee(
        @Body request: EstimateFeeRequest,
    ): MixinResponse<EstimateFeeResponse>

    @POST("web3/rpc")
    suspend fun rpc(
        @Query("chain_id") chainId: String,
        @Body request: RpcRequest,
    ): MixinResponse<String>

    @POST("wallets")
    suspend fun createWallet(
        @Body request: WalletRequest
    ): MixinResponse<Web3WalletResponse>

    @GET("wallets")
    suspend fun getWallets(): MixinResponse<List<Web3Wallet>>

    @GET("wallets/{id}")
    suspend fun getWallet(
        @Path("id") id: String
    ): MixinResponse<Web3Wallet>

    @POST("wallets/{id}/delete")
    suspend fun destroyWallet(
        @Path("id") id: String
    ): MixinResponse<Unit>

    @GET("wallets/{id}/assets")
    suspend fun getWalletAssets(
        @Path("id") id: String
    ): MixinResponse<List<Web3Token>>

    @POST("addresses")
    suspend fun createAddress(
        @Body request: Web3AddressRequest
    ): MixinResponse<List<Web3Address>>

    @GET("wallets/{id}/addresses")
    suspend fun getWalletAddresses(
        @Path("id") walletId: String
    ): MixinResponse<List<Web3Address>>

    @GET("transactions")
    suspend fun getAllTransactions(
        @Query("address") address: String,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = 30
    ): MixinResponse<List<Web3Transaction>>

    @GET("transactions/{id}")
    suspend fun getTransaction(
        @Path("id") id: String
    ): MixinResponse<Web3Transaction>

    @GET("assets/{id}")
    suspend fun getAssetByAddress(
        @Path("id") id: String,
        @Query("address") address: String,
    ): MixinResponse<Web3Token>
}
