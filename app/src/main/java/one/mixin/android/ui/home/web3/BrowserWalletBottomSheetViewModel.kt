package one.mixin.android.ui.home.web3

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
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
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
        private val tipService: TipService,
        private val tip: Tip,
    ) : ViewModel() {

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

        suspend fun getTipPriv(
            context: Context,
            pin: String,
        ): ByteArray {
            val result = tip.getOrRecoverTipPriv(context, pin)
            val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(context), pin, result.getOrThrow())
            return tipPrivToPrivateKey(spendKey)
        }

        suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

    }
