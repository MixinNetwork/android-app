package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import okio.buffer
import okio.source
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.cancelRunOnUiThread
import one.mixin.android.extension.runOnUiThread
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.translationY
import one.mixin.android.util.reportException
import java.nio.charset.Charset

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class CaptchaView(private val context: Context, private val callback: Callback) {
    companion object {
        private const val WEB_VIEW_TIME_OUT = 15000L

        private const val TAG = "CaptchaView"
    }

    val webView: WebView by lazy {
        WebView(context).apply {
            settings.apply {
                defaultTextEncodingName = "utf-8"
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(this@CaptchaView, "MixinContext")
            translationY = context.screenHeight().toFloat()
        }
    }

    private val stopWebViewRunnable =
        Runnable {
            if (captchaType.isG()) {
                reportException(CaptchaException("$TAG load $captchaType timeout"))
                loadCaptcha(CaptchaType.HCaptcha)
            } else {
                webView.loadUrl("about:blank")
                hide()
                webView.webViewClient = object : WebViewClient() {}
                toast(R.string.Recaptcha_timeout)
                callback.onStop()
                reportException(CaptchaException("$TAG load $captchaType timeout"))
            }
        }

    private var captchaType = CaptchaType.GCaptcha

    fun loadCaptcha(captchaType: CaptchaType) {
        this.captchaType = captchaType
        val isG = captchaType.isG()
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView?,
                    url: String?,
                ) {
                    super.onPageFinished(view, url)
                    cancelRunOnUiThread(stopWebViewRunnable)
                    view?.translationY(0f)
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    reportException(CaptchaException("$TAG load $captchaType onReceivedHttpError ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}"))
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    super.onReceivedSslError(view, handler, error)
                    reportException(CaptchaException("$TAG load $captchaType onReceivedSslError ${error?.toString()}"))
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    reportException(CaptchaException("$TAG load $captchaType onReceivedError ${error?.errorCode} ${error?.description}"))
                }
            }
        val input = context.assets.open("captcha.html")
        var html = input.source().buffer().readByteString().string(Charset.forName("utf-8"))
        val apiKey = if (isG) BuildConfig.RECAPTCHA_KEY else BuildConfig.HCAPTCHA_KEY
        val src =
            if (isG) {
                "https://www.recaptcha.net/recaptcha/api.js?onload=onGCaptchaLoad&render=explicit"
            } else {
                "https://hcaptcha.com/1/api.js?onload=onHCaptchaLoad&render=explicit"
            }
        html = html.replace("#src", src)
        html = html.replace("#apiKey", apiKey)
        webView.clearCache(true)
        webView.loadDataWithBaseURL(Constants.API.DOMAIN, html, "text/html", "UTF-8", null)
        runOnUiThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
    }

    fun isVisible() = webView.translationY == 0f

    fun hide() {
        webView.translationY(context.screenHeight().toFloat())
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postMessage(
        @Suppress("UNUSED_PARAMETER") value: String,
    ) {
        cancelRunOnUiThread(stopWebViewRunnable)
        runOnUiThread(stopWebViewRunnable)
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postToken(value: String) {
        cancelRunOnUiThread(stopWebViewRunnable)
        webView.post {
            hide()
            webView.loadUrl("about:blank")
            webView.webViewClient = object : WebViewClient() {}
            callback.onPostToken(Pair(captchaType, value))
        }
    }

    enum class CaptchaType {
        GCaptcha,
        HCaptcha,
        ;

        fun isG() = this == GCaptcha
    }

    interface Callback {
        fun onStop()

        fun onPostToken(value: Pair<CaptchaType, String>)
    }
}

class CaptchaException(message: String) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
