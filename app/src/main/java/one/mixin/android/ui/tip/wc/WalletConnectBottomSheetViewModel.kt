package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.web3.Web3RawTransactionRequest
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import org.sol4k.VersionedTransaction
import org.web3j.crypto.Hash
import java.util.Base64
import javax.inject.Inject

@HiltViewModel
class WalletConnectBottomSheetViewModel
    @Inject
    internal constructor(
        private val assetRepo: TokenRepository,
        private val web3Repository: Web3Repository,
        private val tip: Tip,
    ) : ViewModel() {

        suspend fun estimateFee(request: Web3RawTransactionRequest) = web3Repository.estimateFee(request)

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
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getMnemonicFromEncryptedPreferences(context), tip.getEncryptedSalt(context), pin, result.getOrThrow())
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
                assetRepo.postRawTx(Web3RawTransactionRequest(chain.getWeb3ChainId(), rawTx))
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
