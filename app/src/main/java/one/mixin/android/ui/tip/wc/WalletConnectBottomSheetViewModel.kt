package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS
import one.mixin.android.api.service.TipService
import one.mixin.android.repository.TokenRepository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import org.sol4k.Connection
import org.sol4k.RpcUrl
import org.web3j.exceptions.MessageDecodingException
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

@HiltViewModel
class WalletConnectBottomSheetViewModel
    @Inject
    internal constructor(
        private val assetRepo: TokenRepository,
        private val tipService: TipService,
        private val tip: Tip,
    ) : ViewModel() {
        suspend fun getV2SessionProposal(topic: String): Wallet.Model.SessionProposal? {
            return withContext(Dispatchers.IO) {
                WalletConnectV2.getSessionProposal(topic)
            }
        }

        suspend fun getV2SessionRequest(topic: String): Wallet.Model.SessionRequest? {
            return withContext(Dispatchers.IO) {
                WalletConnectV2.getSessionRequest(topic)
            }
        }

        suspend fun ethGasLimit(
            chain: Chain,
            transaction: Transaction,
        ) = withContext(Dispatchers.IO) {
            WalletConnectV2.ethEstimateGas(chain, transaction)?.run {
                val defaultLimit = if (chain.chainReference == "1") BigInteger.valueOf(4712380L) else null
                convertToGasLimit(this, defaultLimit)
            }
        }

        private fun convertToGasLimit(estimate: EthEstimateGas, defaultLimit: BigInteger?): BigInteger? {
            return if (estimate.hasError()) {
                if (estimate.error.code === -32000) //out of gas
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

        suspend fun ethGasPrice(chain: Chain) = withContext(Dispatchers.IO) {
            WalletConnectV2.ethGasPrice(chain)?.run {
                try {
                    this.gasPrice
                } catch (e: MessageDecodingException) {
                    result?.run { Numeric.toBigInt(this) }
                }
            }
        }

        suspend fun ethMaxPriorityFeePerGas(chain: Chain) = withContext(Dispatchers.IO) {
            WalletConnectV2.ethMaxPriorityFeePerGas(chain)?.run {
                try {
                    this.maxPriorityFeePerGas
                } catch (e: MessageDecodingException) {
                    result?.run { Numeric.toBigInt(this) }
                }
            }
        }

        fun parseV2SignData(address: String, sessionRequest: Wallet.Model.SessionRequest): WalletConnect.WCSignData.V2SignData<*>? {
            return WalletConnectV2.parseSessionRequest(address, sessionRequest)
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

        fun sendTransaction(
            transaction: org.sol4k.CompiledTransaction,
            sessionRequest: Wallet.Model.SessionRequest,
        ): String? {
            val connection = Connection(RpcUrl.MAINNNET)
            val signature: String = connection.sendTransaction(transaction.serialize())
            Timber.d("signature $signature")
            WalletConnectV2.approveSolanaTransaction(signature, sessionRequest)
            return null
        }

        fun sendTransaction(
            version: WalletConnect.Version,
            chain: Chain,
            sessionRequest: Wallet.Model.SessionRequest,
            signedTransactionData: String,
        ): String? {
            try {
                when (version) {
                    WalletConnect.Version.V2 -> WalletConnectV2.sendTransaction(chain, sessionRequest, signedTransactionData)
                    WalletConnect.Version.TIP -> {}
                }
                return null
            } catch (e: Exception) {
                val errorInfo = e.stackTraceToString()
                Timber.d(
                    "${
                        when (version) {
                            WalletConnect.Version.V2 -> WalletConnectV2.TAG
                            else -> WalletConnectTIP.TAG
                        }
                    } $errorInfo",
                )
                return errorInfo
            }
        }
    }
