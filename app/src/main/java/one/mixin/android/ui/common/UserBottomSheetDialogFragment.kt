package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_user_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.event.ExitEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.UserTransactionsFragment
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.media.SharedMediaActivity
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.url.openUrlWithExtraWeb
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.showVerifiedOrBot
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dimen
import org.jetbrains.anko.margin
import org.threeten.bp.Instant
import java.util.concurrent.TimeUnit

class UserBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "UserBottomSheetDialog"

        const val MUTE_8_HOURS = 8 * 60 * 60
        const val MUTE_1_WEEK = 7 * 24 * 60 * 60
        const val MUTE_1_YEAR = 365 * 24 * 60 * 60

        fun newInstance(user: User, conversationId: String? = null) = UserBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_USER, user)
                putString(ARGS_CONVERSATION_ID, conversationId)
            }
        }
    }

    private lateinit var user: User
    // bot need conversation id
    private var conversationId: String? = null
    private lateinit var menu: AlertDialog
    private var creator: User? = null

    private var keepDialog = false

    var showUserTransactionAction: (() -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_user_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        user = arguments!!.getParcelable(ARGS_USER)!!
        conversationId = arguments!!.getString(ARGS_CONVERSATION_ID)
        contentView.title.right_iv.setOnClickListener { dismiss() }
        contentView.avatar.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            user.avatarUrl?.let { url ->
                AvatarActivity.show(requireActivity(), url, contentView.avatar)
                dismiss()
            }
        }

        bottomViewModel.findUserById(user.userId).observe(this, Observer { u ->
            if (u == null) return@Observer
            // prevent add self
            if (u.userId == Session.getAccountId()) {
                activity?.addFragment(this@UserBottomSheetDialogFragment, ProfileFragment.newInstance(), ProfileFragment.TAG)
                dismiss()
                return@Observer
            }
            user = u
            updateUserInfo(u)
            initMenu(u)
        })
        contentView.transfer_fl.setOnClickListener {
            TransferFragment.newInstance(user.userId, supportSwitchAsset = true)
                .showNow(parentFragmentManager, TransferFragment.TAG)
        }
        contentView.send_fl.setOnClickListener {
            if (user.userId == Session.getAccountId()) {
                toast(R.string.cant_talk_self)
                return@setOnClickListener
            }
            context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            dismiss()
        }
        contentView.detail_tv.movementMethod = LinkMovementMethod()
        contentView.detail_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        contentView.detail_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        contentView.detail_tv.setAutoLinkOnClickListener { _, url ->
            openUrlWithExtraWeb(url, conversationId, parentFragmentManager)
            dismiss()
        }

        bottomViewModel.refreshUser(user.userId, true)
    }

    private fun initMenu(u: User) {
        val choices = mutableListOf<String>()
        if (u.isBot()) {
            choices.add(getString(R.string.developer))
        }
        choices.add(getString(R.string.contact_other_share))
        if (conversationId != null) {
            choices.add(getString(R.string.contact_other_search_conversation))
            choices.add(getString(R.string.contact_other_shared_media))
        }
        choices.add(getString(R.string.contact_other_transactions))
        when (user.relationship) {
            UserRelationship.BLOCKING.name -> {
                choices.add(getString(R.string.contact_other_unblock))
            }
            UserRelationship.FRIEND.name -> {
                choices.add(getString(R.string.edit_name))
                setMute(choices)
                choices.add(getString(R.string.contact_other_remove))
            }
            UserRelationship.STRANGER.name -> {
                setMute(choices)
                choices.add(getString(R.string.contact_other_block))
            }
        }
        choices.add(getString(R.string.group_info_clear_chat))
        choices.add(getString(R.string.contact_other_report))
        menu = AlertDialog.Builder(context!!)
            .setItems(choices.toTypedArray()) { _, which ->
                if (!isAdded) return@setItems

                when (choices[which]) {
                    getString(R.string.developer) -> {
                        creator?.let {
                            if (it.userId == Session.getAccountId()) {
                                activity?.addFragment(this@UserBottomSheetDialogFragment, ProfileFragment.newInstance(), ProfileFragment.TAG)
                            } else {
                                UserBottomSheetDialogFragment.newInstance(it).showNow(parentFragmentManager, TAG)
                            }
                        }
                        dismiss()
                    }
                    getString(R.string.contact_other_share) -> {
                        ForwardActivity.show(context!!, arrayListOf(ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId)), true)
                        dismiss()
                    }
                    getString(R.string.contact_other_search_conversation) -> {
                        startSearchConversation()
                        dismiss()
                    }
                    getString(R.string.contact_other_shared_media) -> {
                        SharedMediaActivity.show(requireContext(), conversationId!!)
                        dismiss()
                    }
                    getString(R.string.contact_other_transactions) -> {
                        if (showUserTransactionAction != null) {
                            showUserTransactionAction?.invoke()
                        } else {
                            activity?.addFragment(this, UserTransactionsFragment.newInstance(user.userId), UserTransactionsFragment.TAG)
                        }
                        dismiss()
                    }
                    getString(R.string.edit_name) -> {
                        keepDialog = true
                        showDialog(user.fullName)
                    }
                    getString(R.string.un_mute) -> {
                        unMute()
                    }
                    getString(R.string.mute) -> {
                        keepDialog = true
                        mute()
                    }
                    getString(R.string.group_info_clear_chat) -> {
                        bottomViewModel.deleteMessageByConversationId(
                            generateConversationId(
                                Session.getAccountId()!!,
                                user.userId
                            )
                        )
                    }
                    getString(R.string.contact_other_block) -> {
                        bottomViewModel.updateRelationship(RelationshipRequest(user.userId, RelationshipAction.BLOCK.name))
                    }
                    getString(R.string.contact_other_unblock) -> {
                        bottomViewModel.updateRelationship(RelationshipRequest(user.userId, RelationshipAction.UNBLOCK.name))
                    }
                    getString(R.string.contact_other_remove) -> {
                        updateRelationship(UserRelationship.STRANGER.name)
                    }
                    getString(R.string.contact_other_report) -> {
                        reportUser(user.userId)
                    }
                }
            }.create()
        menu.setOnDismissListener {
            if (!keepDialog) {
                dismiss()
            }
        }

        contentView.more_fl.setOnClickListener {
            (dialog as BottomSheet).fakeDismiss()
            menu.show()
        }
    }

    private fun startSearchConversation() = lifecycleScope.launch(Dispatchers.IO) {
        bottomViewModel.getConversation(conversationId!!)?.let {
            val searchMessageItem = if (it.category == ConversationCategory.CONTACT.name) {
                SearchMessageItem(it.conversationId, it.category, null,
                    0, user.userId, user.fullName, user.avatarUrl, null)
            } else {
                SearchMessageItem(it.conversationId, it.category, it.name,
                    0, "", null, null, it.iconUrl)
            }
            activity?.addFragment(this@UserBottomSheetDialogFragment,
                SearchMessageFragment.newInstance(searchMessageItem, ""), SearchMessageFragment.TAG)
        }
    }

    private fun reportUser(userId: String) {
        AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setMessage(getString(R.string.contact_other_report_warning))
            .setNeutralButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.contact_other_report)) { dialog, _ ->
                val conversationId = generateConversationId(userId, Session.getAccountId()!!)
                bottomViewModel.updateRelationship(RelationshipRequest(userId, RelationshipAction.BLOCK.name), conversationId)
                RxBus.publish(ExitEvent(conversationId))
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun setMute(choices: MutableList<String>) {
        if (user.muteUntil.notNullWithElse({
                Instant.now().isBefore(Instant.parse(it))
            }, false)) {
            choices.add(getString(R.string.un_mute))
        } else {
            choices.add(getString(R.string.mute))
        }
    }

    private fun updateUserInfo(user: User) = lifecycleScope.launch {
        if (!isAdded) return@launch

        contentView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        contentView.name.text = user.fullName
        contentView.id_tv.text = getString(R.string.contact_mixin_id, user.identityNumber)
        contentView.id_tv.setOnLongClickListener {
            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, user.identityNumber))
            context?.toast(R.string.copy_success)
            true
        }
        user.showVerifiedOrBot(contentView.verified_iv, contentView.bot_iv)
        if (user.isBot()) {
            contentView.open_fl.visibility = VISIBLE
            contentView.transfer_fl.visibility = GONE
            bottomViewModel.findAppById(user.appId!!)?.let { app ->
                contentView.open_fl.clicks()
                    .observeOn(AndroidSchedulers.mainThread())
                    .throttleFirst(1, TimeUnit.SECONDS)
                    .autoDispose(stopScope).subscribe {
                        dismiss()
                        WebBottomSheetDialogFragment
                            .newInstance(
                                app.homeUri,
                                conversationId,
                                app.appId,
                                app.name,
                                app.icon_url,
                                app.capabilities
                            )
                            .showNow(parentFragmentManager, WebBottomSheetDialogFragment.TAG)
                    }
                bottomViewModel.findUserById(app.creatorId)
                    .observe(this@UserBottomSheetDialogFragment, Observer { u ->
                        creator = u
                        if (u == null) {
                            bottomViewModel.refreshUser(app.creatorId, true)
                        }
                    })
            }
        } else {
            contentView.open_fl.visibility = GONE
            contentView.transfer_fl.visibility = VISIBLE
        }
        if (user.biography.isNotEmpty()) {
            contentView.detail_tv.text = user.biography
            contentView.detail_tv.visibility = VISIBLE
        } else {
            contentView.detail_tv.visibility = GONE
        }
        updateUserStatus(user.relationship)
    }

    private val blockDrawable: Drawable by lazy {
        val d = resources.getDrawable(R.drawable.ic_bottom_block, context?.theme)
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        d
    }

    private fun updateUserStatus(relationship: String) {
        if (!isAdded) return

        when (relationship) {
            UserRelationship.BLOCKING.name -> {
                contentView.add_tv.visibility = VISIBLE
                contentView.add_tv.text = getString(R.string.contact_other_unblock)
                contentView.add_tv.setCompoundDrawables(blockDrawable, null, null, null)
                contentView.add_tv.setOnClickListener {
                    bottomViewModel.updateRelationship(RelationshipRequest(user.userId, RelationshipAction.UNBLOCK.name))
                }
            }
            UserRelationship.FRIEND.name -> {
                contentView.add_tv.visibility = GONE
            }
            UserRelationship.STRANGER.name -> {
                contentView.add_tv.visibility = VISIBLE
                contentView.add_tv.setCompoundDrawables(null, null, null, null)
                contentView.add_tv.text = getString(R.string.add_contact)
                contentView.add_tv.setOnClickListener {
                    updateRelationship(UserRelationship.FRIEND.name)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(name: String?) {
        if (context == null || !isAdded) {
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
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        val nameDialog = AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(R.string.profile_modify_name)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                bottomViewModel.updateRelationship(RelationshipRequest(user.userId,
                    RelationshipAction.UPDATE.name, editText.text.toString()))
                dialog.dismiss()
            }
            .show()
        nameDialog.setOnDismissListener { dismiss() }
        nameDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                nameDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !(s.isNullOrBlank() || s.toString() == name.toString())
            }
        })

        nameDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        nameDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun showMuteDialog() {
        val choices = arrayOf(getString(R.string.contact_mute_8hours),
            getString(R.string.contact_mute_1week),
            getString(R.string.contact_mute_1year))
        var duration = MUTE_8_HOURS
        var whichItem = 0
        val alert = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val account = Session.getAccount()
                account?.let {
                    bottomViewModel.mute(it.userId, user.userId, duration.toLong())
                    context?.toast(getString(R.string.contact_mute_title) + " ${user.fullName} " + choices[whichItem])
                }
                dialog.dismiss()
            }
            .setSingleChoiceItems(choices, 0) { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = MUTE_8_HOURS
                    1 -> duration = MUTE_1_WEEK
                    2 -> duration = MUTE_1_YEAR
                }
            }
            .show()
        alert.setOnDismissListener { dismiss() }
    }

    private fun mute() {
        showMuteDialog()
    }

    private fun unMute() {
        val account = Session.getAccount()
        account?.let {
            bottomViewModel.mute(it.userId, user.userId, 0)
            context?.toast(getString(R.string.un_mute) + " ${user.fullName}")
        }
    }

    private fun updateRelationship(relationship: String) = lifecycleScope.launch {
        if (!isAdded) return@launch

        updateUserStatus(relationship)
        val request = RelationshipRequest(user.userId,
            if (relationship == UserRelationship.FRIEND.name)
                RelationshipAction.ADD.name else RelationshipAction.REMOVE.name, user.fullName)
        bottomViewModel.updateRelationship(request)
    }
}
