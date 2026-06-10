package one.mixin.android.ui.tip.wc.pay

import android.content.Context
import androidx.lifecycle.ViewModel
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.tip.Tip
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import javax.inject.Inject

@HiltViewModel
class WalletConnectPayViewModel
    @Inject
    internal constructor(
        private val tip: Tip,
    ) : ViewModel() {

        suspend fun getPaymentOptions(paymentLink: String): Wallet.Model.PaymentOptionsResponse =
            withContext(Dispatchers.IO) {
                WalletConnectV2.getPaymentOptions(paymentLink)
            }

        suspend fun getRequiredPaymentActions(paymentId: String, optionId: String): List<Wallet.Model.WalletRpcAction> =
            withContext(Dispatchers.IO) {
                WalletConnectV2.getRequiredPaymentActions(paymentId, optionId)
            }

        suspend fun getWeb3Priv(context: Context, pin: String): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(
                tip.getMnemonicFromEncryptedPreferences(context),
                tip.getEncryptedSalt(context),
                pin,
                result.getOrThrow(),
            )
            return requireNotNull(CryptoWalletHelper.getWeb3PrivateKey(context, spendKey, Chain.Ethereum.assetId))
        }

        suspend fun signAndConfirm(
            priv: ByteArray,
            paymentId: String,
            optionId: String,
            actions: List<Wallet.Model.WalletRpcAction>,
        ) = withContext(Dispatchers.IO) {
            val signatures = actions.map { action -> WalletConnectV2.signPaymentAction(priv, action) }
            WalletConnectV2.confirmPayment(paymentId, optionId, signatures)
        }
    }
