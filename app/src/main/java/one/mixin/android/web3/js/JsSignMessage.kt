package one.mixin.android.web3.js

import android.os.Parcelable
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.util.GsonHelper
import org.web3j.utils.Numeric
import timber.log.Timber

@Parcelize
class JsSignMessage(
    val callbackId: Long,
    val type: Int,
    val wcEthereumTransaction: WCEthereumTransaction? = null,
    val data: String? = null,
) : Parcelable {
    companion object {
        const val TYPE_TYPED_MESSAGE = 0
        const val TYPE_PERSONAL_MESSAGE = 1
        const val TYPE_MESSAGE = 2
        const val TYPE_TRANSACTION = 3
    }

    val reviewData: String?
        get() {
            val data = this.data ?: return null
            try {
                if (type == TYPE_PERSONAL_MESSAGE) {
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