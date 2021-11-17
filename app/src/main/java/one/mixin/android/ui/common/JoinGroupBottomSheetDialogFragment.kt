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
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.databinding.FragmentJoinGroupBottomSheetBinding
import one.mixin.android.event.AvatarEvent
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.screenHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment.Companion.CODE
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.linktext.AutoLinkMode

@Parcelize
data class JoinGroupConversation(
    val conversationId: String,
    val name: String,
    val announcement: String,
    val participantsCount: Int,
    val iconUrl: String?
) : Parcelable

@AndroidEntryPoint
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
        requireArguments().getParcelable(ARGS_JOIN_GROUP_CONVERSATION)!!
    }
    private val code: String by lazy { requireArguments().getString(CODE)!! }

    override fun getLayoutId() = R.layout.fragment_join_group_bottom_sheet

    private val binding by lazy {
        FragmentJoinGroupBottomSheetBinding.bind(contentView)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        behavior?.isDraggable = false
        binding.title.rightIv.setOnClickListener { dismiss() }
        binding.joinTv.setOnClickListener {
            binding.joinTv.visibility = View.INVISIBLE
            binding.joinProgress.visibility = View.VISIBLE
            bottomViewModel.join(code).autoDispose(stopScope).subscribe(
                {
                    binding.joinTv.visibility = View.VISIBLE
                    binding.joinProgress.visibility = View.GONE
                    if (it.isSuccess) {
                        val conversationResponse = it.data as ConversationResponse
                        val accountId = Session.getAccountId()
                        val result = conversationResponse.participants.any { participant ->
                            participant.userId == accountId
                        }
                        if (result) {
                            bottomViewModel.refreshConversation(c.conversationId)
                            ConversationActivity.showAndClear(requireContext(), c.conversationId)
                        }
                    } else {
                        ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                    }
                },
                {
                    binding.joinTv.visibility = View.VISIBLE
                    binding.joinProgress.visibility = View.GONE
                    ErrorHandler.handleError(it)
                }
            )
        }
        binding.detailTv.movementMethod = LinkMovementMethod()
        binding.detailTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        binding.detailTv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        binding.detailTv.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(requireContext(), c.conversationId, parentFragmentManager, lifecycleScope)
            dismiss()
        }
        contentView.post {
            binding.detailTv.maxHeight = requireContext().screenHeight() / 3
        }

        RxBus.listen(AvatarEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe {
                if (it.conversationId == c.conversationId) {
                    binding.avatar.setGroup(it.url)
                }
            }

        loadConversation()
    }

    private fun loadConversation() = lifecycleScope.launch {
        if (!isAdded) return@launch

        binding.name.text = c.name
        if (c.announcement.isBlank()) {
            binding.detailTv.isVisible = false
        } else {
            binding.detailTv.isVisible = true
            binding.detailTv.text = c.announcement
        }
        binding.countTv.text =
            getString(R.string.group_participants_count, c.participantsCount)
        c.iconUrl?.let { binding.avatar.setGroup(it) }
        binding.joinTv.isVisible = true

        contentView.doOnPreDraw {
            behavior?.peekHeight = binding.title.height + binding.scrollContent.height
        }
    }
}
