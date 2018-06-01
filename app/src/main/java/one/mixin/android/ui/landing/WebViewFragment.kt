package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import okio.Okio
import one.mixin.android.BuildConfig
import org.jetbrains.anko.dip
import java.nio.charset.Charset

class WebViewFragment : DialogFragment() {

    companion object {
        const val TAG = "WebViewFragment"
    }

    var callback: Callback? = null

    lateinit var webView: WebView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        webView = WebView(context)
        dialog.setContentView(webView)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window.attributes)
        lp.width = requireContext().dip(420)
        lp.height = requireContext().dip(360)
        dialog.window.attributes = lp
        return dialog
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        webView.settings.apply {
            defaultTextEncodingName = "utf-8"
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(WebViewFragment.WebAppInterface(context!!), "MixinContext")
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.e("Hello", url)
                view!!.loadUrl("javascript:execute();")

            }
        }
        val input = requireContext().assets.open("recaptcha.html")
        var html = Okio.buffer(Okio.source(input)).readByteString().string(Charset.forName("utf-8"))
        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
        webView.loadData(html, "text/html", "UTF-8")
    }



    class WebAppInterface(val context: Context) {
        @JavascriptInterface
        fun postMessage(value: String) {
            Log.e("Hello", value)
            Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
//            callback?.onMessage(value)
//            dismiss()
        }

    }

    interface Callback {
        fun onMessage(value: String)
    }
}