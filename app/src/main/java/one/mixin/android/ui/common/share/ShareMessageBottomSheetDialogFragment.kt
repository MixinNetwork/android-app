package one.mixin.android.ui.common.share

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_share_message_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.share.renderer.ShareAppCardRenderer
import one.mixin.android.ui.common.share.renderer.ShareContactRenderer
import one.mixin.android.ui.common.share.renderer.ShareImageRenderer
import one.mixin.android.ui.common.share.renderer.ShareLiveRenderer
import one.mixin.android.ui.common.share.renderer.SharePostRenderer
import one.mixin.android.ui.common.share.renderer.ShareTextRenderer
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ShareImageData
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.widget.BottomSheet
import java.io.File

@AndroidEntryPoint
class ShareMessageBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ShareMessageBottomSheetDialogFragment"
        const val CATEGORY = "category"
        const val CONTENT = "content"

        private const val CONVERSATION_ID = "conversation_id"
        private const val APP = "app"
        private const val HOST = "host"
        fun newInstance(category: String, content: String, conversationId: String?, app: App? = null, host: String? = null) =
            ShareMessageBottomSheetDialogFragment().withArgs {
                putString(CATEGORY, category)
                putString(CONVERSATION_ID, conversationId)
                putString(CONTENT, content)
                putParcelable(APP, app)
                putString(HOST, host)
            }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<BottomSheetViewModel>()

    private val app by lazy {
        arguments?.getParcelable<App>(APP)
    }

    private val host by lazy {
        arguments?.getString(HOST)
    }

    private val conversationId by lazy {
        arguments?.getString(CONVERSATION_ID)
    }

    private val content by lazy {
        requireNotNull(arguments?.getString(CONTENT)) {
            "error data"
        }
    }

    private val category by lazy {
        requireNotNull(arguments?.getString(CATEGORY)) {
            "error data"
        }
    }

    @SuppressLint("RestrictedApi", "StringFormatInvalid")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_share_message_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        contentView.close.setOnClickListener {
            dismiss()
        }
        loadData()
        when {
            app != null -> {
                contentView.share_title.text = getString(R.string.share_message_description, "${app?.name}(${app?.appNumber})", getMessageCategory())
            }
            host != null -> {
                contentView.share_title.text = getString(R.string.share_message_description, host, getMessageCategory())
            }
            else -> {
                contentView.share_title.text = getString(R.string.share_message_description_empty, getMessageCategory())
            }
        }
        contentView.send.setOnClickListener {
            if (category == Constants.ShareCategory.IMAGE) {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                sendMessage()
                            } else {
                                context?.openPermissionSetting()
                            }
                        },
                        {
                        }
                    )
            } else {
                sendMessage()
            }
        }
    }

    private fun getMessageCategory(): String {
        return when (category) {
            Constants.ShareCategory.TEXT -> {
                getString(R.string.message)
            }
            Constants.ShareCategory.IMAGE -> {
                getString(R.string.photo)
            }
            Constants.ShareCategory.CONTACT -> {
                getString(R.string.contact)
            }
            Constants.ShareCategory.POST -> {
                getString(R.string.post)
            }
            Constants.ShareCategory.APP_CARD -> {
                getString(R.string.card)
            }
            else -> {
                getString(R.string.live)
            }
        }
    }

    private val getScanResult = registerForActivityResult(ForwardActivity.ForwardContract()) { data ->
        data?.getParcelableArrayListExtra<SelectItem>(ARGS_RESULT)?.forEach { item ->
            sendMessage(item)
        }
    }

    private fun sendMessage(selectItem: SelectItem) {
        lifecycleScope.launch {
            val sender = Session.getAccount()?.toUser() ?: return@launch
            viewModel.checkData(selectItem) { conversationId: String, isPlain: Boolean ->
                when (category) {
                    Constants.ShareCategory.TEXT -> {
                        viewModel.sendTextMessage(conversationId, sender, content, isPlain)
                        toast(R.string.send_success)
                        dismiss()
                    }
                    Constants.ShareCategory.IMAGE -> {
                        withContext(Dispatchers.IO) {
                            val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
                            val file: File = Glide.with(requireContext()).asFile().load(shareImageData.url).submit().get()
                            viewModel.sendImageMessage(conversationId, sender, file.toUri(), isPlain)
                        }?.autoDispose(stopScope)?.subscribe(
                            {
                                when (it) {
                                    0 -> {
                                        toast(R.string.send_success)
                                        dismiss()
                                    }
                                    -1 -> context?.toast(R.string.error_image)
                                    -2 -> context?.toast(R.string.error_format)
                                }
                            },
                            {
                                context?.toast(R.string.error_image)
                            }
                        )
                    }
                    Constants.ShareCategory.CONTACT -> {
                        val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
                        viewModel.sendContactMessage(conversationId, sender, contactData.userId, isPlain)
                        toast(R.string.send_success)
                        dismiss()
                    }
                    Constants.ShareCategory.POST -> {
                        viewModel.sendPostMessage(conversationId, sender, content, isPlain)
                        toast(R.string.send_success)
                        dismiss()
                    }
                    Constants.ShareCategory.APP_CARD -> {
                        viewModel.sendAppCardMessage(conversationId, sender, content)
                        toast(R.string.send_success)
                        dismiss()
                    }
                    Constants.ShareCategory.LIVE -> {
                        val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
                        viewModel.sendLiveMessage(conversationId, sender, liveData, isPlain)
                        toast(R.string.send_success)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        conversationId.notNullWithElse(
            {
                sendMessage(SelectItem(it, null))
            },
            {
                if (requireActivity() is UrlInterpreterActivity) {
                    ForwardActivity.send(requireContext(), category, content)
                    dismiss()
                } else {
                    getScanResult.launch(null)
                }
            }
        )
    }

    private fun loadData() {
        when (category) {
            Constants.ShareCategory.TEXT -> {
                loadText(content)
            }
            Constants.ShareCategory.IMAGE -> {
                loadImage(content)
            }
            Constants.ShareCategory.CONTACT -> {
                loadContact(content)
            }
            Constants.ShareCategory.POST -> {
                loadPost(content)
            }
            Constants.ShareCategory.APP_CARD -> {
                loadAppCard(content)
            }
            Constants.ShareCategory.LIVE -> {
                loadLive(content)
            }
        }
    }

    private fun loadText(content: String) {
        val renderer = ShareTextRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(content, requireContext().isNightMode())
    }

    private fun loadImage(content: String) {
        val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
        val renderer = ShareImageRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(shareImageData)
    }

    private fun loadContact(content: String) {
        lifecycleScope.launch {
            val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
            contentView.progress.isVisible = true
            val user = viewModel.refreshUser(contactData.userId)
            if (user == null) {
                toast(R.string.error_not_found)
                return@launch
            }
            val renderer = ShareContactRenderer(requireContext())
            contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
            renderer.render(user, requireContext().isNightMode())
            contentView.progress.isVisible = false
        }
    }

    private fun loadPost(content: String) {
        val renderer = SharePostRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(content, requireContext().isNightMode())
    }

    private fun loadAppCard(content: String) {
        val appCardData = GsonHelper.customGson.fromJson(content, AppCardData::class.java)
        val renderer = ShareAppCardRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(appCardData, requireContext().isNightMode())
    }

    private fun loadLive(content: String) {
        val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
        if (liveData.width <= 0 || liveData.height <= 0) {
            toast(R.string.error_data)
            dismiss()
        }
        val renderer = ShareLiveRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(liveData)
    }

    private fun generateLayoutParams(): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }
}
