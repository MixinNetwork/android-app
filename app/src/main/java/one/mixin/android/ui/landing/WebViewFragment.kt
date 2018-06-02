package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_webview.view.*
import okio.Okio
import one.mixin.android.BuildConfig
import one.mixin.android.R
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
        val contentView = View.inflate(context, R.layout.fragment_webview, null)
        webView = contentView.webView
        pb = contentView.progressBar
        dialog.setContentView(contentView)
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