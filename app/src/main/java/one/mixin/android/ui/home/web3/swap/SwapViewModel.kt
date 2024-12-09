package one.mixin.android.ui.home.web3.swap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.service.Web3Service
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.util.ErrorHandler.Companion.INVALID_QUOTE_AMOUNT
import one.mixin.android.util.getMixinErrorStringByCode
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
        private val web3Service: Web3Service,
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
                        val builder = StringBuilder(context.getMixinErrorStringByCode(response.errorCode, response.errorDescription))
                        val extra = response.error?.extra?.asJsonObject?.get("data")?.asJsonObject
                        if (extra != null) {
                            val min = extra.get("min").asString
                            val max = extra.get("max").asString
                            if (!min.isNullOrBlank() && !max.isNullOrBlank()) {
                                builder.append(context.getString(R.string.single_transaction_should_be_between, min, max))
                            } else if (!min.isNullOrBlank()) {
                                builder.append(context.getString(R.string.single_transaction_should_be_greater_than, min))
                            } else if (!max.isNullOrBlank()) {
                                builder.append(context.getString(R.string.single_transaction_should_be_less_than, max))
                            }
                            @Suppress("UNUSED_VARIABLE")
                            val source = extra.get("source").asString
                        }
                        Result.failure(Throwable(builder.toString()))
                    } else {
                        Result.failure(Throwable(context.getMixinErrorStringByCode(response.errorCode, response.errorDescription)))
                    }
                }
            } else {
                 Result.success(null)
            }
        }

        suspend fun searchTokens(query: String) = assetRepository.searchTokens(query)

        suspend fun web3Tokens(chain: String, address: List<String>? = null): List<Web3Token> {
            return handleMixinResponse(
                invokeNetwork = { web3Service.web3Tokens(chain = chain, addresses = address?.joinToString(",")) },
                successBlock = {
                    return@handleMixinResponse it.data
                },
            ) ?: emptyList()
        }

        suspend fun syncAndFindTokens(assetIds: List<String>): List<TokenItem> =
            tokenRepository.syncAndFindTokens(assetIds)

        suspend fun checkAndSyncTokens(assetIds: List<String>) =
            tokenRepository.checkAndSyncTokens(assetIds)

        suspend fun findToken(assetId: String): TokenItem? {
           return tokenRepository.findAssetItemById(assetId)
        }

        suspend fun allAssetItems() = tokenRepository.allAssetItems()

        private fun addRouteBot(){
            viewModelScope.launch(Dispatchers.IO) {
                val bot = userRepository.getUserById(ROUTE_BOT_USER_ID)
                if (bot == null || bot.relationship != "FRIEND") {
                    jobManager.addJobInBackground(UpdateRelationshipJob(RelationshipRequest(ROUTE_BOT_USER_ID, RelationshipAction.ADD.name)))
                }
            }
        }

}
