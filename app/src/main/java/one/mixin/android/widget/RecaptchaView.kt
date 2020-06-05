package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import okio.buffer
import okio.source
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.cancelRunOnUIThread
import one.mixin.android.extension.runOnUIThread
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import java.nio.charset.Charset

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class RecaptchaView(private val context: Context, private val callback: Callback) {
    companion object {
        private const val WEB_VIEW_TIME_OUT = 30000L
    }

    val webView: WebView by lazy {
        WebView(context).apply {
            settings.apply {
                defaultTextEncodingName = "utf-8"
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(this@RecaptchaView, "MixinContext")
            translationY = context.screenHeight().toFloat()
        }
    }

    private val stopWebViewRunnable = Runnable {
        webView.loadUrl("about:blank")
        hide()
        webView.webViewClient = null
        context.toast(R.string.error_recaptcha_timeout)
        callback.onStop()
    }

    fun loadRecaptcha() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                context.runOnUIThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                context.cancelRunOnUIThread(stopWebViewRunnable)
                webView.animate().translationY(0f)
                webView.evaluateJavascript("javascript:grecaptcha.execute()", null)
            }
        }
        val input = context.assets.open("recaptcha.html")
        var html = input.source().buffer().readByteString().string(Charset.forName("utf-8"))
        html = html.replace("#apiKey", BuildConfig.RECAPTCHA_KEY)
        webView.clearCache(true)
        webView.loadDataWithBaseURL(Constants.API.DOMAIN, html, "text/html", "UTF-8", null)
    }

    fun isVisible() = webView.translationY == 0f

    fun hide() {
        webView.animate().translationY(context.screenHeight().toFloat())
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postMessage(@Suppress("UNUSED_PARAMETER") value: String) {
        context.cancelRunOnUIThread(stopWebViewRunnable)
        context.runOnUIThread(stopWebViewRunnable)
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postToken(value: String) {
        context.cancelRunOnUIThread(stopWebViewRunnable)
        webView.post {
            hide()
            webView.loadUrl("about:blank")
            webView.webViewClient = null
            callback.onPostToken(value)
        }
    }

    interface Callback {
        fun onStop()
        fun onPostToken(value: String)
    }
}
