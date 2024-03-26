package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import com.walletconnect.web3.wallet.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.service.TipService
import one.mixin.android.repository.TokenRepository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import org.web3j.protocol.core.methods.request.Transaction
import timber.log.Timber
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

        suspend fun ethEstimateGas(
            chain: Chain,
            transaction: Transaction,
        ) = withContext(Dispatchers.IO) { WalletConnectV2.ethEstimateGas(chain, transaction) }

        suspend fun ethGasPrice(chain: Chain) = withContext(Dispatchers.IO) { WalletConnectV2.ethGasPrice(chain) }

        suspend fun ethMaxPriorityFeePerGas(chain: Chain) = withContext(Dispatchers.IO) { WalletConnectV2.ethMaxPriorityFeePerGas(chain) }

        fun parseV2SignData(sessionRequest: Wallet.Model.SessionRequest): WalletConnect.WCSignData.V2SignData<*>? {
            return WalletConnectV2.parseSessionRequest(sessionRequest)
        }

        suspend fun getWeb3Priv(
            context: Context,
            pin: String,
        ): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(context), pin, result.getOrThrow())
            return tipPrivToPrivateKey(spendKey)
        }

        suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

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
