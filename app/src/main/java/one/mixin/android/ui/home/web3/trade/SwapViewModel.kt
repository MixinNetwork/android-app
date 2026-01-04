package one.mixin.android.ui.home.web3.trade

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.LimitOrderRequest
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.util.ErrorHandler.Companion.INVALID_QUOTE_AMOUNT
import one.mixin.android.util.ErrorHandler.Companion.NO_AVAILABLE_QUOTE
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.analytics.AnalyticsTracker.TradeQuoteReason
import one.mixin.android.util.analytics.AnalyticsTracker.TradeQuoteType
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.route.Order
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SwapViewModel

    @Inject
    internal constructor(
        private val assetRepository: AssetRepository,
        private val jobManager: MixinJobManager,
        private val tokenRepository: TokenRepository,
        private val userRepository: UserRepository,
        private val web3Repository: Web3Repository,
        private val walletDatabase: WalletDatabase,
    ) : ViewModel() {

    suspend fun getBotPublicKey(botId: String, force: Boolean) = userRepository.getBotPublicKey(botId, force)

    suspend fun web3Tokens(source: String, category: String? = null): MixinResponse<List<SwapToken>> = assetRepository.web3Tokens(source, category)

    suspend fun web3Quote(
        inputMint: String,
        outputMint: String,
        amount: String,
        source: String,
    ): MixinResponse<QuoteResult> = assetRepository.web3Quote(inputMint, outputMint, amount, source)

    suspend fun web3Swap(
        swapRequest: SwapRequest,
    ): MixinResponse<SwapResponse> {
        addRouteBot()
        return assetRepository.web3Swap(swapRequest)
    }

    // Limit order APIs (create is used by UI; others not used yet)
    suspend fun createLimitOrder(request: LimitOrderRequest): MixinResponse<CreateLimitOrderResponse> {
        addRouteBot()
        return assetRepository.createLimitOrder(request)
    }
    suspend fun getLimitOrders(category: String = "all", limit: Int? = 50, offset: String?, state: String?, walletId: String?): MixinResponse<List<Order>>  {
        val response = assetRepository.getLimitOrders(category, limit, offset, state, walletId)
        response.data?.let {
            web3Repository.inserOrders(it)
        }
        return response
    }

    suspend fun cancelLimitOrder(id: String): MixinResponse<Order> = withContext(Dispatchers.IO) {
        assetRepository.cancelLimitOrder(id)
    }

    suspend fun getPendingOrdersFromDb(walletId: String): List<Order> {
        return web3Repository.getPendingOrdersByWallet(walletId)
    }

    suspend fun quote(
        context: Context,
        symbol: String,
        inputMint: String?,
        outputMint: String?,
        amount: String,
        source: String,
    ) : Result<QuoteResult?> {
        val type = if(source.isEmpty()){
            TradeQuoteType.LIMIT
        }else{
            TradeQuoteType.SWAP
        }
        return if (amount.isNotBlank() && inputMint != null && outputMint != null) {
            runCatching {
                val response = web3Quote(
                    inputMint = inputMint,
                    outputMint = outputMint,
                    amount = amount,
                    source = source,
                )
                return if (response.isSuccess) {
                    AnalyticsTracker.trackTradeQuote(
                        AnalyticsTracker.TradeQuoteResult.SUCCESS,
                        type
                    )
                    Result.success(requireNotNull(response.data))
                } else if (response.errorCode == INVALID_QUOTE_AMOUNT) {
                    AnalyticsTracker.trackTradeQuote(
                        AnalyticsTracker.TradeQuoteResult.FAILURE,
                        type,
                        TradeQuoteReason.INVALID_AMOUNT
                    )
                    val extra = response.error?.extra?.asJsonObject?.get("data")?.asJsonObject
                    return when {
                        extra != null -> {
                            val min = extra.get("min")?.asString
                            val max = extra.get("max")?.asString
                            when {
                                !min.isNullOrBlank() && !max.isNullOrBlank() -> Result.failure(AmountException(context.getString(R.string.single_transaction_should_be_between, min, symbol, max, symbol), max, min))
                                !min.isNullOrBlank() -> Result.failure(AmountException(context.getString(R.string.single_transaction_should_be_greater_than, min, symbol), null, min))
                                !max.isNullOrBlank() -> Result.failure(AmountException(context.getString(R.string.single_transaction_should_be_less_than, max, symbol), max, null))
                                else -> Result.failure(Throwable(context.getMixinErrorStringByCode(response.errorCode, response.errorDescription)))
                            }
                        }

                        else -> Result.failure(Throwable(context.getMixinErrorStringByCode(response.errorCode, response.errorDescription)))
                    }
                } else {
                    val reason = if (response.errorCode == NO_AVAILABLE_QUOTE) {
                        TradeQuoteReason.NO_AVAILABLE_QUOTE
                    } else {
                        TradeQuoteReason.OTHER
                    }
                    AnalyticsTracker.trackTradeQuote(
                        AnalyticsTracker.TradeQuoteResult.FAILURE,
                        type,
                        reason
                    )
                    Result.failure(Throwable(context.getMixinErrorStringByCode(response.errorCode, response.errorDescription)))
                }
            }
        } else {
             Result.success(null)
        }
    }

    suspend fun searchTokens(query: String, inMixin: Boolean) = assetRepository.searchTokens(query, inMixin)

    suspend fun syncAndFindTokens(assetIds: List<String>): List<TokenItem> =
        tokenRepository.syncAndFindTokens(assetIds)

    suspend fun checkAndSyncTokens(assetIds: List<String>) =
        tokenRepository.checkAndSyncTokens(assetIds)

    suspend fun findToken(assetId: String): TokenItem? {
       return tokenRepository.findAssetItemById(assetId)
    }

    suspend fun syncAsset(assetId: String): TokenItem? = withContext(Dispatchers.IO) {
        tokenRepository.syncAsset(assetId)
    }

    suspend fun findAssetItemsWithBalance() = tokenRepository.findAssetItemsWithBalance()

    suspend fun findAssetItems() = tokenRepository.allAssetItems()

    suspend fun findWeb3AssetItemsWithBalance(walletId: String) = tokenRepository.findWeb3AssetItemsWithBalance(walletId)

    suspend fun findWeb3AssetItems(walletId: String) = tokenRepository.findWeb3TokenItems(walletId)

    fun walletOrders() = tokenRepository.walletOrders()

    fun getOrderById(orderId: String) = tokenRepository.getOrderById(orderId)

    fun observeOrder(orderId: String) = tokenRepository.observeOrder(orderId)

    private fun addRouteBot() {
        viewModelScope.launch(Dispatchers.IO) {
            val bot = userRepository.getUserById(ROUTE_BOT_USER_ID)
            if (bot == null || bot.relationship != "FRIEND") {
                jobManager.addJobInBackground(UpdateRelationshipJob(RelationshipRequest(ROUTE_BOT_USER_ID, RelationshipAction.ADD.name)))
            }
        }
    }

    suspend fun checkMarketById(assetId: String, force: Boolean): MarketItem? = withContext(Dispatchers.IO) {
        return@withContext tokenRepository.checkMarketById(assetId, force)
    }

    fun tokenExtraFlow(token: SwapToken): Flow<String?> {
        val walletId = token.walletId
        return if (walletId.isNullOrBlank().not()) {
            tokenRepository.web3TokenExtraFlow(walletId, token.assetId)
        } else {
            tokenRepository.tokenExtraFlow(token.assetId)
        }
    }

    suspend fun web3TokenItemById(walletId: String, assetId: String) = withContext(Dispatchers.IO) {
        web3Repository.web3TokenItemById(walletId, assetId)
    }

    suspend fun findWeb3WalletById(walletId: String) = withContext(Dispatchers.IO) {
        web3Repository.findWalletById(walletId)
    }

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun getTokenByWalletAndAssetId(walletId: String, assetId: String): Web3TokenItem? = withContext(Dispatchers.IO) {
        web3Repository.getTokenByWalletAndAssetId(walletId, assetId)
    }
    fun assetItemFlow(assetId: String): Flow<TokenItem?> {
        return tokenRepository.assetItemFlow(assetId)
    }

    suspend fun getAddressesByChainId(walletId: String, chainId: String) = web3Repository.getAddressesByChainId(walletId, chainId)

    suspend fun getAddresses(walletId: String) = web3Repository.getAddresses(walletId)

    fun getPendingOrderCountByWallet(walletId: String): Flow<Int> {
        return walletDatabase.orderDao().getPendingOrderCountByWallet(walletId)
    }

    suspend fun refreshPendingOrders(): Boolean = withContext(Dispatchers.IO) {
        val orderDao = walletDatabase.orderDao()
        val pending = orderDao.getPendingOrders()
        if (pending.isEmpty()) return@withContext false
        val ids = pending.map { it.orderId }
        val resp = assetRepository.getLimitOrders(ids)
        if (resp.isSuccess && resp.data != null) {
            orderDao.insertListSuspend(resp.data!!)
        }
        return@withContext true
    }

    suspend fun refreshOrders(walletId: String) = withContext(Dispatchers.IO) {
        val offsetKey = "order_offset_$walletId"
        val offset = Web3PropertyHelper.findValueByKey(offsetKey, "")
        refreshOrdersInternal(walletId, offset.ifEmpty { null }, offsetKey)
    }

    private suspend fun refreshOrdersInternal(walletId: String, offset: String?, offsetKey: String, previousLastCreatedAt: String? = null) {
        val response = assetRepository.getLimitOrders(category = "all", limit = 20, offset = offset, state = null, walletId = walletId)

        if (response.isSuccess && response.data != null) {
            val orders = response.data!!
            if (orders.isEmpty()) return
            
            val currentLastCreatedAt = orders.lastOrNull()?.createdAt
            if (currentLastCreatedAt != null && currentLastCreatedAt == previousLastCreatedAt) {
                Timber.w("refreshOrders: Detected duplicate offset for wallet $walletId, stopping pagination")
                return
            }
            
            walletDatabase.orderDao().insertListSuspend(orders)
            val sessionWalletId = Session.getAccountId() ?: return
            val assetIds = orders.flatMap { listOfNotNull<String>(it.payAssetId, it.receiveAssetId) }
                .filter { it.isNotEmpty() }
                .toSet()
                .toList()
            if (assetIds.isNotEmpty()) {
                val tokensNow = walletDatabase.web3TokenDao().findWeb3TokenItemsByIdsSync(sessionWalletId, assetIds)
                val existingIds = tokensNow.map { it.assetId }.toSet()
                val missingIds = assetIds.filter { it !in existingIds }
                if (missingIds.isNotEmpty()) {
                    refreshAsset(missingIds)
                }
                val tokensReady = walletDatabase.web3TokenDao().findWeb3TokenItemsByIdsSync(sessionWalletId, assetIds)
                val chainIds = tokensReady.map { it.chainId }.distinct()
                if (chainIds.isNotEmpty()) {
                    fetchChain(chainIds)
                }
            }

            if (currentLastCreatedAt != null) {
                Web3PropertyHelper.updateKeyValue(offsetKey, currentLastCreatedAt)
            }

            val fetchedSize = orders.size
            if (fetchedSize >= 20) {
                val lastCreate = orders.lastOrNull()?.createdAt ?: return
                refreshOrdersInternal(walletId, lastCreate, offsetKey, currentLastCreatedAt)
            }
        }
    }

    private suspend fun refreshAsset(ids: List<String>) {
        val walletId = Session.getAccountId()!!
        val resp = tokenRepository.fetchTokenSuspend(ids)
        val tokens = resp.data ?: return
        val web3Tokens = tokens.map { t ->
            Web3Token(
                walletId = walletId,
                assetId = t.assetId,
                chainId = t.chainId,
                name = t.name,
                assetKey = t.assetKey,
                symbol = t.symbol,
                iconUrl = t.iconUrl,
                precision = t.precision,
                kernelAssetId = "",
                balance = "0",
                priceUsd = t.priceUsd,
                changeUsd = t.changeUsd,
            )
        }
        if (web3Tokens.isNotEmpty()) {
            walletDatabase.web3TokenDao().insertList(web3Tokens)
        }
    }

    private suspend fun fetchChain(chainIds: List<String>) {
        chainIds.forEach { chainId ->
            try {
                if (walletDatabase.web3ChainDao().chainExists(chainId) == null) {
                    val response = tokenRepository.getChainById(chainId)
                    if (response.isSuccess) {
                        val chain = response.data
                        if (chain != null) {
                            walletDatabase.web3ChainDao().insert(
                                Web3Chain(
                                    chainId = chain.chainId,
                                    name = chain.name,
                                    symbol = chain.symbol,
                                    iconUrl = chain.iconUrl,
                                    threshold = chain.threshold,
                                )
                            )
                            Timber.d("Successfully inserted ${chain.name} chain into database")
                        } else {
                            Timber.d("No chain found for chainId: $chainId")
                        }
                    } else {
                        Timber.e("Failed to fetch chain $chainId: ${response.errorCode} - ${response.errorDescription}")
                    }
                } else {
                    Timber.d("Chain $chainId already exists in local database, skipping fetch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception occurred while fetching chain $chainId")
            }
        }
    }
}
