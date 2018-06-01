package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import okio.Okio
import one.mixin.android.BuildConfig
import org.jetbrains.anko.dip
import java.nio.charset.Charset

class WebViewFragment : DialogFragment() {

    companion object {
        const val TAG = "WebViewFragment"
    }

    var callback: Callback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val v = WebView(context)
        dialog.setContentView(v)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window.attributes)
        lp.width = requireContext().dip(220)
        lp.height = requireContext().dip(180)
        dialog.window.attributes = lp
        return dialog
    }

    @SuppressLint("JavascriptInterface")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as WebView
        view.settings.apply {
            defaultTextEncodingName = "utf-8"
        }
        view.addJavascriptInterface(this, "mixin")
        val input = requireContext().assets.open("recaptcha.html")
        var html = Okio.buffer(Okio.source(input)).readByteString().string(Charset.forName("utf-8"))
        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
        view.loadData(html, "text/html", "UTF-8")
    }

    @JavascriptInterface
    fun postMessage(value: String) {
        callback?.onMessage(value)
        dismiss()
    }

    interface Callback {
        fun onMessage(value: String)
    }
}