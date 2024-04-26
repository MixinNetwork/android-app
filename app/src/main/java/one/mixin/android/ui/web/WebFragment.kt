package one.mixin.android.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewClient.ERROR_CONNECT
import android.webkit.WebViewClient.ERROR_HOST_LOOKUP
import android.webkit.WebViewClient.ERROR_IO
import android.webkit.WebViewClient.ERROR_TIMEOUT
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ShareCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import one.mixin.android.web3.js.DAppMethod
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Mixin_Conversation_ID_HEADER
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.databinding.FragmentWebBinding
import one.mixin.android.databinding.ViewWebBottomMenuBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isDarkColor
import one.mixin.android.extension.isMixinUrl
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.matchResourcePattern
import one.mixin.android.extension.openAsUrl
import one.mixin.android.extension.openAsUrlOrQrScan
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.showPipPermissionNotification
import one.mixin.android.extension.toUri
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipSignSpec
import one.mixin.android.tip.tipPrivToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menu
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment.Companion.PERMISSION_AUDIO
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment.Companion.PERMISSION_VIDEO
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.player.MusicActivity
import one.mixin.android.ui.player.MusicService
import one.mixin.android.ui.player.MusicService.Companion.MUSIC_PLAYLIST
import one.mixin.android.ui.qr.QRCodeProcessor
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.setting.SettingActivity.Companion.ARGS_SUCCESS
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import one.mixin.android.ui.tip.wc.showWalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.getCountry
import one.mixin.android.util.getLanguage
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCap
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.web3.js.JsInjectorClient
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SwitchChain
import one.mixin.android.web3.convertWcLink
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.FailLoadView
import one.mixin.android.widget.MixinWebView
import one.mixin.android.widget.SuspiciousLinkView
import one.mixin.android.widget.WebControlView
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class WebFragment : BaseFragment() {
    companion object {
        const val TAG = "WebFragment"
        private const val FILE_CHOOSER = 0x01
        private const val CONTEXT_MENU_ID_SCAN_IMAGE = 0x11
        private const val CONTEXT_MENU_ID_SAVE_IMAGE = 0x12
        const val URL = "url"
        const val CONVERSATION_ID = "conversation_id"
        const val ARGS_APP = "args_app"
        const val ARGS_APP_CARD = "args_app_card"
        const val ARGS_INDEX = "args_index"
        const val ARGS_SHAREABLE = "args_shareable"
        const val themeColorScript =
            """
            (function() {
                var metas = document.getElementsByTagName('meta');
                for (var i = 0; i < metas.length; i++) {
                    if (metas[i].getAttribute('name') === 'theme-color' && metas[i].hasAttribute('content')) {
                        return metas[i].getAttribute('content');
                    }
                }
                return '';
            }) ();
            """

        fun newInstance(
            bundle: Bundle,
        ) = WebFragment().apply {
            arguments = bundle
        }
    }

    private val bottomViewModel by viewModels<BottomSheetViewModel>()
    private val url: String by lazy {
        requireArguments().getString(URL)!!
    }
    private val conversationId: String? by lazy {
        requireArguments().getString(CONVERSATION_ID)
    }
    private var app: App? = null
    private val appCard: AppCardData? by lazy {
        requireArguments().getParcelableCompat(ARGS_APP_CARD, AppCardData::class.java)
    }
    private val shareable: Boolean by lazy {
        requireArguments().getBoolean(ARGS_SHAREABLE, true)
    }

    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var isFinished: Boolean = false
    private val processor = QRCodeProcessor()
    private var webAppInterface: WebAppInterface? = null
    private var index: Int = -1

    fun resetIndex(index: Int) {
        if (this.index == index) {
            this.index = -1
        }
    }

    @Inject
    lateinit var tip: Tip

    private lateinit var getPermissionResult: ActivityResultLauncher<Pair<App, AuthorizationResponse>>

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        webView.hitTestResult.let {
            when (it.type) {
                WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    menu.add(0, CONTEXT_MENU_ID_SCAN_IMAGE, 0, R.string.Extract_QR_Code)
                    menu.getItem(0).setOnMenuItemClickListener { menu ->
                        onContextItemSelected(menu)
                        return@setOnMenuItemClickListener true
                    }
                    menu.add(0, CONTEXT_MENU_ID_SAVE_IMAGE, 1, R.string.Save_image)
                    menu.getItem(1).setOnMenuItemClickListener { menu ->
                        onContextItemSelected(menu)
                        return@setOnMenuItemClickListener true
                    }
                }
                else -> Timber.d("App does not yet handle target type: ${it.type}")
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        webView.hitTestResult.let {
            val url = it.extra
            if (item.itemId == CONTEXT_MENU_ID_SCAN_IMAGE) {
                lifecycleScope.launch {
                    try {
                        val bitmap =
                            withContext(Dispatchers.IO) {
                                Glide.with(requireContext())
                                    .asBitmap()
                                    .load(url)
                                    .submit()
                                    .get(10, TimeUnit.SECONDS)
                            }
                        if (isDetached) return@launch

                        processor.detect(
                            lifecycleScope,
                            bitmap,
                            onSuccess = { result ->
                                result.openAsUrlOrQrScan(
                                    requireActivity(),
                                    parentFragmentManager,
                                    lifecycleScope,
                                )
                            },
                            onFailure = {
                                if (isAdded) toast(R.string.can_not_recognize_qr_code)
                            },
                        )
                    } catch (e: Exception) {
                        if (isAdded) toast(R.string.can_not_recognize_qr_code)
                    }
                }
                return true
            } else if (item.itemId == CONTEXT_MENU_ID_SAVE_IMAGE) {
                saveImageFromUrl(url)
            }
        }
        return super.onContextItemSelected(item)
    }

    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getPermissionResult = registerForActivityResult(SettingActivity.PermissionContract(), requireActivity().activityResultRegistry, ::callbackPermission)
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia(), requireActivity().activityResultRegistry, ::callbackPicker)
    }

    var uploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = requireArguments().getInt(ARGS_INDEX, -1)
    }

    private var _binding: FragmentWebBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWebBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    private lateinit var contentView: ViewGroup
    private lateinit var webView: MixinWebView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        contentView = binding.container
        webView =
            if (index >= 0 && index < clips.size) {
                clips[index].let { clip ->
                    binding.titleTv.text = clip.name
                    clip.icon?.let { icon ->
                        this.icon = icon
                        binding.iconIv.isVisible = true
                        binding.iconIv.setImageBitmap(icon)
                    }
                    isFinished = clip.isFinished
                    clip.webView ?: MixinWebView(MixinApplication.get().contextWrapper)
                }
            } else {
                MixinWebView(MixinApplication.get().contextWrapper)
            }
        if (webView.parent != null) {
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
        binding.webLl.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        binding.webControl.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = requireContext().dpToPx(6f)
        }
        registerForContextMenu(webView)

        WebView.setWebContentsDebuggingEnabled(
            defaultSharedPreferences.getBoolean(
                Constants.Debug.WEB_DEBUG,
                false,
            ),
        )

        app = requireArguments().getParcelableCompat(ARGS_APP, App::class.java)

        initView()

        val card = appCard
        if (card != null) {
            checkAppCard(card)
        } else {
            loadWebView()
        }
    }

    private fun checkAppCard(appCard: AppCardData) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            if (appCard.appId != null) {
                app = bottomViewModel.getAppAndCheckUser(appCard.appId, appCard.updatedAt)
                if (url.matchResourcePattern(app?.resourcePatterns)) {
                    controlSuspiciousView(false)
                    loadWebView()
                } else {
                    controlSuspiciousView(true)
                }
            } else {
                loadWebView()
            }
        }

    private fun controlSuspiciousView(show: Boolean) {
        _binding?.apply {
            suspiciousLinkView.isVisible = show
            if (show) {
                pb.isVisible = false
            }
        }
    }

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() {
        binding.suspiciousLinkView.listener =
            object : SuspiciousLinkView.SuspiciousListener {
                override fun onBackClick() {
                    requireActivity().finish()
                }

                override fun onContinueClick() {
                    loadWebView()
                    controlSuspiciousView(false)
                }
            }
        binding.failLoadView.contactTv.isInvisible = app == null
        binding.failLoadView.listener =
            object : FailLoadView.FailLoadListener {
                override fun onReloadClick() {
                    refresh()
                }

                override fun onContactClick() {
                    app?.let { app ->
                        lifecycleScope.launch {
                            val user = bottomViewModel.refreshUser(app.creatorId)
                            if (user != null) {
                                if (app.creatorId == Session.getAccountId()) {
                                    ProfileBottomSheetDialogFragment.newInstance()
                                        .showNow(parentFragmentManager, ProfileBottomSheetDialogFragment.TAG)
                                    return@launch
                                }

                                ConversationActivity.show(requireContext(), recipientId = app.creatorId)
                                this@WebFragment.requireActivity().finish()
                            }
                        }
                    }
                }
            }

        binding.webControl.callback =
            object : WebControlView.Callback {
                override fun onMoreClick() {
                    showBottomSheet()
                }

                override fun onCloseClick() {
                    requireActivity().finish()
                }
            }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.textZoom = 100
        webView.settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.userAgentString =
            webView.settings.userAgentString + " Mixin/" + BuildConfig.VERSION_NAME

        webView.webViewClient =
            WebViewClientImpl(
                object : WebViewClientImpl.OnPageFinishedListener {
                    override fun onPageFinished() {
                        reloadTheme()
                    }
                },
                conversationId,
                MixinApplication.appContext,
                this.parentFragmentManager,
                requireActivity().activityResultRegistry,
                lifecycleScope,
                { url ->
                    currentUrl = url
                    isFinished = true
                }, { title, url ->
                    currentUrl = url
                    currentTitle = title
                },
                { errorCode, _, failingUrl ->
                    currentUrl = failingUrl
                    if (errorCode == ERROR_HOST_LOOKUP ||
                        errorCode == ERROR_CONNECT ||
                        errorCode == ERROR_IO ||
                        errorCode == ERROR_TIMEOUT
                    ) {
                        _binding?.apply {
                            failLoadView.webFailDescription.text =
                                getString(R.string.web_cannot_reached_desc, failingUrl)
                            failLoadView.isVisible = true
                        }
                    }
                },
            )

        webView.webChromeClient =
            object : WebChromeClient() {
                override fun onShowCustomView(
                    view: View,
                    requestedOrientation: Int,
                    callback: CustomViewCallback?,
                ) {
                    onShowCustomView(view, callback)
                }

                override fun onShowCustomView(
                    view: View,
                    callback: CustomViewCallback?,
                ) {
                    if (customView != null || !isAdded) {
                        customViewCallback?.onCustomViewHidden()
                        return
                    }
                    customView = view
                    customViewCallback = callback
                    originalSystemUiVisibility = requireActivity().window.decorView.systemUiVisibility

                    binding.customViewContainer.addView(view)
                    binding.customViewContainer.isVisible = true
                    binding.webLl.isVisible = false
                    binding.webControl.isVisible = false

                    requireActivity().window.decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onHideCustomView() {
                    if (customView == null) {
                        return
                    }
                    _binding?.apply {
                        customViewContainer.isVisible = false
                        webLl.isVisible = true
                        webControl.isVisible = true
                        customViewContainer.removeView(customView)
                    }

                    customView = null

                    requireActivity().window.decorView.systemUiVisibility = originalSystemUiVisibility
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }

                override fun onReceivedTitle(
                    view: WebView?,
                    title: String?,
                ) {
                    super.onReceivedTitle(view, title)
                    if (!isBot()) {
                        _binding?.titleTv?.text = title
                    }
                }

                override fun onReceivedIcon(
                    view: WebView?,
                    icon: Bitmap?,
                ) {
                    super.onReceivedIcon(view, icon)
                    if (!isBot()) {
                        icon?.let {
                            _binding?.apply {
                                iconIv.isVisible = true
                                iconIv.setImageBitmap(it)
                            }
                            this@WebFragment.icon = it
                        }
                    }
                }

                private var lastGrantedUri: String? = null

                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.let {
                        val permission = mutableListOf<String>()
                        for (code in request.resources) {
                            if (code == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                                permission.add(Manifest.permission.RECORD_AUDIO)
                            } else if (code == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                                permission.add(Manifest.permission.CAMERA)
                            }
                            if (code != PermissionRequest.RESOURCE_VIDEO_CAPTURE && code != PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                                request.deny()
                                lastGrantedUri = null
                                return@let
                            }
                        }
                        if (lastGrantedUri == request.origin.toString()) {
                            request.grant(request.resources)
                            return@let
                        }

                        PermissionBottomSheetDialogFragment.request(
                            binding.titleTv.text.toString(),
                            app?.name,
                            app?.appNumber,
                            *permission.map {
                                if (it == Manifest.permission.RECORD_AUDIO) {
                                    PERMISSION_AUDIO
                                } else {
                                    PERMISSION_VIDEO
                                }
                            }.toIntArray(),
                        )
                            .setCancelAction {
                                lastGrantedUri = null
                                request.deny()
                            }.setGrantedAction {
                                RxPermissions(requireActivity())
                                    .request(*permission.toTypedArray())
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
                                        },
                                    )
                            }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?,
                ): Boolean {
                    if (viewDestroyed()) return false
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = filePathCallback
                    val intent: Intent? = fileChooserParams?.createIntent()
                    if (fileChooserParams?.isCaptureEnabled == true) {
                        if (intent?.type == "video/*") {
                            PermissionBottomSheetDialogFragment.requestVideo(
                                binding.titleTv.text.toString(),
                                app?.name,
                                app?.appNumber,
                            )
                                .setCancelAction {
                                    uploadMessage?.onReceiveValue(null)
                                    uploadMessage = null
                                }
                                .setGrantedAction {
                                    RxPermissions(requireActivity())
                                        .request(Manifest.permission.CAMERA)
                                        .autoDispose(stopScope)
                                        .subscribe(
                                            { granted ->
                                                if (granted) {
                                                    startActivityForResult(
                                                        Intent(MediaStore.ACTION_VIDEO_CAPTURE),
                                                        FILE_CHOOSER,
                                                    )
                                                } else {
                                                    context?.openPermissionSetting()
                                                }
                                            },
                                            {
                                            },
                                        )
                                }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                            return true
                        } else if (intent?.type == "image/*") {
                            PermissionBottomSheetDialogFragment.requestCamera(
                                binding.titleTv.text.toString(),
                                app?.appNumber,
                                app?.iconUrl,
                            )
                                .setCancelAction {
                                    uploadMessage?.onReceiveValue(null)
                                    uploadMessage = null
                                }.setGrantedAction {
                                    RxPermissions(requireActivity())
                                        .request(Manifest.permission.CAMERA)
                                        .autoDispose(stopScope)
                                        .subscribe(
                                            { granted ->
                                                if (granted) {
                                                    openCamera(getImageUri())
                                                } else {
                                                    context?.openPermissionSetting()
                                                }
                                            },
                                            {
                                            },
                                        )
                                }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                            return true
                        }
                    }
                    pickMedia.launch(
                        when (intent?.type) {
                            "image/*" -> {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            }
                            "video/*" -> {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            }
                            else -> {
                                PickVisualMediaRequest()
                            }
                        },
                    )
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?,
                ) {
                    PermissionBottomSheetDialogFragment.requestLocation(
                        binding.titleTv.text.toString(),
                        app?.appNumber,
                        app?.iconUrl,
                    )
                        .setCancelAction {
                            callback?.invoke(origin, false, false)
                        }.setGrantedAction {
                            RxPermissions(requireActivity())
                                .request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                                .autoDispose(stopScope)
                                .subscribe { granted ->
                                    if (granted) {
                                        callback?.invoke(origin, true, false)
                                    } else {
                                        requireContext().openPermissionSetting()
                                    }
                                }
                        }.show(parentFragmentManager, PermissionBottomSheetDialogFragment.TAG)
                }
            }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                startActivity(
                    Intent(ACTION_VIEW).apply {
                        data = url.toUri()
                    },
                )
            } catch (ignored: ActivityNotFoundException) {
            }
        }
    }

    @Override
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
        } else if (requestCode == FILE_CHOOSER && resultCode == Activity.RESULT_OK) {
            val dataString = data?.dataString
            if (dataString != null) {
                callbackPicker(Uri.parse(dataString))
            } else {
                callbackPicker(null)
            }
        } else {
            uploadMessage?.onReceiveValue(null)
            uploadMessage = null
        }
    }

    private fun loadWebView() {
        _binding?.let { binding ->
            binding.pb.isVisible = false
            var immersive = false
            app?.capabilities?.let {
                if (it.contains(AppCap.IMMERSIVE.name)) {
                    immersive = true
                }
            }
            app?.name?.let { binding.titleTv.text = it }
            app?.iconUrl?.let {
                binding.iconIv.isVisible = true
                binding.iconIv.loadImage(it)
                binding.titleTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    marginStart = requireContext().dpToPx(10f)
                }
            }
            binding.titleLl.isGone = immersive

            webAppInterface =
                WebAppInterface(
                    MixinApplication.appContext,
                    conversationId,
                    immersive,
                    reloadThemeAction = { reloadTheme() },
                    playlistAction = { showPlaylist(it) },
                    closeAction = {
                        lifecycleScope.launch {
                            closeSelf()
                        }
                    },
                    getTipAddressAction = { chainId, callback ->
                        getTipAddress(chainId, callback)
                    },
                    tipSignAction = { chainId, message, callback ->
                        tipSign(chainId, message, callback)
                    },
                    getAssetAction = { ids, callback ->
                        getAssets(ids, callback)
                    },
                )
            webAppInterface?.let { webView.addJavascriptInterface(it, "MixinContext") }
            webView.addJavascriptInterface(Web3Interface(
                onWalletActionSuccessful = { e ->
                    lifecycleScope.launch {
                        webView.evaluateJavascript(e, Timber::d)
                    }
                },
                onWalletActionError = { id->
                    lifecycleScope.launch {
                        webView.evaluateJavascript("window.${JsSigner.currentNetwork}.sendResponse(${id}, null)") {}
                    }
                },
                onBrowserSign = { message ->
                    lifecycleScope.launch {
                        showBrowserBottomSheetDialogFragment(
                            requireActivity(),
                            message,
                            currentUrl = currentUrl,
                            currentTitle = currentTitle,
                            onReject = {
                                lifecycleScope.launch {
                                    webView.evaluateJavascript("window.${JsSigner.currentNetwork}.sendResponse(${message.callbackId}, null)") {}
                                }
                            },
                            onDone = { callback ->
                                lifecycleScope.launch {
                                    if (callback != null) webView.evaluateJavascript(callback) {}
                                }
                            },
                        )
                    }
                },
            ), "_mw_")
            val extraHeaders = HashMap<String, String>()
            conversationId?.let {
                extraHeaders[Mixin_Conversation_ID_HEADER] = it
            }
            if (isFinished) {
                if (index >= 0) {
                    refreshByLuminance(requireContext().isNightMode(), clips[index].titleColor)
                }
                return
            } else if (webView.url != null) {
                webView.reload()
                return
            }
            webView.loadUrl(url, extraHeaders)
        }
    }

    private fun closeSelf() {
        if (viewDestroyed()) return
        requireActivity().finish()
    }

    private fun showPlaylist(playlist: Array<String>) {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            if (!checkFloatingPermission()) return@launch

            MusicService.playUrls(requireContext(), playlist)
            if (checkFloatingPermission()) {
                one.mixin.android.ui.player.collapse(MUSIC_PLAYLIST)
            } else {
                requireActivity().showPipPermissionNotification(
                    MusicActivity::class.java,
                    getString(R.string.web_floating_permission),
                )
            }
        }
    }

    private fun reloadTheme() {
        if (viewDestroyed()) return

        lifecycleScope.launch {
            webView.evaluateJavascript(themeColorScript) {
                setStatusBarColor(it)
            }
        }
    }

    private fun getTipAddress(
        chainId: String,
        callbackFunction: String,
    ) {
        if (viewDestroyed()) return
        if (!WalletConnect.isEnabled()) return
        val isValid = chainId.isUUID()
        if (!isValid) {
            lifecycleScope.launch {
                webView.evaluateJavascript("$callbackFunction('')") {}
            }
        }
        lifecycleScope.launch {
            WalletConnectTIP.peer = getPeerUI(PropertyHelper.findValueByKey(EVM_ADDRESS, ""))
            showWalletConnectBottomSheetDialogFragment(
                tip,
                requireActivity(),
                WalletConnect.RequestType.SessionProposal,
                WalletConnect.Version.TIP,
                null,
                onReject = {
                    lifecycleScope.launch {
                        webView.evaluateJavascript("$callbackFunction('')") {}
                    }
                },
                callback = {
                    val address =
                        try {
                            tipPrivToAddress(it, chainId)
                        } catch (e: IllegalArgumentException) {
                            Timber.d("${WalletConnectTIP.TAG} ${e.stackTraceToString()}")
                            ""
                        }
                    lifecycleScope.launch {
                        webView.evaluateJavascript("$callbackFunction('$address')") {}
                    }
                },
            )
        }
    }

    private fun getAssets(
        ids: Array<String>,
        callbackFunction: String,
    ) {
        if (viewDestroyed()) return
        app ?: return

        lifecycleScope.launch {
            val sameHost =
                try {
                    Uri.parse(webView.url).host == Uri.parse(app?.homeUri ?: "").host
                    true
                } catch (e: Exception) {
                    false
                }
            if (!sameHost) {
                webView.evaluateJavascript("$callbackFunction('[]')") {}
                return@launch
            }
            val isValid = ids.isEmpty() || ids.all { it.isUUID() }
            if (!isValid) {
                webView.evaluateJavascript("$callbackFunction('[]')") {}
                return@launch
            }
            val auth = bottomViewModel.getAuthorizationByAppId(app!!.appId)
            val result =
                if (auth?.scopes?.contains("ASSETS:READ") == true) {
                    val tokens =
                        if (ids.isEmpty()) {
                            bottomViewModel.tokenEntry()
                        } else {
                            bottomViewModel.tokenEntry(ids)
                        }
                    GsonHelper.customGson.toJson(tokens)
                } else {
                    "[]"
                }
            webView.evaluateJavascript("$callbackFunction('$result')") {}
        }
    }

    private fun tipSign(
        chainId: String,
        message: String,
        callbackFunction: String,
    ) {
        if (viewDestroyed()) return
        if (!WalletConnect.isEnabled()) return

        lifecycleScope.launch {
            WalletConnectTIP.signData = WalletConnect.WCSignData.TIPSignData(message)
            showWalletConnectBottomSheetDialogFragment(
                tip,
                requireActivity(),
                WalletConnect.RequestType.SessionRequest,
                WalletConnect.Version.TIP,
                null,
                onReject = {
                    lifecycleScope.launch {
                        webView.evaluateJavascript("$callbackFunction('')") {}
                    }
                },
                callback = {
                    val sig = TipSignSpec.Ecdsa.Secp256k1.sign(tipPrivToPrivateKey(it, chainId), message.toByteArray())
                    lifecycleScope.launch {
                        webView.evaluateJavascript("$callbackFunction('$sig')") {}
                    }
                },
            )
        }
    }

    private fun getPeerUI(account: String): PeerUI {
        val a = app
        return if (a != null) {
            PeerUI(
                uri = a.homeUri,
                name = a.name,
                icon = a.iconUrl,
                desc = a.description,
                account = account,
            )
        } else {
            PeerUI(
                uri = webView.url ?: url,
                name = webView.title ?: "",
                icon = "",
                desc = "",
                account = account,
            )
        }
    }

    private var imageUri: Uri? = null

    private fun getImageUri(): Uri {
        if (imageUri == null) {
            imageUri = Uri.fromFile(requireContext().getOtherPath().createImageTemp())
        }
        return imageUri!!
    }

    private fun isBot() = app != null

    private var hold = false
    private var icon: Bitmap? = null

    private fun generateWebClip(): WebClip? {
        val currentUrl = webView.url ?: url
        val v = webView
        if (v.height <= 0) return null
        val screenshot = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.RGB_565)
        val c = Canvas(screenshot)
        c.translate((-v.scrollX).toFloat(), (-v.scrollY).toFloat())
        v.draw(c)

        webAppInterface?.reloadThemeAction = null
        webAppInterface?.playlistAction = null
        webAppInterface = null
        webView.removeJavascriptInterface("MixinContext")
        webView.removeJavascriptInterface("mixin")
        webView.webChromeClient = null
        webView.webViewClient = object : WebViewClient() {}

        return WebClip(
            currentUrl,
            app,
            titleColor,
            app?.name ?: binding.titleTv.text.toString(),
            screenshot,
            icon,
            conversationId,
            appCard?.shareable ?: shareable,
            webView,
            isFinished,
        )
    }

    var titleColor: Int = Color.WHITE

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDestroyView() {
        webView.stopLoading()
        when {
            hold -> {
                generateWebClip()?.let { webClip ->
                    holdClip(webClip)
                }
            }

            index < 0 -> {
                webView.destroy()
                webView.webViewClient = object : WebViewClient() {}
                webView.webChromeClient = null
            }

            else -> {
                generateWebClip()?.let { webClip ->
                    updateClip(index, webClip)
                }
            }
        }
        progressDialog?.dismiss()
        permissionAlert?.dismiss()
        unregisterForContextMenu(webView)
        binding.webLl.removeView(webView)
        processor.close()
        if (requireActivity().requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requireActivity().window.decorView.systemUiVisibility = originalSystemUiVisibility
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        JsSigner.reset()
        super.onDestroyView()
        _binding = null
    }

    private fun showBottomSheet() {
        if (viewDestroyed()) return
        val builder = BottomSheet.Builder(requireActivity())
        val view =
            View.inflate(
                ContextThemeWrapper(requireActivity(), R.style.Custom),
                R.layout.view_web_bottom_menu,
                null,
            )
        val viewBinding = ViewWebBottomMenuBinding.bind(view)
        if (isBot()) {
            app?.let {
                viewBinding.avatar.loadImage(it.iconUrl)
                viewBinding.nameTv.text = it.name
                viewBinding.descTv.text = it.appNumber
            }
            viewBinding.avatar.isVisible = true
        } else {
            viewBinding.nameTv.text = binding.titleTv.text
            viewBinding.descTv.text = webView.url
            viewBinding.avatar.isVisible = false
        }
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.closeIv.setOnClickListener { bottomSheet.dismiss() }
        val shareMenu =
            menu {
                title = getString(if (isBot()) R.string.About else R.string.Share)
                icon = if (isBot()) R.drawable.ic_setting_about else R.drawable.ic_web_share
                action = {
                    if (isBot()) {
                        openBot()
                    } else {
                        activity?.let {
                            ShareCompat.IntentBuilder
                                .from(it)
                                .setType("text/plain")
                                .setChooserTitle(webView.title)
                                .setText(webView.url)
                                .startChooser()
                            bottomSheet.dismiss()
                        }
                    }
                    bottomSheet.dismiss()
                }
            }
        val forwardMenu =
            menu {
                title = getString(R.string.Forward)
                icon = R.drawable.ic_web_forward
                action = {
                    if (appCard?.shareable == false || !shareable) {
                        toast(R.string.app_card_shareable_false)
                    } else {
                        val currentUrl = webView.url ?: url
                        if (isBot()) {
                            app?.appId?.let { id ->
                                lifecycleScope.launch {
                                    val app = bottomViewModel.getAppAndCheckUser(id, app?.updatedAt)
                                    if (app !== null && currentUrl.matchResourcePattern(app.resourcePatterns)) {
                                        var webTitle = webView.title
                                        if (webTitle.isNullOrBlank()) {
                                            webTitle = app.name
                                        }
                                        val appCardData =
                                            AppCardData(
                                                app.appId,
                                                app.iconUrl,
                                                webTitle,
                                                app.name,
                                                currentUrl,
                                                app.updatedAt,
                                                null,
                                            )
                                        ForwardActivity.show(
                                            requireContext(),
                                            arrayListOf(
                                                ForwardMessage(
                                                    ShareCategory.AppCard,
                                                    GsonHelper.customGson.toJson(appCardData),
                                                ),
                                            ),
                                            ForwardAction.App.Resultless(),
                                        )
                                    } else {
                                        ForwardActivity.show(requireContext(), currentUrl)
                                    }
                                }
                            }
                        } else {
                            ForwardActivity.show(requireContext(), currentUrl)
                        }
                        bottomSheet.dismiss()
                    }
                }
            }
        val refreshMenu =
            menu {
                title = getString(R.string.Refresh)
                icon = R.drawable.ic_web_refresh
                action = {
                    refresh()
                    bottomSheet.dismiss()
                }
            }
        val openMenu =
            menu {
                title = getString(R.string.Open_in_browser)
                icon = R.drawable.ic_web_browser
                action = {
                    (webView.url ?: currentUrl)?.let {
                        context?.openUrl(it)
                    }
                    bottomSheet.dismiss()
                }
            }
        val scanMenu =
            menu {
                title = getString(R.string.Scan_QR_Code)
                icon = R.drawable.ic_bot_category_scan
                action = {
                    tryScanQRCode()
                    bottomSheet.dismiss()
                }
            }
        val copyMenu =
            menu {
                title = getString(R.string.Copy_link)
                icon = R.drawable.ic_content_copy
                action = {
                    requireContext().getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    toast(R.string.copied_to_clipboard)
                    bottomSheet.dismiss()
                }
            }
        val isHold = isHold()
        val floatingMenu =
            menu {
                title =
                    if (isHold) {
                        getString(R.string.Cancel_Floating)
                    } else {
                        getString(R.string.Floating)
                    }
                icon =
                    if (isHold) {
                        R.drawable.ic_web_floating_cancel
                    } else {
                        R.drawable.ic_web_floating
                    }
                action = {
                    if (isHold) {
                        releaseClip(index)
                        index = -1
                        bottomSheet.dismiss()
                    } else {
                        if (clips.size >= 6) {
                            toast(R.string.web_full)
                            bottomSheet.dismiss()
                        } else if (checkFloatingPermission()) {
                            hold = true
                            bottomSheet.fakeDismiss(false) {
                                requireActivity().finish()
                            }
                        }
                    }
                }
            }
        val viewAuthMenu =
            menu {
                title = getString(R.string.View_Authorization)
                icon = R.drawable.ic_web_floating
                action = {
                    val app = requireNotNull(app)
                    lifecycleScope.launch {
                        bottomSheet.dismiss()
                        val pb =
                            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                setCancelable(false)
                            }
                        val auth = bottomViewModel.getAuthorizationByAppId(app.appId)
                        if (auth == null) {
                            toast(R.string.bot_not_auth_yet)
                            pb.dismiss()
                            return@launch
                        }
                        pb.dismiss()
                        getPermissionResult.launch(Pair(app, auth))
                    }
                }
            }
        val list =
            if (isBot()) {
                menuList {
                    menuGroup {
                        menu(forwardMenu)
                        menu(floatingMenu)
                        menu(refreshMenu)
                    }
                    menuGroup {
                        menu(shareMenu)
                        menu(viewAuthMenu)
                    }
                }
            } else {
                menuList {
                    menuGroup {
                        menu(forwardMenu)
                        menu(shareMenu)
                        menu(floatingMenu)
                        menu(refreshMenu)
                    }
                    menuGroup {
                        menu(scanMenu)
                        menu(copyMenu)
                        menu(openMenu)
                    }
                }
            }
        list.createMenuLayout(requireContext()).let { layout ->
            viewBinding.root.addView(layout)
            layout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = requireContext().dpToPx(30f)
            }
        }
        bottomSheet.show()
    }

    private fun callbackPermission(data: Intent?) {
        val success = data?.getBooleanExtra(ARGS_SUCCESS, false) ?: false
        if (!success) return

        webView.loadUrl("javascript:localStorage.clear()")
    }

    private fun callbackPicker(uri: Uri?) {
        if (uri != null) {
            uploadMessage?.onReceiveValue(arrayOf(uri))
            uploadMessage = null
        } else {
            uploadMessage?.onReceiveValue(null)
            uploadMessage = null
        }
    }

    private var permissionAlert: AlertDialog? = null

    private fun checkFloatingPermission() =
        requireContext().checkInlinePermissions {
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions

            permissionAlert =
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.web_floating_permission)
                    .setPositiveButton(R.string.Settings) { dialog, _ ->
                        try {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${requireContext().packageName}"),
                                ),
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                        dialog.dismiss()
                    }.show()
        }

    private fun isHold(): Boolean {
        return index >= 0
    }

    private fun refresh() {
        webView.clearCache(true)
        webView.reload()
        _binding?.failLoadView?.isVisible = false
    }

    private fun openBot() =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            if (app?.appId != null) {
                val u = bottomViewModel.suspendFindUserById(app?.appId!!)
                if (u != null) {
                    showUserBottom(parentFragmentManager, u, conversationId)
                }
            }
        }

    private var progressDialog: Dialog? = null

    private fun tryScanQRCode() {
        progressDialog =
            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                show()
            }
        val bitmap = webView.drawToBitmap()
        processor.detect(
            lifecycleScope,
            bitmap,
            onSuccess = { result ->
                result.openAsUrlOrQrScan(
                    requireActivity(),
                    parentFragmentManager,
                    lifecycleScope,
                )
                progressDialog?.dismiss()
            },
            onFailure = {
                toast(R.string.can_not_recognize_qr_code)
                progressDialog?.dismiss()
            },
        )
    }

    override fun onPause() {
        if (!requireActivity().isInMultiWindowMode) {
            webView.onPause()
            webView.pauseTimers()
        }
        super.onPause()
    }

    override fun onResume() {
        if (!requireActivity().isInMultiWindowMode) {
            webView.onResume()
            webView.resumeTimers()
        }
        super.onResume()
    }

    private fun saveImageFromUrl(url: String?) {
        if (viewDestroyed()) return

        fun afterGranted() {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val outFile =
                        requireContext().getPublicPicturePath()
                            .createImageTemp(noMedia = false)
                    val encodingPrefix = "base64,"
                    val prefixIndex = url?.indexOf(encodingPrefix)
                    if (url != null && prefixIndex != null && prefixIndex != -1) {
                        val dataStartIndex = prefixIndex + encodingPrefix.length
                        val imageData =
                            Base64.decode(url.substring(dataStartIndex), Base64.DEFAULT)
                        outFile.copyFromInputStream(ByteArrayInputStream(imageData))
                    } else {
                        val file =
                            Glide.with(MixinApplication.appContext)
                                .asFile()
                                .load(url)
                                .submit()
                                .get(10, TimeUnit.SECONDS)
                        outFile.copyFromInputStream(FileInputStream(file))
                    }
                    MediaScannerConnection.scanFile(
                        requireContext(),
                        arrayOf(outFile.toString()),
                        null,
                        null,
                    )
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            toast(
                                getString(
                                    R.string.Save_to,
                                    outFile.absolutePath,
                                ),
                            )
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { if (isAdded) toast(R.string.Save_failure) }
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        afterGranted()
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        } else {
            afterGranted()
        }
    }

    private fun setStatusBarColor(content: String) {
        try {
            val color = content.replace("\"", "")
            val c = Color.parseColor(color)
            val dark = isDarkColor(c)
            refreshByLuminance(dark, c)
        } catch (e: Exception) {
            context?.let {
                refreshByLuminance(it.isNightMode(), it.colorFromAttribute(R.attr.icon_white))
            }
        }
    }

    private fun refreshByLuminance(
        dark: Boolean,
        color: Int,
    ) {
        if (viewDestroyed()) return

        requireActivity().window.statusBarColor = color
        requireActivity().window?.let {
            SystemUIManager.setAppearanceLightStatusBars(it, !dark)
        }
        titleColor = color
        binding.titleTv.setTextColor(if (dark) Color.WHITE else Color.BLACK)
        binding.titleLl.setBackgroundColor(color)
        binding.webControl.mode = dark
    }

    @Suppress("DEPRECATION")
    class WebViewClientImpl(
        private val onPageFinishedListener: OnPageFinishedListener,
        val conversationId: String?,
        private val context: Context,
        private val fragmentManager: FragmentManager,
        private val registry: ActivityResultRegistry,
        private val scope: CoroutineScope,
        private val onFinished: (url: String?) -> Unit,
        private val onWebpageLoaded: (title: String?, url: String?) -> Unit,
        private val onReceivedError: (request: Int?, description: String?, failingUrl: String?) -> Unit,
    ) : WebViewClient() {
        private var redirect = false
        private var loadingError = false
        private val jsInjectorClient by lazy {
            JsInjectorClient()
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view ?: return
            view.clearCache(true)
            Timber.e("onPageStarted ${JsSigner.currentChain.name}")
            if (!redirect) {
                view.evaluateJavascript(jsInjectorClient.loadProviderJs(view.context), null)
                view.evaluateJavascript(jsInjectorClient.initJs(view.context, JsSigner.currentChain, JsSigner.address), null)
            }
            redirect = false
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?,
        ) {
            super.onPageFinished(view, url)
            if (!redirect && !loadingError) {
                onWebpageLoaded.invoke(view?.title, url)
            }
            onPageFinishedListener.onPageFinished()
            onFinished(url)
            redirect = false
            loadingError = false
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?,
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            onReceivedError(errorCode, description, failingUrl)
            loadingError = true
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
            Timber.e("${error?.toString()}")
        }

        override fun onPageCommitVisible(
            view: WebView?,
            url: String?,
        ) {
            super.onPageCommitVisible(view, url)
            onPageFinishedListener.onPageFinished()
        }

        private var lastHandleUrl: Pair<String, Long>? = null

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            redirect = true
            if (view == null || request == null) {
                return super.shouldOverrideUrlLoading(view, request)
            }
            val url = request.url.toString()

            if (url.startsWith(Constants.Scheme.WALLET_CONNECT_PREFIX, true) ||
                url.startsWith(Constants.Scheme.MIXIN_WC) ||
                url.startsWith(Constants.Scheme.HTTPS_MIXIN_WC)) {
                val wcUrl = convertWcLink(url)
                if (wcUrl != null) {
                    // handle wallet connect url
                    UrlInterpreterActivity.show(view.context, wcUrl)
                }
                // ignore wallet connect data url
                return true
            }

            if (url.isMixinUrl()) {
                if (url == lastHandleUrl?.first && System.currentTimeMillis() - (lastHandleUrl?.second ?: 0) <= 1000L) {
                    return true
                }
                lastHandleUrl = Pair<String, Long>(url, System.currentTimeMillis())
                val host = view.url?.run { Uri.parse(this).host }
                url.openAsUrl(
                    context,
                    fragmentManager,
                    scope,
                    host = host,
                    currentConversation = conversationId,
                ) {}
                return true
            }
            val extraHeaders = HashMap<String, String>()
            conversationId?.let {
                extraHeaders[Mixin_Conversation_ID_HEADER] = it
            }
            if (url.isWebUrl()) {
                view.loadUrl(url, extraHeaders)
                return true
            } else {
                try {
                    val context = view.context
                    // https://developer.android.com/training/package-visibility/use-cases#let-non-browser-apps-handle-urls
                    val intent =
                        Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                            action = ACTION_VIEW
                            addCategory(CATEGORY_BROWSABLE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                flags = FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                            }
                        }
                    if (context !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        val fallbackUrl = intent.extras?.getString("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl, extraHeaders)
                        } else {
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    if (e is ActivityNotFoundException) {
                        // do nothing
                    } else {
                        view.loadUrl(url, extraHeaders)
                    }
                }
            }
            return true
        }

        interface OnPageFinishedListener {
            fun onPageFinished()
        }
    }
    class Web3Interface(
        val onWalletActionSuccessful: (String) -> Unit,
        val onWalletActionError: (Long) -> Unit,
        val onBrowserSign: (JsSignMessage) -> Unit,
    ) {
        @JavascriptInterface
        fun postMessage(json: String) {
            Timber.e("postMessage $json")
            val obj = JSONObject(json)
            val id = obj.getLong("id")
            val method = DAppMethod.fromValue(obj.getString("name"))
            val network = obj.getString("network")
            when(method) {
                DAppMethod.REQUESTACCOUNTS -> {
                    onWalletActionSuccessful("window.$network.setAddress(\"${JsSigner.address}\");")
                    onWalletActionSuccessful("window.$network.sendResponse($id, [\"${JsSigner.address}\"]);")
                }

                DAppMethod.SWITCHETHEREUMCHAIN -> {
                    walletSwitchEthereumChain(id, obj.getJSONObject("object").toString())
                }

                DAppMethod.SIGNMESSAGE -> {
                    signMessage(id, obj.getJSONObject("object").toString())
                }

                DAppMethod.SIGNPERSONALMESSAGE -> {
                    signPersonalMessage(id, obj.getJSONObject("object"))
                }

                DAppMethod.SIGNTYPEDMESSAGE -> {
                    signTypedMessage(id, obj.getJSONObject("object").getString("raw"))
                }

                DAppMethod.SIGNTRANSACTION -> {
                    val transaction = obj.getJSONObject("object")
                    val to = transaction.getString("to")
                    val from = transaction.getString("from")
                    val gas = if (transaction.has("gas")) {
                        transaction.getString("gas")
                    } else {
                        null
                    }
                    val data = if (transaction.has("data")) {
                        transaction.getString("data")
                    } else {
                        null
                    }
                    val value = if (transaction.has("value")) {
                        transaction.getString("value")
                    } else {
                        "0x0"
                    }
                    val maxPriorityFeePerGas = if (transaction.has("maxPriorityFeePerGas")) {
                        transaction.getString("maxPriorityFeePerGas")
                    } else {
                        null
                    }
                    val maxFeePerGas = if (transaction.has("maxFeePerGas")) {
                        transaction.getString("maxFeePerGas")
                    } else {
                        null
                    }

                    signTransaction(id, WCEthereumTransaction(from, to, null, null, maxFeePerGas, maxPriorityFeePerGas, gas, null, value, data))
                }

                else -> {
                    Timber.e("json $json")
                }
            }
        }

        private fun signTransaction(
            callbackId: Long,
            wcEthereumTransaction: WCEthereumTransaction,
        ) {
            onBrowserSign(JsSignMessage(callbackId, JsSignMessage.TYPE_TRANSACTION, wcEthereumTransaction = wcEthereumTransaction))
        }

        private fun signMessage(callbackId: Long, data: String) {
            onBrowserSign(JsSignMessage(callbackId, JsSignMessage.TYPE_MESSAGE, data = data))
        }

        private fun signPersonalMessage(callbackId: Long, data:JSONObject) {
            try {
                val address = data.getString("address")
                if (!address.equals(JsSigner.address, true)) {
                    throw IllegalArgumentException("Address unequal")
                }
                onBrowserSign(JsSignMessage(callbackId, JsSignMessage.TYPE_PERSONAL_MESSAGE, data = data.getString("data")))
            } catch (e: Exception) {
                onWalletActionError(callbackId)
            }
        }

        private fun signTypedMessage(callbackId: Long, data: String) {
            onBrowserSign(JsSignMessage(callbackId, JsSignMessage.TYPE_TYPED_MESSAGE, data = data))
        }

        private fun ethCall(callbackId: Long, recipient: String) {
            // do nothing
            Timber.e("ethCall $callbackId $recipient")
        }

        private fun walletAddEthereumChain(callbackId: Long, msgParams: String) {
            Timber.e("walletAddEthereumChain $callbackId $msgParams")
        }

        private fun walletSwitchEthereumChain(callbackId: Long, msgParams: String) {
            val switchChain = GsonHelper.customGson.fromJson(msgParams, SwitchChain::class.java)
            val result = JsSigner.switchChain(switchChain)
            if (result.isSuccess) {
                onWalletActionSuccessful(
                    """
                    var config = {
                    ethereum: {
                        address: "${JsSigner.address}",
                        chainId: ${JsSigner.currentChain.chainReference},
                        rpcUrl: "${JsSigner.currentChain.rpcUrl}"
                    }
                };
                mixinwallet.${JsSigner.currentNetwork}.setConfig(config);
                """
                )
                onWalletActionSuccessful("window.${JsSigner.currentNetwork}.emitChainChanged('${JsSigner.currentChain.hexReference}');")
            }
            onWalletActionSuccessful("window.${JsSigner.currentNetwork}.sendResponse($callbackId, null);")
        }
    }

    class WebAppInterface(
        val context: Context,
        val conversationId: String?,
        val immersive: Boolean,
        var reloadThemeAction: (() -> Unit)? = null,
        var playlistAction: ((Array<String>) -> Unit)? = null,
        var closeAction: (() -> Unit)? = null,
        var getTipAddressAction: ((String, String) -> Unit)? = null,
        var tipSignAction: ((String, String, String) -> Unit)? = null,
        var getAssetAction: ((Array<String>, String) -> Unit)? = null,
    ) {
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun getContext(): String? =
            Gson().toJson(
                MixinContext(
                    conversationId,
                    immersive,
                    appearance =
                        if (context.isNightMode()) {
                            "dark"
                        } else {
                            "light"
                        },
                ),
            )

        @JavascriptInterface
        fun reloadTheme() {
            reloadThemeAction?.invoke()
        }

        @JavascriptInterface
        fun getAssets(
            list: Array<String>,
            callbackFunction: String,
        ) {
            getAssetAction?.invoke(list, callbackFunction)
        }

        @JavascriptInterface
        fun playlist(list: Array<String>) {
            playlistAction?.invoke(list)
        }

        @JavascriptInterface
        fun close() {
            closeAction?.invoke()
        }


        @JavascriptInterface
        fun getTipAddress(
            chainId: String,
            callbackFunction: String,
        ) {
            getTipAddressAction?.invoke(chainId, callbackFunction)
        }

        @JavascriptInterface
        fun tipSign(
            chainId: String,
            message: String,
            callbackFunction: String,
        ) {
            tipSignAction?.invoke(chainId, message, callbackFunction)
        }
    }

    class MixinContext(
        @SerializedName("conversation_id")
        val conversationId: String?,
        @SerializedName("immersive")
        val immersive: Boolean,
        @SerializedName("app_version")
        val appVersion: String = BuildConfig.VERSION_NAME,
        @SerializedName("appearance")
        val appearance: String,
        @SerializedName("platform")
        val platform: String = "Android",
        @SerializedName("currency")
        val currency: String = Session.getFiatCurrency(),
        @SerializedName("locale")
        val locale: String = "${getLanguage()}-${getCountry()}",
    )
}
