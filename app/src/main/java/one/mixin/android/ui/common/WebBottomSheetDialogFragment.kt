package one.mixin.android.ui.common

import android.Manifest
import android.app.Activity
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
import android.net.Uri
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.uber.autodispose.autoDispose
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWebBottomSheetBinding
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.isMixinUrl
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openAsUrl
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toUri
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment.Companion.PERMISSION_AUDIO
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment.Companion.PERMISSION_VIDEO
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.convertWcLink
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import java.net.URI
import java.util.Locale

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
    private var lastHandledUrl: Pair<String, Long>? = null
    private var lastGrantedUri: String? = null
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var imageUri: Uri? = null
    private lateinit var fileChooser: ActivityResultLauncher<Intent>
    private lateinit var videoCapture: ActivityResultLauncher<Intent>
    private val keyboardLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            val visibleHeight = getDialogVisibleHeight()
            if (visibleHeight <= 0) return@OnGlobalLayoutListener
            if (visibleHeight == lastVisibleHeight) return@OnGlobalLayoutListener
            lastVisibleHeight = visibleHeight
            bottomSheet?.setCustomViewHeightSync(visibleHeight)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            uploadMessage?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
            )
            uploadMessage = null
        }
        videoCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                uploadMessage?.onReceiveValue(arrayOf(uri))
            } else {
                uploadMessage?.onReceiveValue(null)
            }
            uploadMessage = null
        }
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
            settings.setGeolocationEnabled(true)
            settings.userAgentString = settings.userAgentString + " Mixin/" + BuildConfig.VERSION_NAME + " GOOGLE_PAY_SUPPORTED"
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
                WebSettingsCompat.setPaymentRequestEnabled(settings, true)
            }
            webChromeClient =
                object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        handlePermissionRequest(request)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        return handleFileChooser(filePathCallback, fileChooserParams)
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?,
                    ) {
                        handleGeolocationPermission(origin, callback)
                    }
                }
            webViewClient = BottomSheetWebViewClient()
            setDownloadListener { url, _, _, _, _ ->
                runCatching {
                    startActivity(
                        Intent(ACTION_VIEW).apply {
                            data = url.toUri()
                        },
                    )
                }
            }
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
        uploadMessage?.onReceiveValue(null)
        uploadMessage = null
        super.onDestroyView()
    }

    private fun handlePermissionRequest(request: PermissionRequest?) {
        request ?: return
        val permissions = mutableListOf<String>()
        val promptTypes = mutableListOf<Int>()
        for (resource in request.resources) {
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                    promptTypes.add(PERMISSION_AUDIO)
                }
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    permissions.add(Manifest.permission.CAMERA)
                    promptTypes.add(PERMISSION_VIDEO)
                }
                else -> {
                    lastGrantedUri = null
                    request.deny()
                    return
                }
            }
        }
        if (lastGrantedUri == request.origin.toString()) {
            request.grant(request.resources)
            return
        }

        PermissionBottomSheetDialogFragment.request(
            title,
            null,
            null,
            *promptTypes.toIntArray(),
        )
            .setCancelAction {
                lastGrantedUri = null
                request.deny()
            }.setGrantedAction {
                RxPermissions(requireActivity())
                    .request(*permissions.toTypedArray())
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                lastGrantedUri = request.origin.toString()
                                request.grant(request.resources)
                            } else {
                                lastGrantedUri = null
                                context?.openPermissionSetting()
                            }
                        },
                        {
                            lastGrantedUri = null
                            request.deny()
                        },
                    )
            }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
    }

    private fun handleFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
    ): Boolean {
        uploadMessage?.onReceiveValue(null)
        uploadMessage = filePathCallback
        val intent = fileChooserParams?.createIntent()
        if (fileChooserParams?.isCaptureEnabled == true) {
            when (intent?.type) {
                "video/*" -> {
                    PermissionBottomSheetDialogFragment.requestVideo(title)
                        .setCancelAction { cancelFileChooser() }
                        .setGrantedAction {
                            RxPermissions(requireActivity())
                                .request(Manifest.permission.CAMERA)
                                .autoDispose(stopScope)
                                .subscribe(
                                    { granted ->
                                        if (granted) {
                                            videoCapture.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
                                        } else {
                                            cancelFileChooser()
                                            context?.openPermissionSetting()
                                        }
                                    },
                                    {
                                        cancelFileChooser()
                                    },
                                )
                        }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                    return true
                }
                "image/*" -> {
                    PermissionBottomSheetDialogFragment.requestCamera(title)
                        .setCancelAction { cancelFileChooser() }
                        .setGrantedAction {
                            RxPermissions(requireActivity())
                                .request(Manifest.permission.CAMERA)
                                .autoDispose(stopScope)
                                .subscribe(
                                    { granted ->
                                        if (granted) {
                                            openCamera(getImageUri())
                                        } else {
                                            cancelFileChooser()
                                            context?.openPermissionSetting()
                                        }
                                    },
                                    {
                                        cancelFileChooser()
                                    },
                                )
                        }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                    return true
                }
            }
        }

        if (intent == null) {
            cancelFileChooser()
            return false
        }
        runCatching {
            fileChooser.launch(intent)
        }.onFailure {
            cancelFileChooser()
        }
        return true
    }

    private fun cancelFileChooser() {
        uploadMessage?.onReceiveValue(null)
        uploadMessage = null
    }

    private fun getImageUri(): Uri {
        if (imageUri == null) {
            imageUri = Uri.fromFile(requireContext().getOtherPath().createImageTemp())
        }
        return requireNotNull(imageUri)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CAMERA) {
            imageUri?.let {
                uploadMessage?.onReceiveValue(arrayOf(it))
                imageUri = null
            }
            uploadMessage = null
        } else {
            cancelFileChooser()
        }
    }

    private fun handleGeolocationPermission(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        PermissionBottomSheetDialogFragment.requestLocation(title)
            .setCancelAction {
                callback?.invoke(origin, false, false)
            }.setGrantedAction {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                callback?.invoke(origin, true, false)
                            } else {
                                callback?.invoke(origin, false, false)
                                requireContext().openPermissionSetting()
                            }
                        },
                        {
                            callback?.invoke(origin, false, false)
                        },
                    )
            }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
    }

    private inner class BottomSheetWebViewClient : WebViewClient() {
        private fun interceptMixinUrl(
            view: WebView?,
            url: String,
        ): Boolean {
            if (url.startsWith(Constants.Scheme.WALLET_CONNECT_PREFIX, true) ||
                url.startsWith(Constants.Scheme.MIXIN_WC) ||
                url.startsWith(Constants.Scheme.HTTPS_MIXIN_WC)
            ) {
                view?.stopLoading()
                convertWcLink(url)?.let { wcUri ->
                    UrlInterpreterActivity.show(view?.context ?: requireContext(), wcUri)
                }
                return true
            }

            if (url.isMixinUrl() || url.isExternalTransferUrl() || url.isLightningUrl()) {
                val now = System.currentTimeMillis()
                if (url == lastHandledUrl?.first && now - (lastHandledUrl?.second ?: 0L) <= 1000L) {
                    view?.stopLoading()
                    return true
                }
                lastHandledUrl = url to now
                view?.stopLoading()
                val host = view?.url?.let { Uri.parse(it).host }
                url.openAsUrl(
                    view?.context ?: requireContext(),
                    parentFragmentManager,
                    lifecycleScope,
                    host = host,
                ) {}
                return true
            }

            return false
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            val uri = request?.url ?: return false
            val url = uri.toString()
            if (interceptMixinUrl(view, url)) {
                return true
            }

            return when (uri.scheme?.lowercase()) {
                "http", "https" -> false
                else -> {
                    openExternalUrl(view, request, url)
                }
            }
        }

        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: android.graphics.Bitmap?,
        ) {
            if (!url.isNullOrBlank() && url != this@WebBottomSheetDialogFragment.url && interceptMixinUrl(view, url)) {
                return
            }
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

    private fun openExternalUrl(
        view: WebView?,
        request: WebResourceRequest,
        url: String,
    ): Boolean {
        return try {
            val context = view?.context ?: context ?: return false
            val intent =
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    action = ACTION_VIEW
                    addCategory(CATEGORY_BROWSABLE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        flags = FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                    }
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            try {
                context.startActivity(intent)
                if (request.isForMainFrame) {
                    dismiss()
                }
                true
            } catch (e: ActivityNotFoundException) {
                val fallbackUrl = intent.extras?.getString("browser_fallback_url")
                if (fallbackUrl != null && isFallbackUrlValid(fallbackUrl)) {
                    view?.loadUrl(fallbackUrl)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            if (e !is ActivityNotFoundException) {
                Timber.w(e)
            }
            false
        }
    }

    private fun isFallbackUrlValid(fallbackUrl: String): Boolean {
        return try {
            val scheme = URI(fallbackUrl).scheme?.lowercase(Locale.US)
            scheme == "http" || scheme == "https"
        } catch (e: Exception) {
            false
        }
    }
}
