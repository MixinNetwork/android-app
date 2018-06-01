package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import okio.Okio
import one.mixin.android.BuildConfig
import one.mixin.android.extension.fadeOut
import java.nio.charset.Charset

class WebViewFragment : DialogFragment() {

    companion object {
        const val TAG = "WebViewFragment"
    }

    var callback: Callback? = null

    private lateinit var webView: WebView
    private lateinit var pb: ProgressBar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val fl = FrameLayout(requireContext())
        pb = ProgressBar(requireContext())
        webView = WebView(requireContext())
        fl.addView(webView, MATCH_PARENT, MATCH_PARENT)
        val pbParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        pbParams.gravity = Gravity.CENTER
        fl.addView(pb, pbParams)
        dialog.setContentView(fl)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
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
        webView.addJavascriptInterface(this, "MixinContext")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pb.fadeOut()
            }
        }
        val input = requireContext().assets.open("recaptcha.html")
        var html = Okio.buffer(Okio.source(input)).readByteString().string(Charset.forName("utf-8"))
        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
        webView.loadDataWithBaseURL("https://mixin.one", html, "text/html", "UTF-8", null)
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