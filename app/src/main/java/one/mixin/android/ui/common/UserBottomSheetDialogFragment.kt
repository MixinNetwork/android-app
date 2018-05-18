package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_user_bottom_sheet.view.*
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.GroupFragment.Companion.ARGS_CONVERSATION_ID
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.util.Session
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dimen
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.margin
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.threeten.bp.Instant

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

    private var keepDialog = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_user_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        user = arguments!!.getParcelable(ARGS_USER)
        conversationId = arguments!!.getString(ARGS_CONVERSATION_ID)
        contentView.left_iv.setOnClickListener { dialog?.dismiss() }
        contentView.right_iv.setOnClickListener {
            (dialog as BottomSheet).fakeDismiss()
            menu.show()
        }
        initMenu()

        bottomViewModel.findUserById(user.userId).observe(this, Observer { u ->
            if (u == null) return@Observer
            // prevent add self
            if (u.userId == Session.getAccountId()) {
                contentView.send_fl.visibility = GONE
                contentView.add_fl.visibility = GONE
                contentView.right_iv.visibility = GONE
                return@Observer
            }
            user = u
            updateUserInfo(u)
        })
        contentView.add_fl.setOnClickListener {
            updateRelationship(UserRelationship.FRIEND.name)
            dialog?.dismiss()
        }
        contentView.send_fl.setOnClickListener {
            // TODO [optimize] have conversation with same user
            context?.let { ctx -> ConversationActivity.show(ctx, null, user) }
            dialog?.dismiss()
        }

        contentView.detail_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        contentView.detail_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        contentView.detail_tv.setAutoLinkOnClickListener({ _, url ->
            when {
                isMixinUrl(url) -> LinkBottomSheetDialogFragment
                    .newInstance(url).show(fragmentManager, LinkBottomSheetDialogFragment.TAG)
                else -> WebBottomSheetDialogFragment
                    .newInstance(url, conversationId)
                    .show(fragmentManager, WebBottomSheetDialogFragment.TAG)
            }
            dialog?.dismiss()
        })
    }

    private fun initMenu() {
        val choices = mutableListOf<String>()
        when (user.relationship) {
            UserRelationship.BLOCKING.name -> {
                choices.add(getString(R.string.contact_other_share))
            }
            UserRelationship.FRIEND.name -> {
                choices.add(getString(R.string.contact_other_share))
                choices.add(getString(R.string.edit_name))
                setMute(choices)
                choices.add(getString(R.string.contact_other_remove))
            }
            UserRelationship.STRANGER.name -> {
                setMute(choices)
                choices.add(getString(R.string.contact_other_block))
            }
        }
        menu = AlertDialog.Builder(context!!)
            .setItems(choices.toTypedArray(), { _, which ->
                when (choices[which]) {
                    getString(R.string.contact_other_share) -> {
                        ForwardActivity.show(context!!, arrayListOf(ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId)), true)
                        dialog?.dismiss()
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
                    getString(R.string.contact_other_block) -> {
                        bottomViewModel.updateRelationship(RelationshipRequest(user.userId, RelationshipAction.BLOCK.name))
                    }
                    getString(R.string.contact_other_remove) -> {
                        updateRelationship(UserRelationship.STRANGER.name)
                    }
                }
            }).create()
        menu.setOnDismissListener {
            if (!keepDialog) {
                dialog?.dismiss()
            }
        }
    }

    private fun setMute(choices: MutableList<String>) {
        if (notNullElse(user.muteUntil, {
                Instant.now().isBefore(Instant.parse(it))
            }, false)) {
            choices.add(getString(R.string.un_mute))
        } else {
            choices.add(getString(R.string.mute))
        }
    }

    private fun updateUserInfo(user: User) {
        contentView.avatar.setInfo(if (user.fullName != null && user.fullName.isNotEmpty())
            user.fullName[0] else ' ', user.avatarUrl, user.identityNumber)
        contentView.name.text = user.fullName
        contentView.id_tv.text = getString(R.string.contact_mixin_id, user.identityNumber)
        contentView.verified_iv.visibility = if (user.isVerified != null && user.isVerified) VISIBLE else GONE
        if (user.isBot()) {
            contentView.bot_iv.visibility = VISIBLE
            doAsync {
                bottomViewModel.findAppById(user.appId!!)?.let { app ->
                    val creator = bottomViewModel.getUserById(app.creatorId)
                    uiThread {
                        if (!isAdded) return@uiThread

                        contentView.detail_tv.visibility = VISIBLE
                        contentView.open_fl.visibility = VISIBLE
                        contentView.creator_tv.visibility = GONE
                        if (!TextUtils.isEmpty(creator?.fullName)) {
                            contentView.creator_tv.visibility = VISIBLE
                            contentView.creator_tv.text = creator?.fullName
                        } else {
                            contentView.creator_tv.visibility = GONE
                        }
                        contentView.detail_tv.text = app.description
                        contentView.open_fl.setOnClickListener {
                            dialog?.dismiss()
                            WebBottomSheetDialogFragment
                                .newInstance(app.homeUri, conversationId, app.name)
                                .show(fragmentManager, WebBottomSheetDialogFragment.TAG)
                        }
                    }
                }
            }
        } else {
            contentView.creator_tv.visibility = GONE
            contentView.bot_iv.visibility = GONE
            contentView.detail_tv.visibility = GONE
            contentView.open_fl.visibility = GONE
        }

        updateUserStatus(user.relationship)
    }

    private fun updateUserStatus(relationship: String) {
        when (relationship) {
            UserRelationship.BLOCKING.name -> {
                contentView.send_fl.visibility = GONE
                contentView.add_fl.visibility = GONE
                contentView.unblock_fl.visibility = VISIBLE
                contentView.unblock_fl.setOnClickListener {
                    bottomViewModel.updateRelationship(RelationshipRequest(user.userId, RelationshipAction.UNBLOCK.name))
                    dialog?.dismiss()
                }
            }
            UserRelationship.FRIEND.name -> {
                contentView.add_fl.visibility = GONE
                contentView.unblock_fl.visibility = GONE
            }
            UserRelationship.STRANGER.name -> {
                contentView.add_fl.visibility = VISIBLE
                contentView.unblock_fl.visibility = GONE
            }
            else -> {
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(name: String?) {
        if (context == null) {
            return
        }
        val editText = EditText(context!!)
        editText.id = R.id.profile_edit_name_et
        editText.hint = getString(R.string.profile_modify_name_hint)
        editText.setText(name)
        if (name != null) {
            editText.setSelection(name.length)
        }
        val frameLayout = FrameLayout(context)
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        val nameDialog = AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(R.string.profile_modify_name)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
            .setPositiveButton(R.string.confirm, { dialog, _ ->
                bottomViewModel.updateRelationship(RelationshipRequest(user.userId,
                    RelationshipAction.UPDATE.name, editText.text.toString()))
                dialog.dismiss()
            })
            .show()
        nameDialog.setOnDismissListener { dialog?.dismiss() }
        nameDialog.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        nameDialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun showMuteDialog() {
        val choices = arrayOf(getString(R.string.contact_mute_8hours),
            getString(R.string.contact_mute_1week),
            getString(R.string.contact_mute_1year))
        var duration = MUTE_8_HOURS
        var whichItem = 0
        val alert = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.cancel, { dialog, _ ->
                dialog.dismiss()
            })
            .setPositiveButton(R.string.confirm, { dialog, which ->
                val account = Session.getAccount()
                account?.let {
                    bottomViewModel.mute(it.userId, user.userId, duration.toLong())
                    toast(getString(R.string.contact_mute_title) + " ${user.fullName} " + choices[whichItem])
                }
                dialog.dismiss()
            })
            .setSingleChoiceItems(choices, 0, { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = MUTE_8_HOURS
                    1 -> duration = MUTE_1_WEEK
                    2 -> duration = MUTE_1_YEAR
                }
            })
            .show()
        alert.setOnDismissListener { dialog?.dismiss() }
    }

    private fun mute() {
        showMuteDialog()
    }

    private fun unMute() {
        val account = Session.getAccount()
        account?.let {
            bottomViewModel.mute(it.userId, user.userId, 0)
            toast(getString(R.string.un_mute) + " ${user.fullName}")
        }
    }

    private fun updateRelationship(relationship: String) {
        updateUserStatus(relationship)
        val request = RelationshipRequest(user.userId,
            if (relationship == UserRelationship.FRIEND.name)
                RelationshipAction.ADD.name else RelationshipAction.REMOVE.name, user.fullName)
        bottomViewModel.updateRelationship(request)
    }
}