package one.mixin.android.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.http.SslError
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import okio.buffer
import okio.source
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.cancelRunOnUiThread
import one.mixin.android.extension.dp
import one.mixin.android.extension.runOnUiThread
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.toast
import one.mixin.android.extension.translationY
import one.mixin.android.util.reportException
import timber.log.Timber
import java.nio.charset.Charset

internal data class CaptchaDialogBarStyle(
    val heightDp: Int,
    @DrawableRes val closeIconResId: Int,
    val closeIconGravity: Int,
    val cornerRadiusDp: Int,
    val progressBelowBar: Boolean,
)

internal fun captchaDialogBarStyle() =
    CaptchaDialogBarStyle(
        heightDp = 48,
        closeIconResId = R.drawable.ic_circle_close,
        closeIconGravity = Gravity.END or Gravity.CENTER_VERTICAL,
        cornerRadiusDp = 12,
        progressBelowBar = true,
    )

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class CaptchaView(private val context: Context, private val callback: Callback) {
    companion object {
        private const val WEB_VIEW_TIME_OUT = 35000L

        private const val DIALOG_HORIZONTAL_MARGIN_DP = 20
        private const val CAPTCHA_CONTENT_MAX_HEIGHT_DP = 560
        private const val CAPTCHA_DIALOG_DIM_AMOUNT = 0.6f

        private const val TAG = "CaptchaView"

        const val reCAPTCHA = "reCAPTCHA"
        const val hCAPTCHA = "hCaptcha"
        const val gtCAPTCHA = "GeeTest"
    }

    private var captchaDialog: Dialog? = null
    private var released = false
    private val timedOutCaptchaTypes = mutableSetOf<CaptchaType>()

    private val captchaContentHeight by lazy {
        val barHeight = captchaDialogBarStyle().heightDp.dp
        minOf(CAPTCHA_CONTENT_MAX_HEIGHT_DP.dp, (context.screenHeight() * 0.85f).toInt() - barHeight).coerceAtLeast(320.dp)
    }

    private val captchaContainer: LinearLayout by lazy {
        val barStyle = captchaDialogBarStyle()
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = barStyle.cornerRadiusDp.dp.toFloat()
            }
            clipToOutline = true
            addView(
                captchaBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    barStyle.heightDp.dp,
                ),
            )
            addView(
                captchaContent,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    captchaContentHeight,
                ),
            )
        }
    }

    private val captchaBar: FrameLayout by lazy {
        val barStyle = captchaDialogBarStyle()
        FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            addView(
                closeButton,
                FrameLayout.LayoutParams(
                    48.dp,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    barStyle.closeIconGravity,
                ),
            )
        }
    }

    private val closeButton: ImageView by lazy {
        val barStyle = captchaDialogBarStyle()
        ImageView(context).apply {
            setImageResource(barStyle.closeIconResId)
            setBackgroundResource(R.drawable.mixin_ripple)
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
            contentDescription = context.getString(R.string.Cancel)
            setOnClickListener { cancelCaptcha() }
        }
    }

    private val captchaContent: FrameLayout by lazy {
        FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE)
            addView(
                webView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(
                progressBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    2.dp,
                    Gravity.TOP,
                ),
            )
        }
    }

    private val progressBar: ProgressBar by lazy {
        ProgressBar(ContextThemeWrapper(context, R.style.ProgressTheme), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = ProgressBar.GONE
        }
    }

    private val webViewLazy = lazy {
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

    val webView: WebView by webViewLazy

    private val stopWebViewRunnable = Runnable { handleCaptchaTimeout() }

    private var captchaType = CaptchaType.GCaptcha
    private var fallbackEnabled = true

    fun loadCaptcha(captchaType: CaptchaType) = loadCaptcha(captchaType, true, true)

    fun loadCaptchaWithoutFallback(captchaType: CaptchaType) = loadCaptcha(captchaType, true, false)

    private fun loadCaptcha(
        captchaType: CaptchaType,
        resetTimeoutFallbacks: Boolean,
        fallbackEnabled: Boolean,
    ) {
        if (released) return
        this.captchaType = captchaType
        this.fallbackEnabled = fallbackEnabled
        if (resetTimeoutFallbacks) {
            timedOutCaptchaTypes.clear()
        }
        show()
        val isG = captchaType.isG()
        val isH = captchaType.isH()
        val isGT = captchaType.isGT()
        if (isG || isH || isGT) {
            updateProgress(0)
            webView.webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(
                        view: WebView?,
                        newProgress: Int,
                    ) {
                        super.onProgressChanged(view, newProgress)
                        updateProgress(newProgress)
                    }
                }
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
                        updateProgress(100)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val message = "$TAG load $captchaType onReceivedHttpError ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}"
                        Timber.e(message)
                        reportException(CaptchaException(message))
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        super.onReceivedSslError(view, handler, error)
                        val message = "$TAG load $captchaType onReceivedSslError ${error?.toString()}"
                        Timber.e(message)
                        reportException(CaptchaException(message))
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        val message = "$TAG load $captchaType onReceivedError ${error?.errorCode} ${error?.description}"
                        Timber.e(message)
                        reportException(CaptchaException(message))
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        if (isGT && request?.url?.toString()?.endsWith("gt4.js") == true) {
                            try {
                                val inputStream = context.assets.open("gt4.js")
                                return WebResourceResponse("application/javascript", "UTF-8", inputStream)
                            } catch (e: Exception) {
                                Timber.e(e, "$TAG load $captchaType intercept local gt4.js failed")
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


            if (isGT) {
                val gt4Input = context.assets.open("gt4.js")
                val gt4Content = gt4Input.source().buffer().readByteString().string(Charset.forName("utf-8"))
                gt4Input.close()
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
            runOnUiThread(stopWebViewRunnable, WEB_VIEW_TIME_OUT)
        }
    }

    private fun handleCaptchaTimeout() {
        if (released) return
        val message = "$TAG load $captchaType timeout"
        Timber.e(message)
        reportException(CaptchaException(message))
        if (!fallbackEnabled) {
            updateProgress(100)
            callback.onStop()
            return
        }
        timedOutCaptchaTypes.add(captchaType)
        val fallbackCaptchaType = captchaType.fallback()
        if (fallbackCaptchaType !in timedOutCaptchaTypes) {
            loadCaptcha(fallbackCaptchaType, false, true)
        } else {
            hide()
            toast(R.string.Recaptcha_timeout)
            callback.onStop()
        }
    }

    private fun show() {
        val dialog = captchaDialog ?: createDialog().also {
            captchaDialog = it
        }
        webView.translationY = context.screenHeight().toFloat()
        if (!dialog.isShowing) {
            try {
                dialog.show()
            } catch (e: Exception) {
                Timber.e(e, "$TAG show dialog failed captchaType=$captchaType")
                reportException(e)
            }
        }
        updateDialogWindow(dialog)
    }

    private fun createDialog() =
        Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            (captchaContainer.parent as? ViewGroup)?.removeView(captchaContainer)
            setContentView(captchaContainer)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setGravity(Gravity.CENTER)
            setOnShowListener {
                updateDialogWindow(this)
            }
            setOnCancelListener {
                stopCaptcha()
                captchaDialog = null
                callback.onStop()
            }
            setOnDismissListener {
                if (captchaDialog === this) {
                    captchaDialog = null
                }
            }
    }

    private fun updateDialogWindow(dialog: Dialog) {
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(CAPTCHA_DIALOG_DIM_AMOUNT)
            setLayout(
                context.screenWidth() - DIALOG_HORIZONTAL_MARGIN_DP.dp * 2,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    fun isVisible() = captchaDialog?.isShowing == true

    private fun updateProgress(progress: Int) {
        progressBar.progress = progress
        progressBar.visibility = if (progress in 0..99) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    fun hide() {
        if (released) return
        stopCaptcha()
        val dialog = captchaDialog
        captchaDialog = null
        dialog?.dismiss()
    }

    private fun cancelCaptcha() {
        if (released) return
        hide()
        callback.onStop()
    }

    fun release() {
        if (released) return
        released = true
        cancelRunOnUiThread(stopWebViewRunnable)
        val dialog = captchaDialog
        captchaDialog = null
        dialog?.dismiss()
        if (!webViewLazy.isInitialized()) return
        webView.webChromeClient = object : WebChromeClient() {}
        webView.loadUrl("about:blank")
        webView.webViewClient = object : WebViewClient() {}
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun stopCaptcha() {
        cancelRunOnUiThread(stopWebViewRunnable)
        updateProgress(100)
        if (!webViewLazy.isInitialized()) return
        webView.translationY(context.screenHeight().toFloat())
        webView.webChromeClient = object : WebChromeClient() {}
        webView.loadUrl("about:blank")
        webView.webViewClient = object : WebViewClient() {}
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postMessage(
        @Suppress("UNUSED_PARAMETER") value: String,
    ) {
        if (released) return
        if (value.isBlank()) return
        Timber.e("$TAG postMessage captchaType=$captchaType value=$value")
        cancelRunOnUiThread(stopWebViewRunnable)
        runOnUiThread(stopWebViewRunnable)
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postToken(value: String) {
        if (released) return
        cancelRunOnUiThread(stopWebViewRunnable)
        webView.post {
            if (released) return@post
            hide()
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

        fun fallback() =
            when (this) {
                GCaptcha -> HCaptcha
                HCaptcha -> GTCaptcha
                GTCaptcha -> GCaptcha
            }
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
