package one.mixin.android.ui.common.share

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_share_message_bottom_sheet.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.share.renderer.ShareAppButtonGroupRenderer
import one.mixin.android.ui.common.share.renderer.ShareAppCardRenderer
import one.mixin.android.ui.common.share.renderer.ShareContactRenderer
import one.mixin.android.ui.common.share.renderer.ShareImageRenderer
import one.mixin.android.ui.common.share.renderer.ShareLiveRenderer
import one.mixin.android.ui.common.share.renderer.SharePostRenderer
import one.mixin.android.ui.common.share.renderer.ShareTextRenderer
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ShareImageData
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.widget.BottomSheet

class ShareMessageBottomSheetDialogFragment : MixinBottomSheetDialogFragment(), Injectable {

    companion object {
        const val TAG = "ShareMessageBottomSheetDialogFragment"
        private const val CATEGORY = "category"
        private const val CONVERSATION_ID = "conversation_id"
        private const val CONTENT = "content"
        fun newInstance(category: String, conversationId: String?, content: String) = ShareMessageBottomSheetDialogFragment().withArgs {
            putString(CATEGORY, category)
            putString(CONVERSATION_ID, conversationId)
            putString(CONTENT, content)
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel: BottomSheetViewModel by viewModels { viewModelFactory }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_share_message_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
        contentView.close.setOnClickListener {
            dismiss()
        }
        loadData()
    }

    private fun loadData() {
        val content = arguments?.getString(CONTENT)
        val category = arguments?.getString(CATEGORY)
        if (content == null || category == null) {
            toast(R.string.error_data)
            dismiss()
            return
        }
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
            Constants.ShareCategory.APP_BUTTON_GROUP -> {
                loadAppButtonGroup(content)
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
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(content)
    }

    private fun loadImage(content: String) {
        val shareImageData = GsonHelper.customGson.fromJson(content, ShareImageData::class.java)
        val renderer = ShareImageRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(shareImageData)
    }

    private fun loadContact(content: String) {
        val contactData = GsonHelper.customGson.fromJson(content, ContactMessagePayload::class.java)
        val renderer = ShareContactRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(contactData)
    }

    private fun loadPost(content: String) {
        val renderer = SharePostRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(content)
    }

    private fun loadAppButtonGroup(content: String) {
        val appButton = GsonHelper.customGson.fromJson(content, Array<AppButtonData>::class.java)
        for (item in appButton) {
            ColorUtil.parseColor(item.color.trim())
        }
        val renderer = ShareAppButtonGroupRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(appButton)
    }

    private fun loadAppCard(content: String) {
        val appCardData = GsonHelper.customGson.fromJson(content, AppCardData::class.java)
        val renderer = ShareAppCardRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(appCardData)
    }

    private fun loadLive(content: String) {
        val liveData = GsonHelper.customGson.fromJson(content, LiveMessagePayload::class.java)
        if (liveData.width <= 0 || liveData.height <= 0) {
            toast(R.string.error_data)
            dismiss()
        }
        val renderer = ShareLiveRenderer(requireContext())
        contentView.content_layout.addView(renderer.contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        renderer.render(liveData)
    }
}