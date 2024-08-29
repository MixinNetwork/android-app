package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS
import one.mixin.android.api.request.web3.PostTxRequest
import one.mixin.android.api.service.TipService
import one.mixin.android.repository.TokenRepository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.web3.js.getSolanaRpc
import org.sol4k.VersionedTransaction
import org.sol4k.exception.RpcException
import org.web3j.crypto.Hash
import org.web3j.exceptions.MessageDecodingException
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.Base64
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
                        result?.run { Numeric.decodeQuantity(this) }
                    }
                }
            }

        suspend fun ethMaxPriorityFeePerGas(chain: Chain) =
            withContext(Dispatchers.IO) {
                WalletConnectV2.ethMaxPriorityFeePerGas(chain)?.run {
                    try {
                        this.maxPriorityFeePerGas
                    } catch (e: MessageDecodingException) {
                        result?.run { Numeric.decodeQuantity(this) }
                    }
                }
            }

        fun parseV2SignData(
            address: String,
            sessionRequest: Wallet.Model.SessionRequest,
        ): WalletConnect.WCSignData.V2SignData<*>? {
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

        suspend fun sendTransaction(
            signedTransactionData: Any,
            chain: Chain,
            sessionRequest: Wallet.Model.SessionRequest,
        ): String? {
            val signature: String
            val rawTx = if (chain == Chain.Solana) {
                signedTransactionData as VersionedTransaction
                signature = signedTransactionData.signatures.first()
                Base64.getEncoder().encodeToString(signedTransactionData.serialize())
            } else {
                signedTransactionData as String
                signature =  Hash.sha3(signedTransactionData)
                signedTransactionData
            }
            try {
                assetRepo.postRawTx(PostTxRequest(rawTx, chain.getWeb3ChainId()))
            } catch (e: Exception) {
                WalletConnectV2.rejectRequest(e.message, sessionRequest)
                throw e
            }
            if (chain == Chain.Solana) {
                WalletConnectV2.approveSolanaTransaction(signature, sessionRequest)
            } else {
                WalletConnectV2.approveRequestInternal(signature, sessionRequest)
            }
            return null
        }
    }
