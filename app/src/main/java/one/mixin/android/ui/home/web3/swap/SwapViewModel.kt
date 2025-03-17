package one.mixin.android.ui.home.web3.swap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
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
    ) : ViewModel() {

    suspend fun getBotPublicKey(botId: String, force: Boolean) = userRepository.getBotPublicKey(botId, force)

    suspend fun web3Tokens(source: String): MixinResponse<List<SwapToken>> = assetRepository.web3Tokens(source)

    suspend fun web3Quote(
        inputMint: String,
        outputMint: String,
        amount: String,
        slippage: String,
        source: String,
    ): MixinResponse<QuoteResult> = assetRepository.web3Quote(inputMint, outputMint, amount, slippage, source)

    suspend fun web3Swap(
        swapRequest: SwapRequest,
    ): MixinResponse<SwapResponse> {
        addRouteBot()
        return assetRepository.web3Swap(swapRequest)
    }

    suspend fun quote(
        context: Context,
        symbol: String,
        inputMint: String?,
        outputMint: String?,
        amount: String,
        slippage: String,
        source: String,
    ) : Result<QuoteResult?> {
        return if (amount.isNotBlank() && inputMint != null && outputMint != null) {
            runCatching {
                val response = web3Quote(
                    inputMint = inputMint,
                    outputMint = outputMint,
                    amount = amount,
                    slippage = slippage,
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

    suspend fun allAssetItems() = tokenRepository.allAssetItems()

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

    fun tokenExtraFlow(assetId: String) = tokenRepository.tokenExtraFlow(assetId)
}
