package one.mixin.android.ui.home.web3

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.EstimateFeeResponse
import one.mixin.android.api.request.web3.Web3RawTransactionRequest
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.util.ErrorHandler
import org.sol4k.exception.RpcException
import javax.inject.Inject

@HiltViewModel
class BrowserWalletBottomSheetViewModel
    @Inject
    internal constructor(
        private val assetRepo: TokenRepository,
        private val userRepo: UserRepository,
        private val web3Repository: Web3Repository,
        private val tip: Tip,
    ) : ViewModel() {
        suspend fun getWeb3Priv(
            context: Context,
            pin: String,
            chainId: String,
        ): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, result.getOrThrow())
            return tipPrivToPrivateKey(spendKey, chainId)
        }

        suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

        suspend fun solanaWeb3Tokens(addresses: List<String>): List<SwapToken> {
            val result = mutableListOf<SwapToken>()
            addresses.forEach { address ->
                val t = web3Repository.web3TokenItemByAddress(address)
                if (t != null) {
                    result.add(t.toSwapToken())
                }
                val resp = assetRepo.getSwapToken(address)
                if (resp.isSuccess) {
                    result.add(resp.data!!)
                }
            }
            return result
        }

        suspend fun getPriorityFee(tx: String): EstimateFeeResponse? {
            return handleMixinResponse(
                invokeNetwork = { web3Repository.estimateFee(EstimateFeeRequest(Constants.ChainId.SOLANA_CHAIN_ID, tx)) },
                successBlock = {
                    it.data
                },
            )
        }

        suspend fun simulateWeb3Tx(tx: String, chainId: String, from: String?): ParsedTx? {
            var meet401 = false
            var parsedTx: ParsedTx? = null
            handleMixinResponse(
                invokeNetwork = { assetRepo.simulateWeb3Tx(Web3RawTransactionRequest(chainId, tx,from)) },
                successBlock = { parsedTx = it.data },
                failureBlock = {
                    if (it.errorCode == ErrorHandler.SIMULATE_TRANSACTION_FAILED) {
                        parsedTx = ParsedTx(code = ErrorHandler.SIMULATE_TRANSACTION_FAILED)
                        return@handleMixinResponse true
                    } else if (it.errorCode == 401) {
                        meet401 = true
                        return@handleMixinResponse true
                    }
                    return@handleMixinResponse false
                }
            )
            if (parsedTx == null && meet401) {
                userRepo.getBotPublicKey(ROUTE_BOT_USER_ID, true)
                return simulateWeb3Tx(tx, chainId, from)
            } else {
                return parsedTx
            }
        }

        suspend fun postRawTx(rawTx: String, web3ChainId: String, account: String, assetId: String? = null) {
            val resp = assetRepo.postRawTx(Web3RawTransactionRequest(web3ChainId, rawTx, account), assetId)
            if (!resp.isSuccess) {
                val err = resp.error!!
                // simulate RpcException
                throw RpcException(err.code, err.description, err.toString())
            }
        }

        suspend fun estimateFee(request: EstimateFeeRequest) = web3Repository.estimateFee(request)
    }
