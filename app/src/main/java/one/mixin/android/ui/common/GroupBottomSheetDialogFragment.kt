@file:Suppress("OVERRIDE_DEPRECATION")

package one.mixin.android.ui.common

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.uber.autodispose.autoDispose
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
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
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.databinding.FragmentGroupBottomSheetBinding
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.dayTime
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getCapturedImage
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.localTime
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showConfirmDialog
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.info.MenuStyle
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menu
import one.mixin.android.ui.common.info.menuGroup
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment.Companion.MAX_PHOTO_SIZE
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.group.GroupActivity.Companion.ARGS_EXPAND
import one.mixin.android.ui.media.SharedMediaActivity
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.widget.picker.getTimeInterval
import org.threeten.bp.Instant
import java.io.File

@AndroidEntryPoint
class GroupBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GroupBottomSheetDialogFragment"
        private const val STATE_IMAGE_URI = "state_image_uri"

        @SuppressLint("StaticFieldLeak")
        private var instant: GroupBottomSheetDialogFragment? = null

        fun newInstance(
            conversationId: String,
            expand: Boolean = false,
        ): GroupBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return GroupBottomSheetDialogFragment().apply {
                arguments =
                    Bundle().apply {
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
    var sharedMediaCallback: (() -> Unit)? = null

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }
    private lateinit var conversation: Conversation
    private var me: Participant? = null
    private var photoUri: Uri? = null
    private val imageUri: Uri
        get() = photoUri ?: Uri.fromFile(requireContext().getOtherPath().createImageTemp()).also { photoUri = it }
    private var uploadDialog: Dialog? = null
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && isAdded) cropPhoto(uri)
    }

    private var menuListLayout: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoUri = savedInstanceState?.getString(STATE_IMAGE_URI)?.let(Uri::parse)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_IMAGE_URI, photoUri?.toString())
    }

    override fun onDestroyView() {
        uploadDialog?.dismiss()
        uploadDialog = null
        super.onDestroyView()
    }

    override fun getLayoutId() = R.layout.fragment_group_bottom_sheet

    private val binding by lazy {
        FragmentGroupBottomSheetBinding.bind(contentView)
    }

    override fun getPeekHeight(
        contentView: View,
        behavior: BottomSheetBehavior<*>,
    ): Int = calculateCollapsedContentHeight()

    override fun shouldApplyBottomInsetToBottomSheetContainer(): Boolean = false

    override fun shouldIncludeBottomInsetInPeekHeight(): Boolean = false

    override fun extraPeekOffsetWhenNavigationBarPresent(): Int = 5.dp

    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        binding.title.rightIv.setOnClickListener { dismiss() }
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

                val changeMenu =
                    menuListLayout == null ||
                        c.muteUntil != conversation.muteUntil
                conversation = c
                val icon = c.iconUrl
                binding.avatar.setGroup(icon)
                debugLongClick(
                    binding.avatar,
                    {
                        context?.getClipboardManager()
                            ?.setPrimaryClip(ClipData.newPlainText(null, conversationId))
                    },
                )
                if (icon.isNullOrBlank() || !icon.isWebUrl() && !File(icon).exists()) {
                    bottomViewModel.startGenerateAvatar(c.conversationId)
                }
                renderCollapsedContent(c)
                initParticipant(changeMenu, c)
            },
        )

        bottomViewModel.refreshConversation(conversationId)
    }

    @SuppressLint("SetTextI18n")
    private fun initParticipant(
        changeMenu: Boolean,
        conversation: Conversation,
    ) = lifecycleScope.launch {
        if (!isAdded) return@launch

        var participantCount: Int
        var localMe: Participant?
        withContext(Dispatchers.IO) {
            localMe = bottomViewModel.findParticipantById(conversationId, Session.getAccountId()!!)
            participantCount = bottomViewModel.getParticipantsCount(conversationId)
        }
        if (!isAdded) return@launch

        binding.countTv.text = requireContext().resources.getQuantityString(R.plurals.title_participants, participantCount, participantCount)
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
            binding.memberFl.setOnClickListener {
                GroupActivity.show(requireContext(), GroupActivity.INFO, conversationId)
                dismiss()
            }
            binding.memberTv.isVisible = false
        } else {
            val withoutCode = conversation.status == ConversationStatus.QUIT.ordinal
            binding.scrollView.isEnabled = withoutCode
            binding.opsLl.isVisible = withoutCode
            binding.memberIv.setImageResource(R.drawable.ic_search_home)
            binding.memberFl.setOnClickListener {
                startSearchConversation()
                dismiss()
            }
            binding.memberTv.isVisible = true
        }

        updateCollapsedPeekHeight()
    }

    private fun initMenu(
        me: Participant?,
        circleNames: List<String>,
    ) {
        if (!isAdded) return

        val list =
            menuList {
                menuGroup {
                    menu {
                        title = getString(R.string.Shared_Media)
                        action = {
                            val callback = this@GroupBottomSheetDialogFragment.sharedMediaCallback
                            if (callback != null) {
                                callback.invoke()
                            } else {
                                SharedMediaActivity.show(requireContext(), conversationId, false)
                            }
                            dismiss()
                        }
                    }
                    menu {
                        title = getString(R.string.Search_Conversation)
                        action = {
                            startSearchConversation()
                            dismiss()
                        }
                    }
                }
            }

        if (me != null) {
            if (me.role == ParticipantRole.OWNER.name || me.role == ParticipantRole.ADMIN.name) {
                val announcementString =
                    if (TextUtils.isEmpty(conversation.announcement)) {
                        getString(R.string.Add_group_description)
                    } else {
                        getString(R.string.Edit_Group_Description)
                    }
                list.groups.add(
                    menuGroup {
                        menu {
                            title = announcementString
                            action = {
                                editDialog {
                                    titleText = this@GroupBottomSheetDialogFragment.getString(R.string.Edit_Group_Description)
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
                            title = getString(R.string.Edit_group_name)
                            action = {
                                editDialog {
                                    titleText = this@GroupBottomSheetDialogFragment.getString(R.string.Edit_group_name)
                                    editText = conversation.name
                                    maxTextCount = 40
                                    allowEmpty = false
                                    rightAction = {
                                        bottomViewModel.updateGroup(conversationId, it, null)
                                    }
                                }
                            }
                        }
                    },
                )

                list.groups.add(
                    menuGroup {
                        menu {
                            title = getString(R.string.disappearing_message)
                            subtitle = conversation.expireIn.getTimeInterval()
                            action = {
                                showDisappearing()
                            }
                        }
                    },
                )
                list.groups.add(
                    menuGroup {
                        menu {
                            title = getString(R.string.Change_Group_Photo_with_Camera)
                            action = { changePhoto(true) }
                        }
                        menu {
                            title = getString(R.string.Change_Group_Photo_with_Library)
                            action = { changePhoto(false) }
                        }
                    },
                )
            }
            val muteMenu =
                if (conversation.muteUntil.notNullWithElse(
                        {
                            Instant.now().isBefore(Instant.parse(it))
                        },
                        false,
                    )
                ) {
                    menu {
                        title = getString(R.string.Unmute)
                        subtitle = getString(R.string.Mute_until, conversation.muteUntil?.localTime())
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
                },
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
            },
        )

        val deleteMenu =
            if (me != null) {
                menu {
                    title = getString(R.string.Exit_Group)
                    style = MenuStyle.Danger
                    action = {
                        requireContext().showConfirmDialog(getString(R.string.Exit_Group)) {
                            bottomViewModel.exitGroup(conversationId)
                            dismiss()
                        }
                    }
                }
            } else {
                menu {
                    title = getString(R.string.Delete_Group)
                    style = MenuStyle.Danger
                    action = {
                        requireContext().showConfirmDialog(getString(R.string.Delete_Group)) {
                            bottomViewModel.deleteConversation(conversationId)
                            callback?.onDelete()
                        }
                    }
                }
            }
        list.groups.add(
            menuGroup {
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
                menu(deleteMenu)
            },
        )

        binding.menuContainer.removeAllViews()
        list.createMenuLayout(
            requireContext(),
            getString(R.string.Created, conversation.createdAt.dayTime()),
        )
            .let { layout ->
                menuListLayout = layout
                binding.menuContainer.addView(layout)
                updateCollapsedPeekHeight()
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

    private fun renderCollapsedContent(conversation: Conversation) {
        if (!isAdded) return

        binding.name.text = conversation.name
        if (conversation.announcement.isNullOrBlank()) {
            binding.detailTv.isVisible = false
        } else {
            binding.detailTv.isVisible = true
            binding.detailTv.originalText = conversation.announcement
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
        }
        updateCollapsedPeekHeight()
    }

    private fun changePhoto(byCamera: Boolean) {
        if (!byCamera) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            RxPermissions(requireActivity())
                .request(
                    *mutableListOf(Manifest.permission.CAMERA).apply {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }.toTypedArray(),
                )
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        openCamera(imageUri)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
    }

    private fun cropPhoto(uri: Uri) {
        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            setToolbarWidgetColor(Color.WHITE)
            setHideBottomControls(true)
        }
        UCrop.of(uri, imageUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
            .start(requireContext(), this)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isAdded) return

        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            cropPhoto(imageUri)
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK && data != null) {
            UCrop.getOutput(data)?.let { updatePhoto(it) }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == UCrop.RESULT_ERROR && data != null) {
            UCrop.getError(data)?.let(ErrorHandler::handleError)
        }
    }

    private fun updatePhoto(uri: Uri) = lifecycleScope.launch {
        if (!isAdded || uploadDialog != null) return@launch

        val accountId = Session.getAccountId() ?: return@launch
        val resolver = requireContext().contentResolver
        val progress = indeterminateProgressDialog(message = R.string.Please_wait_a_bit) { setCancelable(false) }
        uploadDialog = progress
        try {
            val participant = withContext(Dispatchers.IO) {
                bottomViewModel.findParticipantById(conversationId, accountId)
            }
            if (participant?.role != ParticipantRole.OWNER.name && participant?.role != ParticipantRole.ADMIN.name) {
                toast(R.string.Access_denied)
                return@launch
            }
            handleMixinResponse<ConversationResponse, Unit>(
                invokeNetwork = {
                    val bitmap = uri.getCapturedImage(resolver)
                    val iconBase64 = try {
                        Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
                    } finally {
                        bitmap.recycle()
                    }
                    bottomViewModel.updateGroupIcon(conversationId, iconBase64)
                },
                exceptionBlock = {
                    if (it is CancellationException) throw it
                    false
                },
            )
        } finally {
            progress.dismiss()
            uploadDialog = null
        }
    }

    private fun updateCollapsedPeekHeight() {
        schedulePeekHeightUpdate()
    }

    private fun calculateCollapsedContentHeight(): Int {
        val titleHeight = binding.title.height.takeIf { it > 0 } ?: measureCollapsedViewHeight(binding.title)
        val topContentHeight = binding.topContent.height.takeIf { it > 0 } ?: measureCollapsedViewHeight(binding.topContent)
        return titleHeight + topContentHeight
    }

    private fun measureCollapsedViewHeight(view: View): Int {
        val width = contentView.measuredWidth.takeIf { it > 0 } ?: contentView.width
        if (width <= 0) return 0

        val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        return view.measuredHeight
    }

    private fun startSearchConversation() =
        lifecycleScope.launch {
            bottomViewModel.getConversation(conversationId)?.let {
                val searchMessageItem =
                    SearchMessageItem(
                        it.conversationId,
                        it.category,
                        it.name,
                        0,
                        "",
                        null,
                        null,
                        null,
                        null,
                        it.iconUrl,
                        false,
                        null
                    )
                activity?.addFragment(
                    this@GroupBottomSheetDialogFragment,
                    SearchMessageFragment.newInstance(searchMessageItem, ""),
                    SearchMessageFragment.TAG,
                )
            }
        }

    private fun startCircleManager() {
        activity?.addFragment(
            this,
            CircleManagerFragment.newInstance(conversation.name, conversationId = conversation.conversationId),
            CircleManagerFragment.TAG,
        )
    }

    private fun showDisappearing() {
        dismiss()
        activity?.addFragment(
            this,
            DisappearingFragment.newInstance(conversation.conversationId),
            DisappearingFragment.TAG,
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
                    },
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
                                    conversationId = conversationId,
                                )
                            },
                            successBlock = { response ->
                                bottomViewModel.updateGroupMuteUntil(conversationId, response.data!!.muteUntil)
                                toast(getString(R.string.contact_mute_title) + " ${conversation.name} " + choices[whichItem])
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

    interface Callback {
        fun onDelete()
    }
}
