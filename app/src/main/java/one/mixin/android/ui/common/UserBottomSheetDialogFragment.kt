package one.mixin.android.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.view.clicks
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_user_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.event.ExitEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.localTime
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showConfirmDialog
import one.mixin.android.extension.toast
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.common.info.MenuStyle
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menu
import one.mixin.android.ui.common.info.menuGroup
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
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
import one.mixin.android.vo.CallState
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.showVerifiedOrBot
import one.mixin.android.webrtc.CallService
import one.mixin.android.widget.linktext.AutoLinkMode
import org.threeten.bp.Instant

class UserBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {

    companion object {
        const val TAG = "UserBottomSheetDialogFragment"

        const val MUTE_8_HOURS = 8 * 60 * 60
        const val MUTE_1_WEEK = 7 * 24 * 60 * 60
        const val MUTE_1_YEAR = 365 * 24 * 60 * 60
        private var instant: UserBottomSheetDialogFragment? = null
        fun newInstance(user: User, conversationId: String? = null): UserBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return UserBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_USER, user)
                    putString(ARGS_CONVERSATION_ID, conversationId)
                }
            }.apply {
                instant = this
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    private lateinit var user: User
    // bot need conversation id
    private var conversationId: String? = null
    private var creator: User? = null

    @Inject
    lateinit var linkState: LinkState
    @Inject
    lateinit var callState: CallState

    private var menuListLayout: ViewGroup? = null

    var showUserTransactionAction: (() -> Unit)? = null

    override fun getLayoutId() = R.layout.fragment_user_bottom_sheet

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
                ProfileBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, TAG)
                dismiss()
                return@Observer
            }
            updateUserInfo(u)
            if (menuListLayout == null ||
                u.relationship != user.relationship ||
                u.muteUntil != user.muteUntil ||
                u.fullName != user.fullName
            ) {
                initMenu(u)
            }
            user = u

            contentView.doOnPreDraw {
                behavior?.peekHeight =
                    contentView.title.height + contentView.scroll_content.height -
                        (menuListLayout?.height
                            ?: 0) - if (menuListLayout != null) requireContext().dpToPx(38f) else requireContext().dpToPx(
                        8f
                    )
            }
        })
        contentView.transfer_fl.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                TransferFragment.newInstance(user.userId, supportSwitchAsset = true)
                    .showNow(parentFragmentManager, TransferFragment.TAG)
                dismiss()
            } else {
                toast(R.string.transfer_without_pin)
            }
        }
        contentView.send_fl.setOnClickListener {
            if (user.userId == Session.getAccountId()) {
                toast(R.string.cant_talk_self)
                return@setOnClickListener
            }
            context?.let { ctx ->
                if (MixinApplication.conversationId == null || generateConversationId(
                        user.userId,
                        Session.getAccountId()!!
                    ) != MixinApplication.conversationId
                ) {
                    ConversationActivity.show(ctx, null, user.userId)
                }
            }
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
        lifecycleScope.launch {
            bottomViewModel.loadFavoriteApps(user.userId) { apps ->
                contentView.avatar_ll.isVisible = !apps.isNullOrEmpty()
                contentView.avatar_ll.setOnClickListener {
                    if (!apps.isNullOrEmpty()) {
                        AppListBottomSheetDialogFragment.newInstance(
                            apps,
                            getString(R.string.contact_share_apps_title, user.fullName)
                        )
                            .showNow(parentFragmentManager, AppListBottomSheetDialogFragment.TAG)
                    }
                }
                apps?.let {
                    contentView.avatar_group.setApps(it)
                    contentView.doOnPreDraw {
                        behavior?.peekHeight =
                            contentView.title.height + contentView.scroll_content.height -
                                (menuListLayout?.height
                                    ?: 0) - if (menuListLayout != null) requireContext().dpToPx(38f) else requireContext().dpToPx(
                                8f
                            )
                    }
                }
            }
        }
    }

    private fun initMenu(u: User) {
        val clearMenu = menu {
            title = getString(R.string.group_info_clear_chat)
            style = MenuStyle.Danger
            action = {
                requireContext().showConfirmDialog(getString(R.string.group_info_clear_chat)) {
                    bottomViewModel.deleteMessageByConversationId(
                        generateConversationId(
                            Session.getAccountId()!!,
                            u.userId
                        )
                    )
                }
            }
        }
        val muteMenu = if (u.muteUntil.notNullWithElse({
                Instant.now().isBefore(Instant.parse(it))
            }, false)) {
            menu {
                title = getString(R.string.un_mute)
                subtitle = getString(R.string.mute_until, u.muteUntil?.localTime())
                action = { unMute() }
            }
        } else {
            menu {
                title = getString(R.string.mute)
                action = { mute() }
            }
        }
        val transactionMenu = menu {
            title = getString(R.string.contact_other_transactions)
            action = {
                if (showUserTransactionAction != null) {
                    showUserTransactionAction?.invoke()
                } else {
                    activity?.addFragment(
                        this@UserBottomSheetDialogFragment,
                        UserTransactionsFragment.newInstance(u.userId),
                        UserTransactionsFragment.TAG
                    )
                }
                dismiss()
            }
        }
        val editNameMenu = menu {
            title = getString(R.string.edit_name)
            action = { showDialog(u.fullName) }
        }
        val voiceCallMenu = menu {
            title = getString(R.string.voice_call)
            action = {
                startVoiceCall()
            }
        }
        val phoneNum = user.phone
        val telephoneCallMenu = if (!phoneNum.isNullOrEmpty()) {
            val phoneUri = Uri.parse("tel:$phoneNum")
            menu {
                title = getString(R.string.phone_call)
                subtitle = phoneNum
                action = {
                    requireContext().showConfirmDialog(getString(R.string.call_who, phoneNum)) {
                        Intent(Intent.ACTION_DIAL).run {
                            this.data = phoneUri
                            startActivity(this)
                        }
                    }
                }
            }
        } else null
        val developerMenu = menu {
            title = getString(R.string.developer)
            action = {
                creator?.let {
                    if (it.userId == Session.getAccountId()) {
                        ProfileBottomSheetDialogFragment.newInstance()
                            .showNow(parentFragmentManager, TAG)
                    } else {
                        UserBottomSheetDialogFragment.newInstance(it)
                            .showNow(parentFragmentManager, TAG)
                    }
                }
                dismiss()
            }
        }
        val list = menuList {
            menuGroup {
                menu {
                    title = getString(R.string.contact_other_share)
                    action = {
                        ForwardActivity.show(
                            requireContext(),
                            arrayListOf(
                                ForwardMessage(
                                    ForwardCategory.CONTACT.name,
                                    sharedUserId = u.userId
                                )
                            )
                        )
                        dismiss()
                    }
                }
            }
            menuGroup {
                menu {
                    title = getString(R.string.contact_other_shared_media)
                    action = {
                        SharedMediaActivity.show(
                            requireContext(), generateConversationId(
                                user.userId,
                                Session.getAccountId()!!
                            )
                        )
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

        if (u.relationship == UserRelationship.FRIEND.name) {
            list.groups.add(menuGroup {
                menu(muteMenu)
                menu(editNameMenu)
            })
        } else {
            list.groups.add(menuGroup {
                menu(muteMenu)
            })
        }

        if (u.isBot()) {
            if (telephoneCallMenu != null) {
                list.groups.add(menuGroup {
                    menu(telephoneCallMenu)
                })
            }
        } else {
            list.groups.add(menuGroup {
                menu(voiceCallMenu)
                telephoneCallMenu?.let { menu(it) }
            })
        }

        if (u.isBot()) {
            list.groups.add(menuGroup {
                menu(developerMenu)
                menu(transactionMenu)
            })
        } else {
            list.groups.add(menuGroup {
                menu(transactionMenu)
            })
        }

        when (u.relationship) {
            UserRelationship.BLOCKING.name -> {
                list.groups.add(menuGroup {
                    menu {
                        title = getString(R.string.contact_other_unblock)
                        style = MenuStyle.Danger
                        action = {
                            bottomViewModel.updateRelationship(
                                RelationshipRequest(
                                    u.userId,
                                    RelationshipAction.UNBLOCK.name
                                )
                            )
                        }
                    }
                    menu(clearMenu)
                })
            }
            UserRelationship.FRIEND.name -> {
                list.groups.add(menuGroup {
                    menu {
                        title = getString(R.string.contact_other_remove)
                        style = MenuStyle.Danger
                        action = {
                            requireContext().showConfirmDialog(getString(R.string.contact_other_remove)) {
                                updateRelationship(UserRelationship.STRANGER.name)
                            }
                        }
                    }
                    menu(clearMenu)
                })
            }
            UserRelationship.STRANGER.name -> {
                list.groups.add(menuGroup {
                    menu {
                        title = getString(R.string.contact_other_block)
                        style = MenuStyle.Danger
                        action = {
                            requireContext().showConfirmDialog(getString(R.string.contact_other_block)) {
                                bottomViewModel.updateRelationship(
                                    RelationshipRequest(
                                        u.userId,
                                        RelationshipAction.BLOCK.name
                                    )
                                )
                            }
                        }
                    }
                    menu(clearMenu)
                })
            }
        }
        list.groups.add(menuGroup {
            menu {
                title = getString(R.string.contact_other_report)
                style = MenuStyle.Danger
                action = {
                    requireContext().showConfirmDialog(getString(R.string.contact_other_report)) {
                        reportUser(u.userId)
                    }
                }
            }
        })

        menuListLayout?.removeAllViews()
        contentView.scroll_content.removeView(menuListLayout)
        list.createMenuLayout(requireContext()).let { layout ->
            menuListLayout = layout
            contentView.scroll_content.addView(layout)
            layout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = requireContext().dpToPx(30f)
            }
            contentView.more_fl.setOnClickListener {
                if (behavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    contentView.more_iv.rotationX = 180f
                } else {
                    behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                    contentView.scroll_view.smoothScrollTo(0, 0)
                    contentView.more_iv.rotationX = 0f
                }
            }
        }
    }

    private fun startSearchConversation() = lifecycleScope.launch(Dispatchers.IO) {
        bottomViewModel.getConversation(
            generateConversationId(
                user.userId,
                Session.getAccountId()!!
            )
        )?.let {
            val searchMessageItem = if (it.category == ConversationCategory.CONTACT.name) {
                SearchMessageItem(
                    it.conversationId, it.category, null,
                    0, user.userId, user.fullName, user.avatarUrl, null
                )
            } else {
                SearchMessageItem(
                    it.conversationId, it.category, it.name,
                    0, "", null, null, it.iconUrl
                )
            }
            activity?.addFragment(
                this@UserBottomSheetDialogFragment,
                SearchMessageFragment.newInstance(searchMessageItem, ""),
                SearchMessageFragment.TAG
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun startVoiceCall() {
        if (!callState.isIdle()) {
            if (callState.user?.userId == user.userId) {
                CallActivity.show(requireContext(), user)
            } else {
                alertDialogBuilder()
                    .setMessage(getString(R.string.chat_call_warning_call))
                    .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        } else {
            RxPermissions(requireActivity())
                .request(Manifest.permission.RECORD_AUDIO)
                .subscribe({ granted ->
                    if (granted) {
                        callVoice()
                    } else {
                        context?.openPermissionSetting()
                    }
                }, {
                })
        }
    }

    private fun callVoice() {
        if (LinkState.isOnline(linkState.state)) {
            CallService.outgoing(requireContext(), user, generateConversationId(
                Session.getAccountId()!!,
                user.userId
            ))
            dismiss()
        } else {
            toast(R.string.error_no_connection)
        }
    }

    private fun reportUser(userId: String) {
        alertDialogBuilder()
            .setMessage(getString(R.string.contact_other_report_warning))
            .setNeutralButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.contact_other_report)) { dialog, _ ->
                val conversationId = generateConversationId(userId, Session.getAccountId()!!)
                bottomViewModel.updateRelationship(
                    RelationshipRequest(
                        userId,
                        RelationshipAction.BLOCK.name
                    ), conversationId
                )
                RxBus.publish(ExitEvent(conversationId))
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateUserInfo(user: User) = lifecycleScope.launch {
        if (!isAdded) return@launch

        contentView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        contentView.name.text = user.fullName
        contentView.id_tv.text = getString(R.string.contact_mixin_id, user.identityNumber)
        contentView.id_tv.setOnLongClickListener {
            context?.getClipboardManager()
                ?.setPrimaryClip(ClipData.newPlainText(null, user.identityNumber))
            context?.toast(R.string.copy_success)
            true
        }
        if (user.biography.isNotEmpty()) {
            contentView.detail_tv.text = user.biography
            contentView.detail_tv.visibility = VISIBLE
        } else {
            contentView.detail_tv.visibility = GONE
        }
        updateUserStatus(user.relationship)
        user.showVerifiedOrBot(contentView.verified_iv, contentView.bot_iv)
        contentView.op_ll.isVisible = true
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
                    bottomViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.UNBLOCK.name
                        )
                    )
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

        editDialog {
            titleText = this@UserBottomSheetDialogFragment.getString(R.string.edit_name)
            editText = name
            maxTextCount = 40
            allowEmpty = false
            rightAction = {
                bottomViewModel.updateRelationship(
                    RelationshipRequest(
                        user.userId,
                        RelationshipAction.UPDATE.name, it
                    )
                )
            }
        }
    }

    private fun showMuteDialog() {
        val choices = arrayOf(
            getString(R.string.contact_mute_8hours),
            getString(R.string.contact_mute_1week),
            getString(R.string.contact_mute_1year)
        )
        var duration = MUTE_8_HOURS
        var whichItem = 0
        alertDialogBuilder()
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
        val request = RelationshipRequest(
            user.userId,
            if (relationship == UserRelationship.FRIEND.name)
                RelationshipAction.ADD.name else RelationshipAction.REMOVE.name, user.fullName
        )
        bottomViewModel.updateRelationship(request)
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        when (newState) {
            BottomSheetBehavior.STATE_HIDDEN -> dismiss()
            BottomSheetBehavior.STATE_COLLAPSED -> contentView.more_iv.rotationX = 0f
            BottomSheetBehavior.STATE_EXPANDED -> contentView.more_iv.rotationX = 180f
        }
    }
}
