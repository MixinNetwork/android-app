package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import timber.log.Timber
import java.nio.charset.Charset

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class CaptchaView(private val context: Context, private val callback: Callback) {
    companion object {
        private const val WEB_VIEW_TIME_OUT = 35000L

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
            setBackgroundColor(Color.WHITE)
            translationY = context.screenHeight().toFloat()
        }
    }

    private val stopWebViewRunnable =
        Runnable {
            if (captchaType.isG()) {
                Timber.e("load GCaptcha timeout")
                reportException(CaptchaException("$TAG load $captchaType timeout"))
                loadCaptcha(CaptchaType.HCaptcha)
            } else if (captchaType.isGT()) {
                Timber.e("load GTCaptcha timeout")
                reportException(CaptchaException("$TAG load $captchaType timeout"))
                loadCaptcha(CaptchaType.GCaptcha)
            } else if (captchaType.isH()) {
                Timber.e("load HCaptcha timeout")
                reportException(CaptchaException("$TAG load $captchaType timeout"))
                loadCaptcha(CaptchaType.GTCaptcha)
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
        val isH = captchaType.isH()
        val isGT = captchaType.isGT()
        Timber.e("load $captchaType")
        if (isG || isH || isGT) {
            webView.webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        super.onPageFinished(view, url)
                        if (isGT) view?.evaluateJavascript("initGTCaptcha()") {}
                        cancelRunOnUiThread(stopWebViewRunnable)
                        view?.translationY(0f)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        reportException(CaptchaException("$TAG load $captchaType onReceivedHttpError ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}"))
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        super.onReceivedSslError(view, handler, error)
                        reportException(CaptchaException("$TAG load $captchaType onReceivedSslError ${error?.toString()}"))
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        reportException(CaptchaException("$TAG load $captchaType onReceivedError ${error?.errorCode} ${error?.description}"))
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (isGT && request?.url.toString().endsWith("gt4.js")) {
                            try {
                                val inputStream = context.assets.open("gt4.js")
                                return WebResourceResponse("application/javascript", "UTF-8", inputStream)
                            } catch (e: Exception) {
                                reportException(e)
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            val input = context.assets.open("captcha.html")
            var html = input.source().buffer().readByteString().string(Charset.forName("utf-8"))
            val apiKey = when {
                isG -> BuildConfig.RECAPTCHA_KEY
                isH -> BuildConfig.HCAPTCHA_KEY
                isGT -> BuildConfig.GEETEST_KEY
                else -> ""
            }

            html = html.replace("#apiKey", apiKey)
            when {
                isG -> html = html.replace("#src", "https://www.recaptcha.net/recaptcha/api.js?onload=onGCaptchaLoad&render=explicit")
                isH -> html = html.replace("#src", "https://js.hcaptcha.com/1/api.js?onload=onHCaptchaLoad&render=explicit")
                else -> html = html.replace("#src", "")
            }

            val gt4Input = context.assets.open("gt4.js")
            val gt4Content = gt4Input.source().buffer().readByteString().string(Charset.forName("utf-8"))
            gt4Input.close()
            if (isGT) {
                html = html.replace(
                    "#gt", """
                    <script type="text/javascript">
                    ${gt4Content}
                    </script>
                """
                )
            } else {
                html = html.replace("#gt", "")
            }

            webView.clearCache(true)
            webView.loadDataWithBaseURL(Constants.API.DOMAIN, html, "text/html", "UTF-8", null)
//            runOnUiThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
        }
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
        Timber.e("postMessage: $value")
//        cancelRunOnUiThread(stopWebViewRunnable)
//        runOnUiThread(stopWebViewRunnable)
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
        GTCaptcha;


        fun isG() = this == GCaptcha
        fun isH() = this == HCaptcha
        fun isGT() = this == GTCaptcha
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
