package one.mixin.android.web3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.tip.wc.internal.WCEthereumTransaction

@Parcelize
class JsSignMessage(
    val callbackId: Long,
    val type: Int,
    val wcEthereumTransaction: WCEthereumTransaction? = null,
    val data: String? = null,
) : Parcelable {
    companion object {
        const val TYPE_TYPED_MESSAGE = 0
        const val TYPE_MESSAGE = 1
        const val TYPE_TRANSACTION = 2
    }
}