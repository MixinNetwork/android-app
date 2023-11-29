package one.mixin.android.ui.common

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.Constants.Mute.MUTE_1_HOUR
import one.mixin.android.Constants.Mute.MUTE_1_WEEK
import one.mixin.android.Constants.Mute.MUTE_1_YEAR
import one.mixin.android.Constants.Mute.MUTE_8_HOURS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.databinding.FragmentUserBottomSheetBinding
import one.mixin.android.event.BotCloseEvent
import one.mixin.android.event.BotEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.localTime
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showConfirmDialog
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.common.biometric.buildEmptyTransferBiometricItem
import one.mixin.android.ui.common.info.MenuStyle
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menu
import one.mixin.android.ui.common.info.menuGroup
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.media.SharedMediaActivity
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.wallet.UserTransactionBottomSheetFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.addPinShortcut
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.showVerifiedOrBot
import one.mixin.android.webrtc.outgoingCall
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.widget.picker.getTimeInterval
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class UserBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {
    companion object {
        const val TAG = "UserBottomSheetDialogFragment"

        @SuppressLint("StaticFieldLeak")
        private var instant: UserBottomSheetDialogFragment? = null

        fun newInstance(
            user: User,
            conversationId: String? = null,
        ): UserBottomSheetDialogFragment? {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            if (user.notMessengerUser()) {
                return null
            }
            return UserBottomSheetDialogFragment().apply {
                arguments =
                    Bundle().apply {
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
    private var botConversationId: String? = null
    private val conversationId by lazy {
        generateConversationId(Session.getAccountId()!!, user.userId)
    }
    private var creator: User? = null

    @Inject
    lateinit var linkState: LinkState

    @Inject
    lateinit var callState: CallStateLiveData

    private var menuListLayout: ViewGroup? = null

    var showUserTransactionAction: (() -> Unit)? = null
    var sharedMediaCallback: (() -> Unit)? = null

    override fun getLayoutId() = R.layout.fragment_user_bottom_sheet

    private val binding by lazy {
        FragmentUserBottomSheetBinding.bind(contentView)
    }

    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        user = requireArguments().getParcelableCompat(ARGS_USER, User::class.java)!!
        botConversationId = requireArguments().getString(ARGS_CONVERSATION_ID)
        binding.title.rightIv.setOnClickListener { dismiss() }
        binding.avatar.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val avatar = user.avatarUrl
            if (avatar.isNullOrBlank()) {
                return@setOnClickListener
            }
            AvatarActivity.show(requireActivity(), avatar, binding.avatar)
            dismiss()
        }

        bottomViewModel.findUserById(user.userId).observe(
            this,
            Observer { u ->
                if (u == null) return@Observer
                // prevent add self
                if (u.userId == Session.getAccountId()) {
                    ProfileBottomSheetDialogFragment.newInstance()
                        .showNow(parentFragmentManager, ProfileBottomSheetDialogFragment.TAG)
                    dismiss()
                    return@Observer
                }
                // compare user info changes should refresh menu
                if (menuListLayout == null ||
                    u.relationship != user.relationship ||
                    u.muteUntil != user.muteUntil ||
                    u.fullName != user.fullName
                ) {
                    lifecycleScope.launch {
                        val circleNames = bottomViewModel.findCirclesNameByConversationId(conversationId)
                        val conversation = bottomViewModel.getConversation(conversationId)
                        initMenu(u, circleNames, conversation)
                    }
                }

                user = u
                updateUserInfo(u)

                contentView.doOnPreDraw {
                    if (!isAdded) return@doOnPreDraw

                    behavior?.peekHeight =
                        binding.title.height +
                        binding.scrollContent.height -
                        (menuListLayout?.height ?: 0) - if (menuListLayout != null) 38.dp else 8.dp
                }
            },
        )
        binding.transferFl.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                TransferFragment.newInstance(buildEmptyTransferBiometricItem(user))
                    .showNow(parentFragmentManager, TransferFragment.TAG)
                RxBus.publish(BotCloseEvent())
                dismiss()
            } else {
                toast(R.string.transfer_without_pin)
            }
        }
        binding.sendFl.setOnClickListener {
            if (user.userId == Session.getAccountId()) {
                toast(R.string.cant_talk_self)
                return@setOnClickListener
            }
            context?.let { ctx ->
                if (MixinApplication.conversationId == null || conversationId != MixinApplication.conversationId) {
                    RxBus.publish(BotCloseEvent())
                    ConversationActivity.showAndClear(ctx, conversationId = null, recipientId = user.userId)
                }
                dismiss()
            }
        }
        binding.shareFl.setOnClickListener {
            forwardContact(user)
        }
        setDetailsTv(binding.detailTv, binding.scrollView, conversationId)
        bottomViewModel.refreshUser(user.userId, true)
        bottomViewModel.loadFavoriteApps(user.userId)
        bottomViewModel.observerFavoriteApps(user.userId).observe(this@UserBottomSheetDialogFragment) { apps ->
            binding.avatarLl.isVisible = user.isDeactivated != true && !apps.isNullOrEmpty()
            binding.avatarLl.setOnClickListener {
                if (!apps.isNullOrEmpty()) {
                    AppListBottomSheetDialogFragment.newInstance(
                        apps,
                        getString(R.string.contact_share_bots_title, user.fullName),
                    ).showNow(parentFragmentManager, AppListBottomSheetDialogFragment.TAG)
                }
            }
            debugLongClick(
                binding.avatar,
                {
                    context?.getClipboardManager()?.setPrimaryClip(
                        ClipData.newPlainText(
                            null,
                            "mixin://users/${user.userId}",
                        ),
                    )
                },
            )
            apps?.let {
                binding.avatarGroup.setApps(it)
                contentView.doOnPreDraw {
                    behavior?.peekHeight =
                        binding.title.height + binding.scrollContent.height -
                        (
                            menuListLayout?.height
                                ?: 0
                        ) - if (menuListLayout != null) 38.dp else 8.dp
                }
            }
        }
    }

    private fun initMenu(
        u: User,
        circleNames: List<String>,
        conversation: Conversation?,
    ) {
        if (!isAdded) return

        val clearMenu =
            menu {
                title = getString(R.string.Clear_chat)
                style = MenuStyle.Danger
                action = {
                    requireContext().showConfirmDialog(getString(R.string.Clear_chat)) {
                        bottomViewModel.clearChat(conversationId)
                        dismiss()
                    }
                }
            }
        val muteMenu =
            if (u.muteUntil.notNullWithElse(
                    {
                        Instant.now().isBefore(Instant.parse(it))
                    },
                    false,
                )
            ) {
                menu {
                    title = getString(R.string.Unmute)
                    subtitle = getString(R.string.Mute_until, u.muteUntil?.localTime())
                    action = { unMute() }
                }
            } else {
                menu {
                    title = getString(R.string.Mute)
                    action = { mute() }
                }
            }
        val transactionMenu =
            menu {
                title = getString(R.string.Transactions)
                action = {
                    if (showUserTransactionAction != null) {
                        showUserTransactionAction?.invoke()
                    } else {
                        RxBus.publish(BotCloseEvent())
                        UserTransactionBottomSheetFragment.newInstance(u.userId)
                            .showNow(parentFragmentManager, UserTransactionBottomSheetFragment.TAG)
                    }
                    dismiss()
                }
            }
        val editNameMenu =
            menu {
                title = getString(R.string.Edit_Name)
                action = { showDialog(u.fullName) }
            }
        val voiceCallMenu =
            menu {
                title = getString(R.string.Voice_call)
                action = {
                    startVoiceCall()
                }
            }
        val phoneNum = user.phone
        val telephoneCallMenu =
            if (!phoneNum.isNullOrEmpty()) {
                val phoneUri = Uri.parse("tel:$phoneNum")
                menu {
                    title = getString(R.string.Phone_call)
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
            } else {
                null
            }
        val developerMenu =
            menu {
                title = getString(R.string.Developer)
                action = {
                    creator?.let {
                        if (it.userId == Session.getAccountId()) {
                            ProfileBottomSheetDialogFragment.newInstance()
                                .showNow(parentFragmentManager, TAG)
                        } else {
                            showUserBottom(parentFragmentManager, it)
                        }
                    }
                    dismiss()
                }
            }

        val list =
            menuList {
                menuGroup {
                    menu {
                        title = getString(R.string.Share_Contact)
                        action = {
                            forwardContact(u)
                        }
                    }
                }
                menuGroup {
                    menu {
                        title = getString(R.string.Shared_Media)
                        action = {
                            val callback = this@UserBottomSheetDialogFragment.sharedMediaCallback
                            if (callback != null) {
                                callback.invoke()
                            } else {
                                SharedMediaActivity.show(requireContext(), conversationId, false)
                            }
                            RxBus.publish(BotCloseEvent())
                            dismiss()
                        }
                    }
                    menu {
                        title = getString(R.string.Search_Conversation)
                        action = {
                            startSearchConversation()
                            RxBus.publish(BotCloseEvent())
                            dismiss()
                        }
                    }
                }
            }

        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.disappearing_message)
                    subtitle = conversation.notNullWithElse({ it.expireIn.getTimeInterval() }, "")
                    action = {
                        showDisappearing()
                    }
                }
            },
        )

        if (u.relationship == UserRelationship.FRIEND.name) {
            list.groups.add(
                menuGroup {
                    menu(muteMenu)
                    menu(editNameMenu)
                },
            )
        } else {
            list.groups.add(
                menuGroup {
                    menu(muteMenu)
                },
            )
        }

        if (u.isBot()) {
            if (telephoneCallMenu != null) {
                list.groups.add(
                    menuGroup {
                        menu(telephoneCallMenu)
                    },
                )
            }
        } else {
            list.groups.add(
                menuGroup {
                    menu(voiceCallMenu)
                    telephoneCallMenu?.let { menu(it) }
                },
            )
        }

        if (u.isBot()) {
            list.groups.add(
                menuGroup {
                    menu(developerMenu)
                    menu(transactionMenu)
                },
            )
        } else {
            list.groups.add(
                menuGroup {
                    menu(transactionMenu)
                },
            )
        }

        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.Groups_In_Common)
                    action = {
                        openGroupsInCommon()
                        RxBus.publish(BotCloseEvent())
                        dismiss()
                    }
                }
            },
        )

        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.Circles)
                    action = {
                        startCircleManager()
                        RxBus.publish(BotCloseEvent())
                        dismiss()
                    }
                    this.circleNames = circleNames
                }
            },
        )

        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.Add_to_Home_screen)
                    action = {
                        addShortcut()
                        dismiss()
                    }
                }
            },
        )

        when (u.relationship) {
            UserRelationship.BLOCKING.name -> {
                list.groups.add(
                    menuGroup {
                        menu {
                            title = getString(R.string.Unblock)
                            style = MenuStyle.Danger
                            action = {
                                bottomViewModel.updateRelationship(
                                    RelationshipRequest(
                                        u.userId,
                                        RelationshipAction.UNBLOCK.name,
                                    ),
                                )
                            }
                        }
                        menu(clearMenu)
                    },
                )
            }
            UserRelationship.FRIEND.name -> {
                list.groups.add(
                    menuGroup {
                        menu {
                            title =
                                getString(
                                    if (user.isBot()) {
                                        R.string.Remove_Bot
                                    } else {
                                        R.string.Remove_Contact
                                    },
                                )

                            style = MenuStyle.Danger
                            action = {
                                requireContext().showConfirmDialog(
                                    getString(
                                        if (user.isBot()) {
                                            R.string.Remove_Bot
                                        } else {
                                            R.string.Remove_Contact
                                        },
                                    ),
                                ) {
                                    updateRelationship(UserRelationship.STRANGER.name)
                                    if (user.isBot()) {
                                        RxBus.publish(BotEvent())
                                    }
                                }
                            }
                        }
                        menu(clearMenu)
                    },
                )
            }
            UserRelationship.STRANGER.name -> {
                list.groups.add(
                    menuGroup {
                        menu {
                            title = getString(R.string.Block)
                            style = MenuStyle.Danger
                            action = {
                                requireContext().showConfirmDialog(getString(R.string.Block)) {
                                    bottomViewModel.updateRelationship(
                                        RelationshipRequest(
                                            u.userId,
                                            RelationshipAction.BLOCK.name,
                                        ),
                                    )
                                    if (user.isBot()) {
                                        RxBus.publish(BotEvent())
                                    }
                                }
                            }
                        }
                        menu(clearMenu)
                    },
                )
            }
        }
        list.groups.add(
            menuGroup {
                menu {
                    title = getString(R.string.Report)
                    style = MenuStyle.Danger
                    action = {
                        reportUser(u.userId)
                    }
                }
            },
        )

        menuListLayout?.removeAllViews()
        binding.scrollContent.removeView(menuListLayout)
        list.createMenuLayout(requireContext()).let { layout ->
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

    private fun startSearchConversation() =
        lifecycleScope.launch {
            bottomViewModel.getConversation(conversationId)?.let {
                val searchMessageItem =
                    if (it.category == ConversationCategory.CONTACT.name) {
                        SearchMessageItem(
                            it.conversationId,
                            it.category,
                            null,
                            0,
                            user.userId,
                            user.fullName,
                            user.avatarUrl,
                            null,
                        )
                    } else {
                        SearchMessageItem(
                            it.conversationId,
                            it.category,
                            it.name,
                            0,
                            "",
                            null,
                            null,
                            it.iconUrl,
                        )
                    }
                activity?.addFragment(
                    this@UserBottomSheetDialogFragment,
                    SearchMessageFragment.newInstance(searchMessageItem, ""),
                    SearchMessageFragment.TAG,
                )
            }
        }

    private fun forwardContact(u: User) {
        ForwardActivity.show(
            requireContext(),
            arrayListOf(
                ForwardMessage(
                    ShareCategory.Contact,
                    GsonHelper.customGson.toJson(ContactMessagePayload(u.userId)),
                ),
            ),
            ForwardAction.App.Resultless(),
        )
        RxBus.publish(BotCloseEvent())
        dismiss()
    }

    private fun openGroupsInCommon() {
        activity?.addFragment(
            this@UserBottomSheetDialogFragment,
            GroupsInCommonFragment.newInstance(user.userId),
            GroupsInCommonFragment.TAG,
        )
    }

    private fun startCircleManager() {
        activity?.addFragment(
            this@UserBottomSheetDialogFragment,
            CircleManagerFragment.newInstance(user.fullName, userId = user.userId),
            CircleManagerFragment.TAG,
        )
    }

    @SuppressLint("CheckResult")
    private fun startVoiceCall() {
        if (callState.isNotIdle()) {
            if (callState.user?.userId == user.userId) {
                CallActivity.show(requireContext())
            } else {
                alertDialogBuilder()
                    .setMessage(getString(R.string.call_on_another_call_hint))
                    .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        } else {
            RxPermissions(requireActivity())
                .request(Manifest.permission.RECORD_AUDIO)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            callVoice()
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {
                    },
                )
        }
    }

    private fun callVoice() {
        if (LinkState.isOnline(linkState.state)) {
            outgoingCall(
                requireContext(),
                conversationId,
                user,
            )
            RxBus.publish(BotCloseEvent())
            dismiss()
        } else {
            toast(R.string.No_network_connection)
        }
    }

    private fun reportUser(userId: String) {
        alertDialogBuilder()
            .setMessage(getString(R.string.Report_and_block))
            .setNeutralButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.Report)) { dialog, _ ->
                bottomViewModel.updateRelationship(
                    RelationshipRequest(
                        userId,
                        RelationshipAction.BLOCK.name,
                    ),
                    true,
                )
                if (user.isBot()) {
                    RxBus.publish(BotEvent())
                }
                dialog.dismiss()
                dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDisappearing() {
        dismiss()
        activity?.addFragment(
            this,
            DisappearingFragment.newInstance(conversationId, user.userId),
            DisappearingFragment.TAG,
        )
    }

    private fun updateUserInfo(user: User) =
        lifecycleScope.launch {
            if (!isAdded) return@launch

            binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            binding.name.text = user.fullName
            binding.idTv.text = getString(R.string.contact_mixin_id, user.identityNumber)
            binding.idTv.setOnLongClickListener {
                context?.getClipboardManager()
                    ?.setPrimaryClip(ClipData.newPlainText(null, user.identityNumber))
                toast(R.string.copied_to_clipboard)
                true
            }
            if (user.biography.isNotEmpty()) {
                binding.detailTv.originalText = user.biography
                binding.detailTv.visibility = VISIBLE
                binding.detailTv.heightDifferenceCallback = { heightDifference, duration ->
                    if (behavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        behavior?.peekHeight?.let { peekHeight ->
                            ValueAnimator.ofInt(peekHeight, peekHeight + heightDifference).apply {
                                interpolator = FastOutSlowInInterpolator()
                                setDuration(duration)
                                addUpdateListener { value ->
                                    behavior?.peekHeight = value.animatedValue as Int
                                }
                                start()
                            }
                        }
                    } else if (behavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                        behavior?.peekHeight?.let { peekHeight ->
                            behavior?.peekHeight = heightDifference + peekHeight
                        }
                    }
                }
            } else {
                binding.detailTv.visibility = GONE
            }
            updateUserStatus(user.relationship)
            user.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
            binding.opLl.isVisible = true
            if (user.isBot()) {
                binding.openFl.visibility = VISIBLE
                binding.transferFl.visibility = GONE
                binding.shareFl.isVisible = false
                bottomViewModel.findAppById(user.appId!!)?.let { app ->
                    binding.openFl.clicks()
                        .observeOn(AndroidSchedulers.mainThread())
                        .throttleFirst(1, TimeUnit.SECONDS)
                        .autoDispose(stopScope).subscribe {
                            dismiss()
                            RxBus.publish(BotCloseEvent())
                            WebActivity.show(requireActivity(), app.homeUri, botConversationId, app)
                            dismiss()
                        }
                    bottomViewModel.findUserById(app.creatorId)
                        .observe(
                            this@UserBottomSheetDialogFragment,
                        ) { u ->
                            creator = u
                            if (u == null) {
                                bottomViewModel.refreshUser(app.creatorId, true)
                            }
                        }
                }
            } else if (user.isDeactivated == true) {
                binding.transferFl.isVisible = false
                binding.openFl.isVisible = false
                binding.shareFl.isVisible = true
            } else {
                binding.openFl.visibility = GONE
                binding.transferFl.visibility = VISIBLE
                binding.shareFl.isVisible = false
            }
        }

    private val blockDrawable: Drawable by lazy {
        val d = requireNotNull(ResourcesCompat.getDrawable(resources, R.drawable.ic_bottom_block, context?.theme))
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        d
    }

    private fun updateUserStatus(relationship: String) {
        if (!isAdded) return

        if (user.isDeactivated == true) {
            binding.addTv.isVisible = false
            binding.detailTv.isVisible = false
            binding.deletedTv.isVisible = true
            return
        }

        when (relationship) {
            UserRelationship.BLOCKING.name -> {
                binding.addTv.visibility = VISIBLE
                binding.addTv.text = getString(R.string.Unblock)
                binding.addTv.setCompoundDrawables(blockDrawable, null, null, null)
                binding.addTv.setOnClickListener {
                    bottomViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.UNBLOCK.name,
                        ),
                    )
                    if (user.isBot()) {
                        RxBus.publish(BotEvent())
                    }
                }
            }
            UserRelationship.FRIEND.name -> {
                binding.addTv.visibility = GONE
            }
            UserRelationship.STRANGER.name -> {
                binding.addTv.visibility = VISIBLE
                binding.addTv.setCompoundDrawables(null, null, null, null)
                "+ ${getString(
                    if (user.isBot()) {
                        R.string.Add_bot
                    } else {
                        R.string.Add_Contact
                    },
                )}".also { binding.addTv.text = it }
                binding.addTv.setOnClickListener {
                    updateRelationship(UserRelationship.FRIEND.name)
                    if (user.isBot()) {
                        RxBus.publish(BotEvent())
                    }
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
            titleText = this@UserBottomSheetDialogFragment.getString(R.string.Edit_Name)
            editText = name
            maxTextCount = 40
            allowEmpty = false
            rightAction = {
                bottomViewModel.updateRelationship(
                    RelationshipRequest(
                        user.userId,
                        RelationshipAction.UPDATE.name,
                        it,
                    ),
                )
            }
        }
    }

    private fun showMuteDialog() {
        val choices =
            arrayOf(
                getString(R.string.one_hour),
                resources.getQuantityString(R.plurals.Hour, 8, 8),
                getString(R.string.one_week),
                getString(R.string.one_year),
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
                                    senderId = it.userId,
                                    recipientId = user.userId,
                                )
                            },
                            successBlock = { response ->
                                bottomViewModel.updateMuteUntil(user.userId, response.data!!.muteUntil)
                                toast(getString(R.string.contact_mute_title) + " ${user.fullName} " + choices[whichItem])
                            },
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

    private fun mute() {
        showMuteDialog()
    }

    private fun unMute() {
        val account = Session.getAccount()
        account?.let {
            lifecycleScope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        bottomViewModel.mute(0, senderId = it.userId, recipientId = user.userId)
                    },
                    successBlock = { response ->
                        bottomViewModel.updateMuteUntil(user.userId, response.data!!.muteUntil)
                        toast(getString(R.string.Unmute) + " ${user.fullName}")
                    },
                )
            }
        }
    }

    private fun updateRelationship(relationship: String) =
        lifecycleScope.launch {
            if (!isAdded) return@launch

            updateUserStatus(relationship)
            val request =
                RelationshipRequest(
                    user.userId,
                    if (relationship == UserRelationship.FRIEND.name) {
                        RelationshipAction.ADD.name
                    } else {
                        RelationshipAction.REMOVE.name
                    },
                    user.fullName,
                )
            bottomViewModel.updateRelationship(request)
        }

    private fun addShortcut() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.INSTALL_SHORTCUT)
            .autoDispose(stopScope)
            .subscribe(
                { granted ->
                    if (granted) {
                        addShortcutInternal()
                    } else {
                        context?.openPermissionSetting()
                    }
                },
                {
                    Timber.e(it)
                },
            )
    }

    private fun addShortcutInternal() {
        Glide.with(requireContext())
            .asBitmap()
            .load(user.avatarUrl)
            .listener(
                object : RequestListener<Bitmap> {
                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        user.fullName?.let {
                            val conversationId = conversationId
                            addPinShortcut(
                                requireContext(),
                                conversationId,
                                it,
                                resource,
                                ConversationActivity.getShortcutIntent(
                                    requireContext(),
                                    conversationId,
                                    user.userId,
                                ),
                            )
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        return false
                    }
                },
            ).submit()
    }

    private fun exportChat() {
        lifecycleScope.launch {
            val backupFile = File("${requireContext().getOtherPath().absolutePath}${File.separator}${user.fullName}-chats.txt")
            bottomViewModel.exportChat(conversationId, backupFile)
        }
    }

    override fun onStateChanged(
        bottomSheet: View,
        newState: Int,
    ) {
        when (newState) {
            BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
            BottomSheetBehavior.STATE_COLLAPSED -> binding.moreIv.rotationX = 0f
            BottomSheetBehavior.STATE_EXPANDED -> binding.moreIv.rotationX = 180f
        }
    }
}

fun showUserBottom(
    fragmentManager: FragmentManager,
    user: User,
    conversationId: String? = null,
    sharedMediaCallback: (() -> Unit)? = null,
) {
    if (fragmentManager.isStateSaved) return

    if (user.notMessengerUser()) {
        NonMessengerUserBottomSheetDialogFragment.newInstance(user, conversationId)
            .showNow(fragmentManager, NonMessengerUserBottomSheetDialogFragment.TAG)
    } else {
        UserBottomSheetDialogFragment.newInstance(user, conversationId)?.apply {
            sharedMediaCallback?.let {
                this.sharedMediaCallback = sharedMediaCallback
            }
        }?.showNow(fragmentManager, UserBottomSheetDialogFragment.TAG)
    }
}
