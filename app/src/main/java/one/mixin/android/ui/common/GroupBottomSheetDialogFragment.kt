package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.Mute.MUTE_1_HOUR
import one.mixin.android.Constants.Mute.MUTE_1_WEEK
import one.mixin.android.Constants.Mute.MUTE_1_YEAR
import one.mixin.android.Constants.Mute.MUTE_8_HOURS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentGroupBottomSheetBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.localTime
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.showConfirmDialog
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.info.MenuStyle
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menu
import one.mixin.android.ui.common.info.menuGroup
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.group.GroupActivity.Companion.ARGS_EXPAND
import one.mixin.android.ui.media.SharedMediaActivity
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.SearchMessageItem
import org.threeten.bp.Instant
import java.io.File

@AndroidEntryPoint
class GroupBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {

    companion object {
        const val TAG = "GroupBottomSheetDialogFragment"

        @SuppressLint("StaticFieldLeak")
        private var instant: GroupBottomSheetDialogFragment? = null
        fun newInstance(
            conversationId: String,
            expand: Boolean = false
        ): GroupBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return GroupBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_CONVERSATION_ID, conversationId)
                    putBoolean(ARGS_EXPAND, expand)
                }
                instant = this
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    var callback: Callback? = null

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }
    private lateinit var conversation: Conversation
    private var me: Participant? = null

    private var menuListLayout: ViewGroup? = null

    override fun getLayoutId() = R.layout.fragment_group_bottom_sheet

    private val binding by lazy {
        FragmentGroupBottomSheetBinding.bind(contentView)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        binding.title.rightIv.setOnClickListener { dismiss() }
        binding.memberFl.setOnClickListener {
            GroupActivity.show(requireContext(), GroupActivity.INFO, conversationId)
            dismiss()
        }
        binding.sendFl.setOnClickListener {
            if (conversationId != MixinApplication.conversationId) {
                ConversationActivity.showAndClear(requireContext(), conversationId)
            }
            dismiss()
        }
        setDetailsTv(binding.detailTv, binding.scrollView, conversationId)
        bottomViewModel.getConversationById(conversationId).observe(
            this,
            Observer { c ->
                if (c == null) return@Observer

                val changeMenu = menuListLayout == null ||
                    c.muteUntil != conversation.muteUntil
                conversation = c
                val icon = c.iconUrl
                binding.avatar.setGroup(icon)
                debugLongClick(
                    binding.avatar,
                    {
                        context?.getClipboardManager()
                            ?.setPrimaryClip(ClipData.newPlainText(null, conversationId))
                    }
                )
                if (icon == null || !File(icon).exists()) {
                    bottomViewModel.startGenerateAvatar(c.conversationId)
                }
                binding.name.text = c.name
                if (c.announcement.isNullOrBlank()) {
                    binding.detailTv.isVisible = false
                } else {
                    binding.detailTv.isVisible = true
                    binding.detailTv.text = c.announcement
                }
                initParticipant(changeMenu, c)
            }
        )

        contentView.post {
            binding.detailTv.maxHeight = requireContext().screenHeight() / 3
        }

        bottomViewModel.refreshConversation(conversationId)
    }

    @SuppressLint("SetTextI18n")
    private fun initParticipant(
        changeMenu: Boolean,
        conversation: Conversation
    ) = lifecycleScope.launch {
        if (!isAdded) return@launch

        var participantCount: Int
        var localMe: Participant?
        withContext(Dispatchers.IO) {
            localMe = bottomViewModel.findParticipantById(conversationId, Session.getAccountId()!!)
            participantCount = bottomViewModel.getParticipantsCount(conversationId)
        }
        if (!isAdded) return@launch

        binding.countTv.text = requireContext().resources.getQuantityString(R.plurals.group_participants_count, participantCount, participantCount)
        if (changeMenu || me != localMe) {
            lifecycleScope.launch {
                val circleNames = bottomViewModel.findCirclesNameByConversationId(conversationId)
                initMenu(localMe, circleNames)
            }
        }
        me = localMe

        if (!isAdded) return@launch

        if (me != null) {
            binding.opsLl.isVisible = true
            binding.scrollView.isEnabled = true
        } else {
            val withoutCode = conversation.status == ConversationStatus.QUIT.ordinal
            binding.scrollView.isEnabled = withoutCode
            binding.opsLl.isVisible = withoutCode
        }

        contentView.doOnPreDraw {
            behavior?.peekHeight = binding.title.height + binding.scrollContent.height -
                (menuListLayout?.height ?: 0) - if (menuListLayout != null) 38.dp else 8.dp
        }
    }

    private fun initMenu(me: Participant?, circleNames: List<String>) {
        if (!isAdded) return

        val list = menuList {
            menuGroup {
                menu {
                    title = getString(R.string.contact_other_shared_media)
                    action = {
                        SharedMediaActivity.show(requireContext(), conversationId)
                        dismiss()
                    }
                }
                menu {
                    title = getString(R.string.contact_other_search_conversation)
                    action = {
                        startSearchConversation()
                        dismiss()
                    }
                }
            }
        }

        if (me != null) {
            if (me.role == ParticipantRole.OWNER.name || me.role == ParticipantRole.ADMIN.name) {
                val announcementString = if (TextUtils.isEmpty(conversation.announcement)) {
                    getString(R.string.add_group_description)
                } else {
                    getString(R.string.group_info_edit)
                }
                list.groups.add(
                    menuGroup {
                        menu {
                            title = announcementString
                            action = {
                                editDialog {
                                    titleText = this@GroupBottomSheetDialogFragment.getString(R.string.group_info_edit)
                                    editText = conversation.announcement
                                    maxTextCount = 512
                                    editMaxLines = EditDialog.MAX_LINE.toInt()
                                    allowEmpty = false
                                    rightAction = {
                                        bottomViewModel.updateGroup(conversationId, announcement = it)
                                    }
                                }
                            }
                        }
                        menu {
                            title = getString(R.string.group_edit_name)
                            action = {
                                editDialog {
                                    titleText = this@GroupBottomSheetDialogFragment.getString(R.string.group_edit_name)
                                    editText = conversation.name
                                    maxTextCount = 40
                                    allowEmpty = false
                                    rightAction = {
                                        bottomViewModel.updateGroup(conversationId, it, null)
                                    }
                                }
                            }
                        }
                    }
                )
            }
            val muteMenu = if (conversation.muteUntil.notNullWithElse(
                    {
                        Instant.now().isBefore(Instant.parse(it))
                    },
                    false
                )
            ) {
                menu {
                    title = getString(R.string.Unmute)
                    subtitle = getString(R.string.mute_until, conversation.muteUntil?.localTime())
                    action = { unMute() }
                }
            } else {
                menu {
                    title = getString(R.string.Mute)
                    action = { mute() }
                }
            }
            list.groups.add(
                menuGroup {
                    menu(muteMenu)
                }
            )
        }

        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.Circles)
                    action = {
                        startCircleManager()
                        dismiss()
                    }
                    this.circleNames = circleNames
                }
            }
        )

        val deleteMenu = if (me != null) {
            menu {
                title = getString(R.string.group_info_exit_group)
                style = MenuStyle.Danger
                action = {
                    requireContext().showConfirmDialog(getString(R.string.group_info_exit_group)) {
                        bottomViewModel.exitGroup(conversationId)
                        dismiss()
                    }
                }
            }
        } else {
            menu {
                title = getString(R.string.group_info_delete_group)
                style = MenuStyle.Danger
                action = {
                    requireContext().showConfirmDialog(getString(R.string.group_info_delete_group)) {
                        bottomViewModel.deleteGroup(conversationId)
                        callback?.onDelete()
                    }
                }
            }
        }
        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.group_info_clear_chat)
                    style = MenuStyle.Danger
                    action = {
                        requireContext().showConfirmDialog(getString(R.string.group_info_clear_chat)) {
                            bottomViewModel.deleteMessageByConversationId(conversationId)
                            dismiss()
                        }
                    }
                }
                menu(deleteMenu)
            }
        )

        menuListLayout?.removeAllViews()
        binding.scrollContent.removeView(menuListLayout)
        list.createMenuLayout(requireContext())
            .let { layout ->
                menuListLayout = layout
                binding.scrollContent.addView(layout)
                layout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = requireContext().dpToPx(30f)
                }
                binding.moreFl.setOnClickListener {
                    if (behavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        binding.moreIv.rotationX = 180f
                    } else {
                        behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                        binding.scrollView.smoothScrollTo(0, 0)
                        binding.moreIv.rotationX = 0f
                    }
                }
            }
    }

    private fun startSearchConversation() = lifecycleScope.launch {
        bottomViewModel.getConversation(conversationId)?.let {
            val searchMessageItem = SearchMessageItem(
                it.conversationId,
                it.category,
                it.name,
                0,
                "",
                null,
                null,
                it.iconUrl
            )
            activity?.addFragment(
                this@GroupBottomSheetDialogFragment,
                SearchMessageFragment.newInstance(searchMessageItem, ""),
                SearchMessageFragment.TAG
            )
        }
    }

    private fun startCircleManager() {
        activity?.addFragment(
            this,
            CircleManagerFragment.newInstance(conversation.name, conversationId = conversation.conversationId),
            CircleManagerFragment.TAG
        )
    }

    private fun mute() {
        showMuteDialog()
    }

    private fun unMute() {
        val account = Session.getAccount()
        account?.let {
            lifecycleScope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        bottomViewModel.mute(0, conversationId = conversationId)
                    },
                    successBlock = { response ->
                        bottomViewModel.updateGroupMuteUntil(conversationId, response.data!!.muteUntil)
                        toast(getString(R.string.Unmute) + " ${conversation.name}")
                    }
                )
            }
        }
    }

    private fun showMuteDialog() {
        val choices = arrayOf(
            getString(R.string.one_hour),
            getString(R.string.eight_hours),
            getString(R.string.one_week),
            getString(R.string.one_year)
        )
        var duration = MUTE_1_HOUR
        var whichItem = 0
        alertDialogBuilder()
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                val account = Session.getAccount()
                account?.let {
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                bottomViewModel.mute(
                                    duration.toLong(),
                                    conversationId = conversationId
                                )
                            },
                            successBlock = { response ->
                                bottomViewModel.updateGroupMuteUntil(conversationId, response.data!!.muteUntil)
                                toast(getString(R.string.contact_mute_title) + " ${conversation.name} " + choices[whichItem])
                            }
                        )
                    }
                }
                dialog.dismiss()
            }
            .setSingleChoiceItems(choices, 0) { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = MUTE_1_HOUR
                    1 -> duration = MUTE_8_HOURS
                    2 -> duration = MUTE_1_WEEK
                    3 -> duration = MUTE_1_YEAR
                }
            }
            .show()
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        when (newState) {
            BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
            BottomSheetBehavior.STATE_COLLAPSED -> binding.moreIv.rotationX = 0f
            BottomSheetBehavior.STATE_EXPANDED -> binding.moreIv.rotationX = 180f
        }
    }

    interface Callback {
        fun onDelete()
    }
}
