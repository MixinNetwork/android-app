package one.mixin.android.ui.common

import android.app.Dialog
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_join_group_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.event.AvatarEvent
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment.Companion.CODE
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.widget.linktext.AutoLinkMode

@Parcelize
data class JoinGroupConversation(
    val conversationId: String,
    val name: String,
    val announcement: String,
    val participantsCount: Int,
    val iconUrl: String?
) : Parcelable

class JoinGroupBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {
    companion object {
        const val TAG = "JoinGroupBottomSheetDialogFragment"
        private const val ARGS_JOIN_GROUP_CONVERSATION = "args_join_group_conversation"

        fun newInstance(
            joinGroupConversation: JoinGroupConversation,
            code: String
        ) = JoinGroupBottomSheetDialogFragment().apply {
            arguments = bundleOf(
                ARGS_JOIN_GROUP_CONVERSATION to joinGroupConversation,
                CODE to code
            )
        }
    }

    private val c: JoinGroupConversation by lazy {
        requireArguments().getParcelable<JoinGroupConversation>(ARGS_JOIN_GROUP_CONVERSATION)!!
    }
    private val code: String by lazy { requireArguments().getString(CODE)!! }

    override fun getLayoutId() = R.layout.fragment_join_group_bottom_sheet

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        behavior?.isDraggable = false
        contentView.title.right_iv.setOnClickListener { dismiss() }
        contentView.join_tv.setOnClickListener {
            contentView.join_tv?.visibility = View.INVISIBLE
            contentView.join_progress?.visibility = View.VISIBLE
            bottomViewModel.join(code).autoDispose(stopScope).subscribe({
                contentView.join_tv?.visibility = View.VISIBLE
                contentView.join_progress?.visibility = View.GONE
                if (it.isSuccess) {
                    val conversationResponse = it.data as ConversationResponse
                    val accountId = Session.getAccountId()
                    val result = conversationResponse.participants.any { participant ->
                        participant.userId == accountId
                    }
                    if (result) {
                        bottomViewModel.refreshConversation(c.conversationId)
                        ConversationActivity.show(requireContext(), c.conversationId)
                    }
                } else {
                    ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                }
            }, {
                contentView.join_tv?.visibility = View.VISIBLE
                contentView.join_progress?.visibility = View.GONE
                ErrorHandler.handleError(it)
            })
        }
        contentView.detail_tv.movementMethod = LinkMovementMethod()
        contentView.detail_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        contentView.detail_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        contentView.detail_tv.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(c.conversationId, parentFragmentManager, lifecycleScope)
            dismiss()
        }
        contentView.post {
            contentView.detail_tv.maxHeight = requireContext().screenHeight() / 3
        }

        RxBus.listen(AvatarEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe {
                if (it.conversationId == c.conversationId) {
                    contentView.avatar.setGroup(it.url)
                }
            }

        loadConversation()
    }

    private fun loadConversation() = lifecycleScope.launch {
        if (!isAdded) return@launch

        contentView.name.text = c.name
        if (c.announcement.isBlank()) {
            contentView.detail_tv.isVisible = false
        } else {
            contentView.detail_tv.isVisible = true
            contentView.detail_tv.text = c.announcement
        }
        contentView.count_tv.text = getString(R.string.group_participants_count, c.participantsCount)
        c.iconUrl?.let { contentView.avatar.setGroup(it) }
        contentView.join_tv.isVisible = true

        contentView.doOnPreDraw {
            behavior?.peekHeight = contentView.title.height + contentView.scroll_content.height
        }
    }
}
