package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import okio.Okio
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.cancelRunOnUIThread
import one.mixin.android.extension.runOnUIThread
import one.mixin.android.extension.toast
import java.nio.charset.Charset

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class RecaptchaView(private val context: Context, private val callback: Callback) {
    companion object {
        private const val WEB_VIEW_TIME_OUT = 10000L
    }

    val webView: WebView by lazy {
        WebView(context).apply {
            settings.apply {
                defaultTextEncodingName = "utf-8"
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(this@RecaptchaView, "MixinContext")
            visibility = GONE
        }
    }

    private var hasPostToken = false

    private val stopWebViewRunnable = Runnable {
        webView.stopLoading()
        webView.visibility = GONE
        webView.webViewClient = null
        context.toast(R.string.error_recaptcha_timeout)
        callback.onStop()
    }

    fun loadRecaptcha() {
        hasPostToken = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                context.runOnUIThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("javascript:gReCaptchaExecute()", null)
            }
        }
        val input = context.assets.open("recaptcha.html")
        var html = Okio.buffer(Okio.source(input)).readByteString().string(Charset.forName("utf-8"))
        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
        webView.loadDataWithBaseURL(Constants.API.DOMAIN, html, "text/html", "UTF-8", null)
    }

    fun isVisible() = webView.visibility == VISIBLE

    @JavascriptInterface
    fun postMessage(value: String) {
        if (!hasPostToken && value == "challenge_change") {
            context.cancelRunOnUIThread(stopWebViewRunnable)
            webView.post { webView.visibility = VISIBLE }
        }
    }

    @JavascriptInterface
    fun postToken(value: String) {
        hasPostToken = true
        context.cancelRunOnUIThread(stopWebViewRunnable)
        webView.post {
            webView.visibility = GONE
            callback.onPostToken(value)
        }
    }

    interface Callback {
        fun onStop()
        fun onPostToken(value: String)
    }
}