package one.mixin.android.ui.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

open class SignInWebViewClient(previous: SignInWebViewClient? = null) : WebViewClient() {
    private var signInPageUrl: String? = previous?.signInPageUrl
    private var loadingFailed: Boolean = previous?.loadingFailed ?: false

    fun getSignInPageUrl(view: WebView): String? {
        val page = signInPageUrl?.toHttpUrlOrNull() ?: return null
        val current = view.url?.toHttpUrlOrNull() ?: return null
        return signInPageUrl?.takeIf {
            page.scheme == current.scheme && page.host == current.host && page.port == current.port
        }
    }

    protected fun invalidateSignInPage() {
        signInPageUrl = null
        loadingFailed = true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        invalidateSignInPage()
        loadingFailed = false
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!loadingFailed) {
            signInPageUrl = url?.takeIf { it == view?.url }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        invalidateSignInPage()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
        invalidateSignInPage()
    }
}
