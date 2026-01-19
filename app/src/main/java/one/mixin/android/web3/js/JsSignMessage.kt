package one.mixin.android.web3.js

import android.os.Parcelable
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.util.GsonHelper
import org.web3j.utils.Numeric
import java.math.BigDecimal

@Parcelize
class JsSignMessage(
    val callbackId: Long,
    val type: Int,
    val wcEthereumTransaction: WCEthereumTransaction? = null,
    val data: String? = null,
    val solanaTxSource: SolanaTxSource = SolanaTxSource.InnerTransfer,
    val isSpeedUp: Boolean = false,
    val isCancelTx: Boolean = false,
    val fee: BigDecimal? = null, // only btc
    val rate: BigDecimal? = null, // only btc
    val virtualSize: Int? = null, // only btc, vbytes
) : Parcelable {
    companion object {
        const val TYPE_TYPED_MESSAGE = 0
        const val TYPE_PERSONAL_MESSAGE = 1
        const val TYPE_MESSAGE = 2
        const val TYPE_TRANSACTION = 3
        const val TYPE_RAW_TRANSACTION = 4
        const val TYPE_SIGN_IN = 5

        const val TYPE_BTC_TRANSACTION = 6

        fun isSignMessage(type: Int): Boolean =
            type == TYPE_MESSAGE || type == TYPE_TYPED_MESSAGE || type == TYPE_PERSONAL_MESSAGE || type == TYPE_SIGN_IN

    }

    // TYPE_MESSAGE Any chain could be
    fun isSolMessage() = type == TYPE_RAW_TRANSACTION || type == TYPE_SIGN_IN
    fun isEvmMessage() = type == TYPE_TYPED_MESSAGE || type == TYPE_PERSONAL_MESSAGE || type == TYPE_TRANSACTION

    val reviewData: String?
        get() {
            val data = this.data ?: return null
            try {
                if (type == TYPE_PERSONAL_MESSAGE || type == TYPE_MESSAGE) {
                    return String(Numeric.cleanHexPrefix(data).hexStringToByteArray())
                } else if (type == TYPE_TYPED_MESSAGE) {
                    val listType = object : TypeToken<List<String>>() {}.type
                    val params = GsonHelper.customGson.fromJson<List<String>>(data, listType)
                    if (params.size >= 2) {
                        val messageString = params[1]
                        return messageString
                    } else {
                        throw IllegalArgumentException("IllegalArgument")
                    }
                }
            } catch (e: Exception) {
                return data
            }
            return data
        }
}

enum class SolanaTxSource {
    InnerTransfer, InnerSwap, InnerStake, Web, Link, WalletConnect;

    fun isInnerTx() = this == InnerTransfer || this == InnerSwap || this == InnerStake

    fun isConnectDapp() = this == Web || this == WalletConnect
}
