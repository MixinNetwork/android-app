package one.mixin.android.ui.common

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class WebAppInterface(val context: Context, val conversationId: String?) {
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun getContext(): String? {
        return if (conversationId != null) {
            Gson().toJson(MixinContext(conversationId))
        } else {
            null
        }
    }
}

class MixinContext(
    @SerializedName("conversation_id")
    val conversationId: String?
)