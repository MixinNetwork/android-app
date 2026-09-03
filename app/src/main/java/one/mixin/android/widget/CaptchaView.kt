package one.mixin.android.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.SystemClock
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
        private const val MAX_CAPTCHA_CYCLES = 2
        private const val CAPTCHA_TYPES_PER_CYCLE = 3
        private const val MAX_CAPTCHA_FAILURES = MAX_CAPTCHA_CYCLES * CAPTCHA_TYPES_PER_CYCLE

        private const val DIALOG_HORIZONTAL_MARGIN_DP = 20
        private const val CAPTCHA_CONTENT_MAX_HEIGHT_DP = 560
        private const val CAPTCHA_DIALOG_DIM_AMOUNT = 0.6f

        private const val TAG = "CaptchaView"

        private const val EVENT_PROGRESS = "progress"
        private const val EVENT_READY = "ready"
        private const val EVENT_ERROR = "error"
        private const val EVENT_CANCEL = "cancel"
        private const val STAGE_PAGE_LOADING = "page_loading"
        private const val STAGE_PAGE_FINISHED = "page_finished"
        private const val STAGE_SDK_LOADED = "sdk_loaded"
        private const val STAGE_WIDGET_RENDERED = "widget_rendered"
        private const val STAGE_CHALLENGE_READY = "challenge_ready"

        const val reCAPTCHA = "reCAPTCHA"
        const val hCAPTCHA = "hCaptcha"
        const val gtCAPTCHA = "GeeTest"
    }

    private var captchaDialog: Dialog? = null
    private var released = false
    private val captchaFailureHistory = mutableListOf<String>()
    private var nextCaptchaLoadId = 0L
    private var activeCaptchaLoad: CaptchaLoadState? = null

    private data class CaptchaLoadState(
        val id: String,
        val type: CaptchaType,
        val fallbackEnabled: Boolean,
        val startedAt: Long,
        val documentUrl: String,
        val scriptOnloadCallback: String,
        var stage: String,
        var settled: Boolean = false,
        var timeoutRunnable: Runnable? = null,
        var lastWebError: String? = null,
    )

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

    fun loadCaptcha(captchaType: CaptchaType) = loadCaptcha(captchaType, true, true)

    fun loadCaptchaWithoutFallback(captchaType: CaptchaType) = loadCaptcha(captchaType, true, false)

    private fun loadCaptcha(
        captchaType: CaptchaType,
        resetTimeoutFallbacks: Boolean,
        fallbackEnabled: Boolean,
    ) {
        if (released) return
        if (resetTimeoutFallbacks) {
            captchaFailureHistory.clear()
        }
        val load = startCaptchaLoad(captchaType, fallbackEnabled)
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
                        if (!isActiveCaptchaLoad(load.id)) return
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
                        if (!isActiveCaptchaLoad(load.id) || url != load.documentUrl) return
                        handleCaptchaLoadEvent(load.id, CaptchaLoadEvent.PageFinished, STAGE_PAGE_FINISHED)
                        if (isGT) view?.evaluateJavascript("initGTCaptcha()") {}
                        view?.translationY(0f)
                        updateProgress(100)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val state = activeCaptchaLoad(load.id) ?: return
                        if (isStaleCaptchaRequest(state, request)) return
                        val detail =
                            "status=${errorResponse?.statusCode}" +
                                " mainFrame=${request?.isForMainFrame}" +
                                " resource=${captchaResource(request)}"
                        if (isCriticalCaptchaRequest(state, request)) {
                            failCaptchaLoad(load.id, "http_error", detail)
                        } else {
                            logCaptchaWebError(state, "http_error", detail)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.cancel()
                        val state = activeCaptchaLoad(load.id) ?: return
                        if (isStaleCaptchaApiUrl(state, error?.url)) return
                        val detail = "code=${error?.primaryError} resource=${captchaResource(error?.url)}"
                        if (isCriticalCaptchaUrl(state, error?.url)) {
                            failCaptchaLoad(load.id, "ssl_error", detail)
                        } else {
                            logCaptchaWebError(state, "ssl_error", detail)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        val state = activeCaptchaLoad(load.id) ?: return
                        if (isStaleCaptchaRequest(state, request)) return
                        val detail =
                            "code=${error?.errorCode}" +
                                " mainFrame=${request?.isForMainFrame}" +
                                " resource=${captchaResource(request)}" +
                                " description=${error?.description}"
                        if (isCriticalCaptchaRequest(state, request)) {
                            failCaptchaLoad(load.id, "resource_error", detail)
                        } else {
                            logCaptchaWebError(state, "resource_error", detail)
                        }
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
                                Timber.e(e, "$TAG load ${load.type} intercept local gt4.js failed")
                                webView.post {
                                    failCaptchaLoad(load.id, "script_load", "local_gt4")
                                }
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            var html =
                context.assets.open("captcha.html").use { input ->
                    input.source().buffer().readByteString().string(Charset.forName("utf-8"))
                }
            val apiKey = when {
                isG -> BuildConfig.RECAPTCHA_KEY
                isH -> BuildConfig.HCAPTCHA_KEY
                isGT -> BuildConfig.GEETEST_KEY
                else -> ""
            }

            html = html.replace("#apiKey", apiKey)
            html = html.replace("#loadId", load.id)
            when {
                isG -> {
                    html = html.replace("onGCaptchaLoad", load.scriptOnloadCallback)
                    html = html.replace(
                        "#src",
                        "https://www.recaptcha.net/recaptcha/api.js?onload=${load.scriptOnloadCallback}&render=explicit",
                    )
                }

                isH -> {
                    html = html.replace("onHCaptchaLoad", load.scriptOnloadCallback)
                    html = html.replace(
                        "#src",
                        "https://js.hcaptcha.com/1/api.js?onload=${load.scriptOnloadCallback}&render=explicit",
                    )
                }

                else -> html = html.replace("#src", "")
            }

            if (isGT) {
                val gt4Content =
                    context.assets.open("gt4.js").use { input ->
                        input.source().buffer().readByteString().string(Charset.forName("utf-8"))
                    }
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
            webView.loadDataWithBaseURL(load.documentUrl, html, "text/html", "UTF-8", null)
        }
    }

    private fun startCaptchaLoad(
        captchaType: CaptchaType,
        fallbackEnabled: Boolean,
    ): CaptchaLoadState {
        invalidateActiveCaptchaLoad()
        if (webViewLazy.isInitialized()) {
            webView.stopLoading()
        }
        val loadId = (++nextCaptchaLoadId).toString()
        val state =
            CaptchaLoadState(
                id = loadId,
                type = captchaType,
                fallbackEnabled = fallbackEnabled,
                startedAt = SystemClock.elapsedRealtime(),
                documentUrl = "${Constants.API.DOMAIN}/?captcha_load_id=$loadId",
                scriptOnloadCallback = "onCaptchaLoad$loadId",
                stage = STAGE_PAGE_LOADING,
            )
        activeCaptchaLoad = state
        restartCaptchaTimeout(state)
        return state
    }

    private fun handleCaptchaLoadEvent(
        loadId: String,
        event: CaptchaLoadEvent,
        stage: String,
    ) {
        val state = activeCaptchaLoad(loadId) ?: return
        if (event != CaptchaLoadEvent.PageFinished || state.stage == STAGE_PAGE_LOADING) {
            state.stage = stage
        }
        when (
            decideCaptchaLoadAction(
                event = event,
                captchaType = state.type,
                failureCount = captchaFailureHistory.size,
                maxFailureCount = MAX_CAPTCHA_FAILURES,
                fallbackEnabled = state.fallbackEnabled,
            )
        ) {
            CaptchaLoadAction.KeepWatching -> Unit
            CaptchaLoadAction.RestartWatchdog -> restartCaptchaTimeout(state)
            is CaptchaLoadAction.SwitchTo,
            CaptchaLoadAction.Stop,
            -> Unit
        }
    }

    private fun restartCaptchaTimeout(state: CaptchaLoadState) {
        if (state.settled || activeCaptchaLoad?.id != state.id) return
        cancelCaptchaTimeout(state)
        val timeoutRunnable = Runnable { failCaptchaLoad(state.id, "timeout", "") }
        state.timeoutRunnable = timeoutRunnable
        runOnUiThread(timeoutRunnable, WEB_VIEW_TIME_OUT)
    }

    private fun cancelCaptchaTimeout(state: CaptchaLoadState) {
        state.timeoutRunnable?.let(::cancelRunOnUiThread)
        state.timeoutRunnable = null
    }

    private fun cancelCaptchaLoad(loadId: String) {
        val state = activeCaptchaLoad(loadId) ?: return
        state.settled = true
        cancelCaptchaTimeout(state)
        hide()
        callback.onStop()
    }

    private fun failCaptchaLoad(
        loadId: String,
        reason: String,
        detail: String,
    ) {
        if (released) return
        val state = activeCaptchaLoad(loadId) ?: return
        state.settled = true
        cancelCaptchaTimeout(state)
        val safeDetail =
            detail
                .ifBlank { state.lastWebError.orEmpty() }
                .replace('\n', ' ')
                .replace('\r', ' ')
                .take(200)
        val elapsed = SystemClock.elapsedRealtime() - state.startedAt
        val message =
            "$TAG load ${state.type} failed stage=${state.stage} reason=$reason" +
                " elapsed=${elapsed}ms${if (safeDetail.isBlank()) "" else " detail=$safeDetail"}"
        Timber.e(message)
        captchaFailureHistory.add(
            "${state.type}:$reason@${state.stage}" +
                if (safeDetail.isBlank()) "" else "($safeDetail)",
        )
        val action =
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.FatalError,
                captchaType = state.type,
                failureCount = captchaFailureHistory.size,
                maxFailureCount = MAX_CAPTCHA_FAILURES,
                fallbackEnabled = state.fallbackEnabled,
            )
        if (!state.fallbackEnabled) {
            reportException(CaptchaException(message))
            updateProgress(100)
            callback.onStop()
            return
        }
        when (action) {
            is CaptchaLoadAction.SwitchTo -> loadCaptcha(action.captchaType, false, true)
            CaptchaLoadAction.Stop -> {
                val exhaustedMessage =
                    "$TAG exhausted captcha fallbacks cycles=$MAX_CAPTCHA_CYCLES" +
                        " attempts=${captchaFailureHistory.size} lastStage=${state.stage}" +
                        " failures=${captchaFailureHistory.joinToString()}"
                Timber.e(exhaustedMessage)
                reportException(CaptchaException(exhaustedMessage))
                hide()
                toast(R.string.Recaptcha_timeout)
                callback.onStop()
            }

            CaptchaLoadAction.KeepWatching,
            CaptchaLoadAction.RestartWatchdog,
            -> Unit
        }
    }

    private fun activeCaptchaLoad(loadId: String): CaptchaLoadState? =
        activeCaptchaLoad?.takeIf { it.id == loadId && !it.settled }

    private fun isActiveCaptchaLoad(loadId: String) = activeCaptchaLoad(loadId) != null

    private fun invalidateActiveCaptchaLoad() {
        activeCaptchaLoad?.let { state ->
            state.settled = true
            cancelCaptchaTimeout(state)
        }
        activeCaptchaLoad = null
    }

    private fun isStaleCaptchaRequest(
        state: CaptchaLoadState,
        request: WebResourceRequest?,
    ): Boolean {
        val resourceRequest = request ?: return false
        if (resourceRequest.isForMainFrame) return resourceRequest.url.toString() != state.documentUrl
        if (isCaptchaApiUri(state.type, resourceRequest.url)) {
            return resourceRequest.url.getQueryParameter("onload") != state.scriptOnloadCallback
        }
        val referer =
            resourceRequest.requestHeaders.entries
                .firstOrNull { it.key.equals("Referer", ignoreCase = true) }
                ?.value ?: return false
        return referer.startsWith("${Constants.API.DOMAIN}/?captcha_load_id=") && referer != state.documentUrl
    }

    private fun isCriticalCaptchaRequest(
        state: CaptchaLoadState,
        request: WebResourceRequest?,
    ): Boolean {
        val resourceRequest = request ?: return false
        if (resourceRequest.isForMainFrame) return resourceRequest.url.toString() == state.documentUrl
        return isCriticalCaptchaUri(state, resourceRequest.url)
    }

    private fun isCriticalCaptchaUrl(
        state: CaptchaLoadState,
        url: String?,
    ): Boolean {
        if (url == state.documentUrl) return true
        return isCriticalCaptchaUri(state, url?.let(Uri::parse))
    }

    private fun isCriticalCaptchaUri(
        state: CaptchaLoadState,
        uri: Uri?,
    ) = isCaptchaApiUri(state.type, uri) && uri?.getQueryParameter("onload") == state.scriptOnloadCallback

    private fun isStaleCaptchaApiUrl(
        state: CaptchaLoadState,
        url: String?,
    ): Boolean {
        val uri = url?.let(Uri::parse) ?: return false
        return isCaptchaApiUri(state.type, uri) && !isCriticalCaptchaUri(state, uri)
    }

    private fun isCaptchaApiUri(
        captchaType: CaptchaType,
        uri: Uri?,
    ): Boolean {
        if (uri == null) return false
        return when (captchaType) {
            CaptchaType.GCaptcha -> uri.host == "www.recaptcha.net" && uri.path == "/recaptcha/api.js"
            CaptchaType.HCaptcha -> uri.host == "js.hcaptcha.com" && uri.path == "/1/api.js"
            CaptchaType.GTCaptcha -> false
        }
    }

    private fun captchaResource(request: WebResourceRequest?): String {
        val uri = request?.url ?: return "unknown"
        return "${uri.host.orEmpty()}${uri.path.orEmpty()}"
    }

    private fun captchaResource(url: String?): String {
        val uri = url?.let(Uri::parse) ?: return "unknown"
        return "${uri.host.orEmpty()}${uri.path.orEmpty()}"
    }

    private fun logCaptchaWebError(
        state: CaptchaLoadState,
        reason: String,
        detail: String,
    ) {
        val message = "$TAG load ${state.type} stage=${state.stage} reason=$reason $detail"
        state.lastWebError = "$reason $detail".take(200)
        Timber.e(message)
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
                Timber.e(e, "$TAG show dialog failed captchaType=${activeCaptchaLoad?.type}")
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
        invalidateActiveCaptchaLoad()
        val dialog = captchaDialog
        captchaDialog = null
        dialog?.dismiss()
        if (!webViewLazy.isInitialized()) return
        webView.webChromeClient = object : WebChromeClient() {}
        webView.webViewClient = object : WebViewClient() {}
        webView.loadUrl("about:blank")
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun stopCaptcha() {
        invalidateActiveCaptchaLoad()
        updateProgress(100)
        if (!webViewLazy.isInitialized()) return
        webView.translationY(context.screenHeight().toFloat())
        webView.webChromeClient = object : WebChromeClient() {}
        webView.webViewClient = object : WebViewClient() {}
        webView.loadUrl("about:blank")
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postEvent(
        loadId: String,
        event: String,
        stage: String,
        detail: String,
    ) {
        if (released) return
        webView.post {
            if (released) return@post
            when (event) {
                EVENT_PROGRESS -> {
                    val loadEvent =
                        when (stage) {
                            STAGE_SDK_LOADED -> CaptchaLoadEvent.SdkLoaded
                            STAGE_WIDGET_RENDERED -> CaptchaLoadEvent.WidgetRendered
                            else -> null
                        }
                    if (loadEvent != null) {
                        handleCaptchaLoadEvent(loadId, loadEvent, stage)
                    }
                }

                EVENT_READY -> handleCaptchaLoadEvent(loadId, CaptchaLoadEvent.ChallengeReady, STAGE_CHALLENGE_READY)
                EVENT_ERROR -> failCaptchaLoad(loadId, stage.ifBlank { "provider_error" }, detail)
                EVENT_CANCEL -> cancelCaptchaLoad(loadId)
            }
        }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun postToken(
        loadId: String,
        value: String,
    ) {
        if (released) return
        webView.post {
            if (released) return@post
            val state = activeCaptchaLoad(loadId) ?: return@post
            state.settled = true
            cancelCaptchaTimeout(state)
            val captchaType = state.type
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
