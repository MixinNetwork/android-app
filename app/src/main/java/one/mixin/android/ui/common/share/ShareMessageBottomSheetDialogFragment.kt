package one.mixin.android.ui.common.share

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentShareMessageBottomSheetBinding
import one.mixin.android.extension.isNightMode
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
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

@AndroidEntryPoint
class ShareMessageBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ShareMessageBottomSheetDialogFragment"
        const val SHARE_MESSAGE = "share_message"

        private const val CONVERSATION_ID = "conversation_id"
        private const val APP = "app"
        private const val HOST = "host"
        fun newInstance(shareMessage: ForwardMessage, conversationId: String?, app: App? = null, host: String? = null) =
            ShareMessageBottomSheetDialogFragment().withArgs {
                putString(CONVERSATION_ID, conversationId)
                putParcelable(SHARE_MESSAGE, shareMessage)
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

    private val shareMessage: ForwardMessage by lazy {
        requireNotNull(arguments?.getParcelable(SHARE_MESSAGE)) {
            "error data"
        }
    }

    private val binding by viewBinding(FragmentShareMessageBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "StringFormatInvalid")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.close.setOnClickListener {
            dismiss()
        }
        try {
            loadData()
        } catch (e: Exception) {
            Timber.e("Load \"${shareMessage.content}\" ERROR!!!")
            toast(getString(R.string.error_unknown_with_message, "${e.javaClass.name} ${shareMessage.content}"))
            dismiss()
            return
        }
        when {
            app != null -> {
                binding.shareTitle.text = getString(R.string.share_message_description, "${app?.name}(${app?.appNumber})", getMessageCategory())
            }
            host != null -> {
                binding.shareTitle.text = getString(R.string.share_message_description, host, getMessageCategory())
            }
            else -> {
                binding.shareTitle.text = getString(R.string.share_message_description_empty, getMessageCategory())
            }
        }
        binding.send.setOnClickListener {
            if (shareMessage.category == ShareCategory.Image) {
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
            } else if (shareMessage.category == ShareCategory.AppCard) {
                val appCardData = GsonHelper.customGson.fromJson(shareMessage.content, AppCardData::class.java)
                if (appCardData.title.length in 1..36 && appCardData.description.length in 1..128) {
                    sendMessage()
                } else {
                    toast(R.string.Data_error)
                }
            } else {
                sendMessage()
            }
        }
    }

    private fun getMessageCategory(): String {
        return when (shareMessage.category) {
            ShareCategory.Text -> {
                getString(R.string.message)
            }
            ShareCategory.Image -> {
                getString(R.string.Photo)
            }
            ShareCategory.Contact -> {
                getString(R.string.Contact)
            }
            ShareCategory.Post -> {
                getString(R.string.Post)
            }
            ShareCategory.AppCard -> {
                getString(R.string.Card)
            }
            ShareCategory.Live -> {
                getString(R.string.Live)
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun sendMessage() {
        ForwardActivity.show(requireContext(), arrayListOf(shareMessage), ForwardAction.App.Resultful(conversationId, getString(R.string.Send)))
        dismiss()
    }

    private fun loadData() {
        val content = shareMessage.content
        when (shareMessage.category) {
            ShareCategory.Text -> {
                loadText(content)
            }
            ShareCategory.Image -> {
                loadImage(content)
            }
            ShareCategory.Contact -> {
                loadContact(content)
            }
            ShareCategory.Post -> {
                loadPost(content)
            }
            ShareCategory.AppCard -> {
                loadAppCard(content)
            }
            ShareCategory.Live -> {
                loadLive(content)
            }
        }
    }

    private fun loadText(content: String) {
        val renderer = ShareTextRenderer(requireContext())
        binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(content, requireContext().isNightMode())
    }

    private fun loadImage(content: String) {
        val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
        val renderer = ShareImageRenderer(requireContext())
        binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(shareImageData)
    }

    private fun loadContact(content: String) {
        lifecycleScope.launch {
            val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
            binding.progress.isVisible = true
            val user = viewModel.refreshUser(contactData.userId)
            if (user == null) {
                toast(R.string.error_not_found)
                return@launch
            }
            val renderer = ShareContactRenderer(requireContext())
            binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
            renderer.render(user, requireContext().isNightMode())
            binding.progress.isVisible = false
        }
    }

    private fun loadPost(content: String) {
        val renderer = SharePostRenderer(requireActivity())
        binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(content, requireContext().isNightMode())
    }

    private fun loadAppCard(content: String) {
        val appCardData = GsonHelper.customGson.fromJson(content, AppCardData::class.java)
        val renderer = ShareAppCardRenderer(requireContext())
        binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
        renderer.render(appCardData, requireContext().isNightMode())
    }

    private fun loadLive(content: String) {
        val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
        if (liveData.width <= 0 || liveData.height <= 0) {
            toast(getString(R.string.error_unknown_with_message, "Illegal size"))
            dismiss()
        }
        val renderer = ShareLiveRenderer(requireContext())
        binding.contentLayout.addView(renderer.contentView, generateLayoutParams())
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
