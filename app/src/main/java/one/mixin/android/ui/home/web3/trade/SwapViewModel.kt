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
import one.mixin.android.api.response.LimitOrder
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.util.ErrorHandler.Companion.INVALID_QUOTE_AMOUNT
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
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
    ) : ViewModel() {

    suspend fun getBotPublicKey(botId: String, force: Boolean) = userRepository.getBotPublicKey(botId, force)

    suspend fun web3Tokens(source: String): MixinResponse<List<SwapToken>> = assetRepository.web3Tokens(source)

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
    suspend fun getLimitOrders(category: String = "all", limit: Int = 50, offset: String?): MixinResponse<List<LimitOrder>> =
        assetRepository.getLimitOrders(category, limit, offset)
    suspend fun getLimitOrder(id: String): MixinResponse<LimitOrder> = assetRepository.getLimitOrder(id)
    suspend fun cancelLimitOrder(id: String): MixinResponse<LimitOrder> = assetRepository.cancelLimitOrder(id)

    suspend fun quote(
        context: Context,
        symbol: String,
        inputMint: String?,
        outputMint: String?,
        amount: String,
        source: String,
    ) : Result<QuoteResult?> {
        return if (amount.isNotBlank() && inputMint != null && outputMint != null) {
            runCatching {
                val response = web3Quote(
                    inputMint = inputMint,
                    outputMint = outputMint,
                    amount = amount,
                    source = source,
                )
                return if (response.isSuccess) {
                    Result.success(requireNotNull(response.data))
                } else if (response.errorCode == INVALID_QUOTE_AMOUNT) {
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

    fun swapOrders() = tokenRepository.swapOrders()

    fun getOrderById(orderId: String) = tokenRepository.getOrderById(orderId)

    private fun addRouteBot() {
        viewModelScope.launch(Dispatchers.IO) {
            val bot = userRepository.getUserById(ROUTE_BOT_USER_ID)
            if (bot == null || bot.relationship != "FRIEND") {
                jobManager.addJobInBackground(UpdateRelationshipJob(RelationshipRequest(ROUTE_BOT_USER_ID, RelationshipAction.ADD.name)))
            }
        }
    }

    suspend fun checkMarketById(assetId: String): MarketItem? = withContext(Dispatchers.IO) {
        return@withContext tokenRepository.checkMarketById(assetId, true)
    }

    fun tokenExtraFlow(token: SwapToken): Flow<String?> {
        return if (token.walletId.isNullOrBlank().not()) {
            tokenRepository.web3TokenExtraFlow(token.walletId,token.assetId)
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
}
