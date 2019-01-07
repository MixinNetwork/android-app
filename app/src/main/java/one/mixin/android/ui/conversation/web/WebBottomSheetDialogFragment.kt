package one.mixin.android.ui.conversation.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_web.view.*
import one.mixin.android.Constants.Mixin_Conversation_ID_HEADER
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.getPublicPictyresPath
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.WebAppInterface
import one.mixin.android.ui.panel.PanelBottomSheet
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.ui.url.openUrl
import one.mixin.android.util.KeyBoardAssist
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DragWebView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.FileInputStream
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

class WebBottomSheetDialogFragment : PanelBottomSheet() {

    companion object {
        const val TAG = "WebBottomSheetDialogFragment"

        private const val FILE_CHOOSER = 0x01

        private const val CONTEXT_MENU_ID_SCAN_IMAGE = 0x11
        private const val CONTEXT_MENU_ID_SAVE_IMAGE = 0x12

        const val URL = "url"
        const val CONVERSATION_ID = "conversation_id"
        private const val NAME = "name"
        fun newInstance(url: String, conversationId: String?, name: String? = null) =
            WebBottomSheetDialogFragment().withArgs {
                putString(URL, url)
                putString(CONVERSATION_ID, conversationId)
                putString(NAME, name)
            }
    }

    private val url: String by lazy {
        arguments!!.getString(URL)
    }
    private val conversationId: String? by lazy {
        arguments!!.getString(CONVERSATION_ID)
    }

    override fun getContentViewId() = R.layout.fragment_web

    @SuppressLint("RestrictedApi", "SetJavaScriptEnabled")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        registerForContextMenu(contentView.chat_web_view)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        contentView.chat_web_view.hitTestResult?.let {
            when (it.type) {
                WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    menu.add(0, CONTEXT_MENU_ID_SCAN_IMAGE, 0, R.string.contact_sq_scan_title)
                    menu.getItem(0).setOnMenuItemClickListener { menu ->
                        onContextItemSelected(menu)
                        return@setOnMenuItemClickListener true
                    }
                    menu.add(0, CONTEXT_MENU_ID_SAVE_IMAGE, 1, R.string.contact_save_image)
                    menu.getItem(1).setOnMenuItemClickListener { menu ->
                        onContextItemSelected(menu)
                        return@setOnMenuItemClickListener true
                    }
                }
                else -> Timber.d("App does not yet handle target type: ${it.type}")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        contentView.chat_web_view.hitTestResult?.let { it ->
            val url = it.extra
            if (item.itemId == CONTEXT_MENU_ID_SCAN_IMAGE) {
                doAsync {
                    try {
                        val bitmap = Glide.with(requireContext())
                            .asBitmap()
                            .load(url)
                            .submit()
                            .get(10, TimeUnit.SECONDS)
                        val result = bitmap.decodeQR()
                        uiThread {
                            if (isDetached) {
                                return@uiThread
                            }
                            if (result != null) {
                                openUrl(result, requireFragmentManager()) {
                                    QrScanBottomSheetDialogFragment.newInstance(result)
                                        .showNow(requireFragmentManager(), QrScanBottomSheetDialogFragment.TAG)
                                }
                            } else {
                                if (isAdded) toast(R.string.can_not_recognize)
                            }
                        }
                    } catch (e: Exception) {
                        if (isAdded) toast(R.string.can_not_recognize)
                    }
                }
                return true
            } else if (item.itemId == CONTEXT_MENU_ID_SAVE_IMAGE) {
                saveImageFromUrl(url)
            }
        }
        return super.onContextItemSelected(item)
    }

    private val miniHeight by lazy {
        context!!.displaySize().y * 3 / 4
    }

    var uploadMessage: ValueCallback<Array<Uri>>? = null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        KeyBoardAssist.assistContent(contentView as ViewGroup)
        contentView.chat_web_view.settings.javaScriptEnabled = true
        contentView.chat_web_view.settings.domStorageEnabled = true

        contentView.chat_web_view.addJavascriptInterface(WebAppInterface(context!!, conversationId), "MixinContext")
        contentView.chat_web_view.webViewClient = WebViewClientImpl(object : WebViewClientImpl.OnPageFinishedListener {
            override fun onPageFinished() {}
        }, conversationId, this.requireFragmentManager())

        contentView.chat_web_view.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = filePathCallback
                val intent: Intent? = fileChooserParams?.createIntent()
                try {
                    startActivityForResult(intent, FILE_CHOOSER)
                } catch (e: ActivityNotFoundException) {
                    uploadMessage = null
                    toast(R.string.error_file_chooser)
                    return false
                }
                return true
            }
        }

        contentView.chat_web_view.setOnScrollListener(object : DragWebView.OnDragListener {
            override fun onUp() {
                ((dialog as BottomSheet).getCustomView())?.let {
                    val height = it.layoutParams.height
                    when {
                        height < closeHeight -> {
                            (dialog as BottomSheet).setCustomViewHeight(0) {
                                dismiss()
                            }
                        }
                        height < middleHeight -> {
                            (dialog as BottomSheet).setCustomViewHeight(miniHeight)
                        }
                        else -> {
                            (dialog as BottomSheet).setCustomViewHeight(maxHeight)
                        }
                    }
                }
            }

            override fun onScroll(disY: Float): Boolean {
                return notNullElse((dialog as BottomSheet).getCustomView(), {
                    val height = it.layoutParams.height - disY.toInt()
                    return if (height in 0..maxHeight) {
                        (dialog as BottomSheet).setCustomViewHeightSync(height)
                        true
                    } else {
                        false
                    }
                }, false)
            }
        })

        dialog.setOnShowListener {
            val extraHeaders = HashMap<String, String>()
            conversationId?.let {
                extraHeaders[Mixin_Conversation_ID_HEADER] = it
            }
            contentView.chat_web_view.loadUrl(url, extraHeaders)
        }
        dialog.setOnDismissListener {
            contentView.hideKeyboard()
            contentView.chat_web_view.stopLoading()
            contentView.chat_web_view.webViewClient = null
            contentView.chat_web_view.webChromeClient = null
            dismiss()
        }
        (dialog as BottomSheet).setCustomViewHeight(maxHeight)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER) {
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            uploadMessage = null
        }
    }

    override fun onDestroyView() {
        contentView.chat_web_view.stopLoading()
        contentView.chat_web_view.webViewClient = null
        contentView.chat_web_view.webChromeClient = null
        unregisterForContextMenu(contentView.chat_web_view)
        super.onDestroyView()
    }

    @SuppressLint("CheckResult")
    private fun saveImageFromUrl(url: String?) {
        if (isAdded) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        doAsync {
                            val file = Glide.with(MixinApplication.appContext)
                                .asFile()
                                .load(url)
                                .submit()
                                .get(10, TimeUnit.SECONDS)
                            val outFile = requireContext().getPublicPictyresPath().createImageTemp(noMedia = false)
                            outFile.copyFromInputStream(FileInputStream(file))
                            requireContext().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
                            uiThread { toast(R.string.save_success) }
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
    }

    class WebViewClientImpl(
        private val onPageFinishedListener: OnPageFinishedListener,
        val conversationId: String?,
        private val fragmentManager: FragmentManager
    ) : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinishedListener.onPageFinished()
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (view == null || url == null) {
                return false
            }
            if (isMixinUrl(url)) {
                openUrl(url, fragmentManager, {})
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
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    if (intent != null) {
                        view.stopLoading()

                        val packageManager = context.packageManager
                        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        if (info != null) {
                            context.startActivity(intent)
                        } else {
                            view.loadUrl(url, extraHeaders)
                        }
                    }
                } catch (e: URISyntaxException) {
                    view.loadUrl(url, extraHeaders)
                }
            }

            return true
        }

        interface OnPageFinishedListener {
            fun onPageFinished()
        }
    }
}