package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.fragment.app.FragmentManager
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWebBottomSheetBinding
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

class WebBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WebBottomSheetDialogFragment"

        private const val ARGS_URL = "args_url"
        private const val ARGS_TITLE = "args_title"
        private const val ARGS_SUBTITLE = "args_subtitle"

        fun newInstance(
            url: String,
            title: String,
            subtitle: String? = null,
        ) = WebBottomSheetDialogFragment().withArgs {
            putString(ARGS_URL, url)
            putString(ARGS_TITLE, title)
            putString(ARGS_SUBTITLE, subtitle)
        }

        fun show(
            manager: FragmentManager,
            url: String,
            title: String,
            subtitle: String? = null,
        ): Boolean {
            if (manager.findFragmentByTag(TAG) != null) {
                return true
            }
            if (manager.isStateSaved) {
                return false
            }
            newInstance(url, title, subtitle).show(manager, TAG)
            return true
        }
    }

    private val binding by viewBinding(FragmentWebBottomSheetBinding::inflate)

    private val url by lazy { requireArguments().getString(ARGS_URL).orEmpty() }
    private val title by lazy { requireArguments().getString(ARGS_TITLE).orEmpty() }
    private val subtitle by lazy { requireArguments().getString(ARGS_SUBTITLE) }
    private var bottomSheet: BottomSheet? = null
    private var lastVisibleHeight = 0
    private val keyboardLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            val visibleHeight = getDialogVisibleHeight()
            if (visibleHeight <= 0) return@OnGlobalLayoutListener
            if (visibleHeight == lastVisibleHeight) return@OnGlobalLayoutListener
            lastVisibleHeight = visibleHeight
            bottomSheet?.setCustomViewHeightSync(visibleHeight)
        }

    @SuppressLint("RestrictedApi", "SetJavaScriptEnabled")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.title.rightIv.setOnClickListener { dismiss() }
        binding.title.setSubTitle(title, subtitle)
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.textZoom = 100
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.userAgentString = settings.userAgentString + " Mixin/" + BuildConfig.VERSION_NAME
            webChromeClient = object : WebChromeClient() {}
            webViewClient = BottomSheetWebViewClient()
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }
        (dialog as BottomSheet).apply {
            bottomSheet = this
            onBackPressedDispatcher.addCallback(this@WebBottomSheetDialogFragment) {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    dismiss()
                }
            }
            setCustomView(contentView)
            lastVisibleHeight = getDialogVisibleHeight().takeIf { it > 0 }
                ?: window?.decorView?.height?.takeIf { it > 0 }
                ?: requireActivity().window.decorView.height.takeIf { it > 0 }
                ?: resources.displayMetrics.heightPixels
            setCustomViewHeight(lastVisibleHeight)
        }
        contentView.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
        if (binding.webView.url == null && url.isNotBlank()) {
            binding.webView.loadUrl(url)
        }
    }

    private fun getDialogVisibleHeight(): Int {
        val rect = Rect()
        contentView.getWindowVisibleDisplayFrame(rect)
        return rect.height()
    }

    override fun onDestroyView() {
        if (contentView.viewTreeObserver.isAlive) {
            contentView.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutListener)
        }
        bottomSheet = null
        binding.webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
    }

    private inner class BottomSheetWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            val uri = request?.url ?: return false
            return when (uri.scheme?.lowercase()) {
                "http", "https" -> false
                else -> {
                    val context = context ?: return false
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }.onFailure {
                        if (it !is ActivityNotFoundException) {
                            Timber.w(it)
                        }
                    }.isSuccess
                }
            }
        }

        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: android.graphics.Bitmap?,
        ) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?,
        ) {
            super.onPageFinished(view, url)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                toast(R.string.Try_Again)
            }
        }
    }
}
