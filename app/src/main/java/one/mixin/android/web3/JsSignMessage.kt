package one.mixin.android.web3

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.OldDepositEntry

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

    val reviewData: String?
        get() {
            val data = this.data ?: return null
            if (type == TYPE_MESSAGE || type == TYPE_TYPED_MESSAGE) {
                try {
                    val listType = object : TypeToken<List<String>>() {}.type
                    val params = GsonHelper.customGson.fromJson<List<String>>(data, listType)
                    if (params.size >= 2) {
                        val encodedMessage = params[0]
                        String(encodedMessage.hexStringToByteArray())
                    } else {
                        throw IllegalArgumentException("IllegalArgument")
                    }
                } catch (e: Exception) {
                    return data
                }
            }
            return data
        }
}