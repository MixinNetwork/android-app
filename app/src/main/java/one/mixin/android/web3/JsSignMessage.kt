import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.tip.wc.internal.WCEthereumTransaction

@Parcelize
class JsSignMessage(
    val callbackId: Int,
    val type: Int,
    val wcEthereumTransaction: WCEthereumTransaction? = null,
    val data: String? = null,
) : Parcelable {
    companion object {
        const val TYPE_MSSAGE = 0
        const val TYPE_PERSONAL_MESSAGE = 1
        const val TYPE_TRANSACTION = 2
    }
}