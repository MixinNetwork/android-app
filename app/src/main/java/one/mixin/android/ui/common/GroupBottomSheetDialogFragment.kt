package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.uber.autodispose.autoDisposable
import kotlinx.android.synthetic.main.fragment_group_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment.Companion.CODE
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.group.GroupActivity.Companion.ARGS_EXPAND
import one.mixin.android.ui.group.GroupEditFragment
import one.mixin.android.ui.group.GroupFragment.Companion.ARGS_CONVERSATION_ID
import one.mixin.android.ui.url.openUrlWithExtraWeb
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.widget.AndroidUtilities.dp
import one.mixin.android.widget.AvatarView
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dimen
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.margin
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import org.threeten.bp.Instant
import java.io.File

class GroupBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ProfileBottomSheetDialogFragment"
        private const val DEFAULT_COUNT = 5

        private const val POS_TV = 0
        private const val POS_PB = 1

        fun newInstance(conversationId: String, code: String? = null, expand: Boolean = false) = GroupBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_CONVERSATION_ID, conversationId)
                putString(CODE, code)
                putBoolean(ARGS_EXPAND, expand)
            }
        }
    }

    private var menu: AlertDialog? = null
    var callback: Callback? = null

    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_CONVERSATION_ID)
    }
    private val code: String? by lazy { arguments!!.getString(CODE) }
    private lateinit var conversation: Conversation
    private var me: Participant? = null
    private var keepDialog: Boolean = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_group_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.left_iv.setOnClickListener { dismiss() }
        contentView.right_iv.setOnClickListener {
            (dialog as BottomSheet).fakeDismiss()
            menu?.show()
        }

        contentView.join_va.setOnClickListener {
            if (code == null) return@setOnClickListener

            contentView.join_va.displayedChild = POS_PB
            contentView.join_va.isEnabled = false
            bottomViewModel.join(code!!).autoDisposable(scopeProvider).subscribe({
                if (it.isSuccess) {
                    dismiss()
                    val conversationResponse = it.data as ConversationResponse
                    val accountId = Session.getAccountId()
                    conversationResponse.participants.forEach {
                        if (it.userId == accountId) {
                            bottomViewModel.refreshConversation(conversationId)
                            return@forEach
                        }
                    }
                    ConversationActivity.show(requireContext(), conversationId)
                } else {
                    contentView.join_va?.displayedChild = POS_TV
                    contentView.join_va.isEnabled = true
                    ErrorHandler.handleMixinError(it.errorCode)
                }
            }, {
                contentView.join_va?.displayedChild = POS_TV
                contentView.join_va.isEnabled = true
                ErrorHandler.handleError(it)
            })
        }
        contentView.detail_tv.movementMethod = LinkMovementMethod()
        contentView.detail_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        contentView.detail_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        contentView.detail_tv.setAutoLinkOnClickListener { _, url ->
            openUrlWithExtraWeb(url, conversationId, requireFragmentManager())
            dismiss()
        }

        bottomViewModel.getConversationById(conversationId).observe(this, Observer { c ->
            if (c == null) return@Observer

            conversation = c
            val icon = c.iconUrl
            contentView.avatar.setGroup(icon)
            if (icon == null || !File(icon).exists()) {
                bottomViewModel.startGenerateAvatar(c.conversationId)
            }
            contentView.name.text = c.name
            contentView.detail_tv.text = c.announcement
            initParticipant()
        })

        contentView.post {
            contentView.detail_tv.maxHeight = requireContext().screenHeight() / 3
        }

        bottomViewModel.refreshConversation(conversationId)
    }

    @SuppressLint("SetTextI18n")
    private fun initParticipant() {
        doAsync {
            val participantsCount = bottomViewModel.getParticipantsCount(conversationId)
            val participants = bottomViewModel.getLimitParticipants(conversationId, DEFAULT_COUNT)
            me = bottomViewModel.findParticipantByIds(conversationId, Session.getAccountId()!!)

            uiThread {
                if (!isAdded) return@uiThread

                initMenu()

                val size = resources.getDimensionPixelSize(R.dimen.bottom_group_avatar_size)
                val margin = dp(7.5f)
                val params = LinearLayout.LayoutParams(size, size)
                params.marginStart = margin
                params.marginEnd = margin
                if (contentView.avatar_container_ll.childCount > 0) {
                    contentView.avatar_container_ll.removeAllViews()
                }
                for (i in 0 until if (participants.size >= DEFAULT_COUNT) DEFAULT_COUNT - 1 else participants.size) {
                    val u = participants[i]
                    val avatarView = AvatarView(requireContext(), null)
                    contentView.avatar_container_ll.addView(avatarView, params)
                    avatarView.setTextSize(14f)
                    avatarView.setInfo(u.fullName, u.avatarUrl, u.userId)
                }
                if (participants.size >= DEFAULT_COUNT) {
                    val moreView = TextView(context).apply {
                        setBackgroundResource(R.drawable.bg_circle_contact)
                        textColor = resources.getColor(R.color.text_gray, null)
                        gravity = Gravity.CENTER
                        text = "+${participantsCount - DEFAULT_COUNT + 1}"
                    }
                    contentView.avatar_container_ll.addView(moreView, params)
                }
                contentView.avatar_container_ll.setOnClickListener {
                    dismiss()
                    GroupActivity.show(requireContext(), GroupActivity.INFO, conversationId)
                }

                contentView.join_va.visibility = if ((me == null) && !TextUtils.isEmpty(code)) VISIBLE else GONE
                contentView.right_iv.visibility = if (me != null) VISIBLE else GONE
            }
        }
    }

    private fun initMenu() {
        val choices = mutableListOf<String>()
        choices.add(getString(R.string.participants))
        if (me != null && (me!!.role == ParticipantRole.OWNER.name || me!!.role == ParticipantRole.ADMIN.name)) {
            if (TextUtils.isEmpty(conversation.announcement)) {
                choices.add(getString(R.string.group_info_add))
            } else {
                choices.add(getString(R.string.group_info_edit))
            }
            choices.add(getString(R.string.group_edit_name))
        }
        if (notNullElse(conversation.muteUntil, {
                Instant.now().isBefore(Instant.parse(it))
            }, false)) {
            choices.add(getString(R.string.un_mute))
        } else {
            choices.add(getString(R.string.mute))
        }
        choices.add(getString(R.string.group_info_clear_chat))
        if (me != null) {
            choices.add(getString(R.string.group_info_exit_group))
        } else {
            choices.add(getString(R.string.group_info_delete_group))
        }
        menu = AlertDialog.Builder(requireContext())
            .setItems(choices.toTypedArray()) { _, which ->
                when (choices[which]) {
                    getString(R.string.participants) -> {
                        dismiss()
                        GroupActivity.show(requireContext(), GroupActivity.INFO, conversationId)
                    }
                    getString(R.string.group_info_add) -> {
                        activity?.addFragment(this@GroupBottomSheetDialogFragment, GroupEditFragment.newInstance(
                            conversationId, conversation.announcement), GroupEditFragment.TAG)
                    }
                    getString(R.string.group_info_edit) -> {
                        activity?.addFragment(this@GroupBottomSheetDialogFragment, GroupEditFragment.newInstance(
                            conversationId, conversation.announcement), GroupEditFragment.TAG)
                    }
                    getString(R.string.group_edit_name) -> {
                        keepDialog = true
                        showDialog(conversation.name)
                    }
                    getString(R.string.un_mute) -> unMute()
                    getString(R.string.mute) -> {
                        keepDialog = true
                        mute()
                    }
                    getString(R.string.group_info_clear_chat) -> {
                        bottomViewModel.deleteMessageByConversationId(conversationId)
                    }
                    getString(R.string.group_info_exit_group) -> {
                        bottomViewModel.exitGroup(conversationId)
                    }
                    getString(R.string.group_info_delete_group) -> {
                        bottomViewModel.deleteGroup(conversationId)
                        callback?.onDelete()
                    }
                }
            }.create()
        menu?.setOnDismissListener {
            if (!keepDialog) {
                dismiss()
            }
        }
    }

    private fun mute() {
        showMuteDialog()
    }

    private fun unMute() {
        val account = Session.getAccount()
        account?.let {
            bottomViewModel.mute(conversationId, 0)
            context?.toast(getString(R.string.un_mute) + " ${conversation.name}")
        }
    }

    private fun showMuteDialog() {
        val choices = arrayOf(getString(R.string.contact_mute_8hours),
            getString(R.string.contact_mute_1week), getString(R.string.contact_mute_1year))
        var duration = UserBottomSheetDialogFragment.MUTE_8_HOURS
        var whichItem = 0
        val alert = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val account = Session.getAccount()
                account?.let {
                    bottomViewModel.mute(conversationId, duration.toLong())
                    context?.toast(getString(R.string.contact_mute_title) + " ${conversation.name} " + choices[whichItem])
                }
                dialog.dismiss()
            }
            .setSingleChoiceItems(choices, 0) { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = UserBottomSheetDialogFragment.MUTE_8_HOURS
                    1 -> duration = UserBottomSheetDialogFragment.MUTE_1_WEEK
                    2 -> duration = UserBottomSheetDialogFragment.MUTE_1_YEAR
                }
            }
            .show()
        alert.setOnDismissListener { dismiss() }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(name: String?) {
        if (context == null) {
            return
        }
        val editText = EditText(requireContext())
        editText.hint = getString(R.string.profile_modify_name_hint)
        editText.setText(name)
        if (name != null) {
            editText.setSelection(name.length)
        }
        val frameLayout = FrameLayout(requireContext())
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = requireContext().dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        val nameDialog = AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setTitle(R.string.profile_modify_name)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                bottomViewModel.updateGroup(conversationId, editText.text.toString(), null)
                dialog.dismiss()
            }
            .show()
        nameDialog.setOnDismissListener { dismiss() }
        nameDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        nameDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    interface Callback {
        fun onDelete()
    }
}