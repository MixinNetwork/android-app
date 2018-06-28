package one.mixin.android.ui.conversation.web

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.fragment_web.view.*
import kotlinx.android.synthetic.main.view_web_bottom.view.*
import kotlinx.coroutines.experimental.timeunit.TimeUnit
import one.mixin.android.Constants.Mixin_Conversation_ID_HEADER
import one.mixin.android.R
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.ui.url.openUrl
import one.mixin.android.util.KeyBoardAssist
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DragWebView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.net.URISyntaxException

class WebBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "WebBottomSheetDialogFragment"

        private const val FILE_CHOOSER = 0x01

        private const val CONTEXT_MENU_ID_SCAN_IMAGE = 0x11

        private const val URL = "url"
        private const val CONVERSATION_ID = "conversation_id"
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
    private val name: String? by lazy {
        arguments!!.getString(NAME)
    }

    @SuppressLint("RestrictedApi", "SetJavaScriptEnabled")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_web, null)
        registerForContextMenu(contentView.chat_web_view)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        contentView.chat_web_view.hitTestResult?.let {
            when (it.type) {
                WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    menu.add(0, CONTEXT_MENU_ID_SCAN_IMAGE, 0, R.string.contact_sq_scan_title)
                    menu.getItem(0).setOnMenuItemClickListener {
                        onContextItemSelected(it)
                        return@setOnMenuItemClickListener true
                    }
                }
                else -> Timber.d("App does not yet handle target type: ${it.type}")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        contentView.chat_web_view.hitTestResult?.let {
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
                                        .show(requireFragmentManager(), QrScanBottomSheetDialogFragment.TAG)
                                }
                            } else {
                                toast(R.string.can_not_recognize)
                            }
                        }
                    } catch (e: Exception) {
                        toast(R.string.can_not_recognize)
                    }
                }
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private val miniHeight by lazy {
        context!!.displaySize().y * 3 / 4
    }

    private val closeHeight by lazy {
        context!!.displaySize().y / 2
    }

    private val maxHeight by lazy {
        context!!.displaySize().y - context!!.statusBarHeight()
    }

    private val middleHeight by lazy {
        (miniHeight + maxHeight) / 2
    }

    var uploadMessage: ValueCallback<Array<Uri>>? = null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        KeyBoardAssist.assistContent(contentView as ViewGroup)
        contentView.close_iv.setOnClickListener {
            dialog.dismiss()
        }
        contentView.chat_web_view.settings.javaScriptEnabled = true
        contentView.chat_web_view.settings.domStorageEnabled = true

        contentView.chat_web_view.addJavascriptInterface(WebAppInterface(context!!, conversationId), "MixinContext")
        contentView.chat_web_view.webViewClient = WebViewClientImpl(object : WebViewClientImpl.OnPageFinishedListener {
            override fun onPageFinished() {
                contentView.progress.visibility = View.GONE
                contentView.title_view.visibility = View.VISIBLE
            }
        }, conversationId, this.requireFragmentManager())

        contentView.chat_web_view.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.equals(url)) {
                    contentView.title_view.text = title
                }
            }

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

        contentView.more_iv.setOnClickListener {
            showBottomSheet()
        }

        name?.let {
            contentView.title_view.text = it
            contentView.progress.visibility = View.GONE
            contentView.title_view.visibility = View.VISIBLE
        }

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
        (dialog as BottomSheet).setCustomViewHeight(miniHeight)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER) {
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            uploadMessage = null
        }
    }

    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(context!!)
        val view = LayoutInflater.from(context).inflate(R.layout.view_web_bottom, null, false)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.refresh.setOnClickListener {
            contentView.chat_web_view.clearCache(true)
            contentView.chat_web_view.reload()
            bottomSheet.dismiss()
        }
        view.open.setOnClickListener {
            context?.openUrl(contentView.chat_web_view.url)
            bottomSheet.dismiss()
        }
        view.cancel_tv.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }

    class WebViewClientImpl(
        private val onPageFinishedListener: OnPageFinishedListener,
        val conversationId: String?,
        val fragmentManager: FragmentManager
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
                            // or call external broswer
                            //                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                            //                    context.startActivity(browserIntent);
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

    class WebAppInterface(val context: Context, val conversationId: String?) {
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun getContext(): String? {
            return if (conversationId != null) {
                Gson().toJson(MixinContext(conversationId))
            } else {
                null
            }
        }
    }

    class MixinContext(
        @SerializedName("conversation_id")
        val conversationId: String?
    )
}