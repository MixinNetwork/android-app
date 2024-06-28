package one.mixin.android.ui.home.web3

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.ParseTxRequest
import one.mixin.android.api.request.web3.PostTxRequest
import one.mixin.android.api.request.web3.PriorityFeeRequest
import one.mixin.android.api.request.web3.PriorityLevel
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.response.web3.PriorityFeeResponse
import one.mixin.android.api.service.Web3Service
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import org.sol4k.exception.RpcException
import org.web3j.exceptions.MessageDecodingException
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.utils.Numeric
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class BrowserWalletBottomSheetViewModel
    @Inject
    internal constructor(
        private val assetRepo: TokenRepository,
        private val userRepo: UserRepository,
        private val web3Service: Web3Service,
        private val tip: Tip,
    ) : ViewModel() {
        suspend fun ethGasLimit(
            chain: Chain,
            transaction: Transaction,
        ) = withContext(Dispatchers.IO) {
            WalletConnectV2.ethEstimateGas(chain, transaction)?.run {
                val defaultLimit = if (chain.chainReference == Chain.Ethereum.chainReference) BigInteger.valueOf(4712380L) else null
                convertToGasLimit(this, defaultLimit)
            }
        }

        private fun convertToGasLimit(
            estimate: EthEstimateGas,
            defaultLimit: BigInteger?,
        ): BigInteger? {
            return if (estimate.hasError()) {
                if (estimate.error.code === -32000) // out of gas
                    {
                        defaultLimit
                    } else {
                    BigInteger.ZERO
                }
            } else if (estimate.amountUsed.compareTo(BigInteger.ZERO) > 0) {
                estimate.amountUsed
            } else if (defaultLimit == null || defaultLimit.equals(BigInteger.ZERO)) {
                BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS)
            } else {
                defaultLimit
            }
        }

        suspend fun ethGasPrice(chain: Chain) =
            withContext(Dispatchers.IO) {
                WalletConnectV2.ethGasPrice(chain)?.run {
                    try {
                        this.gasPrice
                    } catch (e: MessageDecodingException) {
                        result?.run { Numeric.toBigInt(this) }
                    }
                }
            }

        suspend fun ethMaxPriorityFeePerGas(chain: Chain) =
            withContext(Dispatchers.IO) {
                WalletConnectV2.ethMaxPriorityFeePerGas(chain)?.run {
                    try {
                        this.maxPriorityFeePerGas
                    } catch (e: MessageDecodingException) {
                        result?.run { Numeric.toBigInt(this) }
                    }
                }
            }

        suspend fun getWeb3Priv(
            context: Context,
            pin: String,
            chainId: String,
        ): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(context), pin, result.getOrThrow())
            return tipPrivToPrivateKey(spendKey, chainId)
        }

        suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

        suspend fun web3Tokens(address: List<String>): List<Web3Token> {
            val resp = web3Service.web3Tokens(address.joinToString(","))
            return if (resp.isSuccess) {
                resp.data ?: emptyList()
            } else {
                emptyList()
            }
        }

        suspend fun getPriorityFee(tx: String, priorityLevel: PriorityLevel): PriorityFeeResponse? {
            return handleMixinResponse(
                invokeNetwork = { web3Service.getPriorityFee(PriorityFeeRequest(tx, priorityLevel)) },
                successBlock = {
                    it.data
                },
            )
        }

        suspend fun parseWeb3Tx(tx: String): ParsedTx? {
            var meet401 = false
            val parsedTx = handleMixinResponse(
                invokeNetwork = { assetRepo.parseWeb3Tx(ParseTxRequest(tx)) },
                successBlock = { it.data },
                failureBlock = {
                    if (it.errorCode == 401) {
                        meet401 = true
                        return@handleMixinResponse true
                    }
                    return@handleMixinResponse false
                }
            )
            if (parsedTx == null && meet401) {
                userRepo.getBotPublicKey(ROUTE_BOT_USER_ID)
                return parseWeb3Tx(tx)
            } else {
                return parsedTx
            }
        }

        suspend fun postRawTx(rawTx: String) {
            val resp = assetRepo.postRawTx(PostTxRequest(rawTx))
            if (!resp.isSuccess) {
                val err = resp.error!!
                // simulate RpcException
                throw RpcException(err.code, err.description, err.toString())
            }
        }
    }
