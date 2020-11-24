package one.mixin.android.ui.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions2.RxPermissions
import com.twilio.audioswitch.AudioSwitch
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_delete.view.*
import kotlinx.android.synthetic.main.fragment_conversation.*
import kotlinx.android.synthetic.main.view_chat_control.view.*
import kotlinx.android.synthetic.main.view_flag.view.*
import kotlinx.android.synthetic.main.view_reply.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_tool.view.*
import kotlinx.android.synthetic.main.view_url_bottom.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.event.BlinkEvent
import one.mixin.android.event.CallEvent
import one.mixin.android.event.DragReleaseEvent
import one.mixin.android.event.ExitEvent
import one.mixin.android.event.GroupEvent
import one.mixin.android.event.MentionReadEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.REQUEST_FILE
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isBluetoothHeadsetOrWiredHeadset
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.lateOneHours
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.safeActivate
import one.mixin.android.extension.safeStop
import one.mixin.android.extension.scamPreferences
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.selectDocument
import one.mixin.android.extension.selectEarpiece
import one.mixin.android.extension.selectSpeakerphone
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.job.FavoriteAppJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.media.OpusAudioRecorder
import one.mixin.android.media.OpusAudioRecorder.Companion.STATE_NOT_INIT
import one.mixin.android.media.OpusAudioRecorder.Companion.STATE_RECORDING
import one.mixin.android.session.Session
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment.Companion.GROUP_VOICE_MAX_COUNT
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.LinkFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.ui.conversation.adapter.MentionAdapter.OnUserClickListener
import one.mixin.android.ui.conversation.adapter.Menu
import one.mixin.android.ui.conversation.adapter.MenuType
import one.mixin.android.ui.conversation.chat.ChatItemCallback
import one.mixin.android.ui.conversation.chat.ChatItemCallback.Companion.SWAP_SLOT
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.location.LocationActivity
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.sticker.StickerActivity
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.util.Attachment
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mention.mentionDisplay
import one.mixin.android.util.mention.mentionEnd
import one.mixin.android.util.mention.mentionReplace
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.AppItem
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.canNotForward
import one.mixin.android.vo.canNotReply
import one.mixin.android.vo.canRecall
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Image
import one.mixin.android.vo.isLive
import one.mixin.android.vo.saveToLocal
import one.mixin.android.vo.supportSticker
import one.mixin.android.vo.toApp
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.webrtc.TAG_AUDIO
import one.mixin.android.webrtc.anyCallServiceRunning
import one.mixin.android.webrtc.checkPeers
import one.mixin.android.webrtc.outgoingCall
import one.mixin.android.webrtc.receiveInvite
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetItem
import one.mixin.android.widget.ChatControlView
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_UP
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.buildBottomSheetView
import one.mixin.android.widget.gallery.ui.GalleryActivity.Companion.IS_VIDEO
import one.mixin.android.widget.keyboard.KeyboardAwareLinearLayout.OnKeyboardHiddenListener
import one.mixin.android.widget.keyboard.KeyboardAwareLinearLayout.OnKeyboardShownListener
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
@SuppressLint("InvalidWakeLockTag")
class ConversationFragment() :
    LinkFragment(),
    OnKeyboardShownListener,
    OnKeyboardHiddenListener,
    OpusAudioRecorder.Callback,
    SensorEventListener,
    AudioPlayer.StatusListener {

    companion object {
        const val TAG = "ConversationFragment"

        const val CONVERSATION_ID = "conversation_id"
        const val RECIPIENT_ID = "recipient_id"
        const val RECIPIENT = "recipient"
        const val MESSAGE_ID = "message_id"
        const val INITIAL_POSITION_MESSAGE_ID = "initial_position_message_id"
        const val UNREAD_COUNT = "unread_count"
        private const val KEY_WORD = "key_word"

        fun putBundle(
            conversationId: String?,
            recipientId: String?,
            messageId: String?,
            keyword: String?,
            unreadCount: Int? = null
        ): Bundle =
            Bundle().apply {
                require(!(conversationId == null && recipientId == null)) { "lose data" }
                messageId?.let {
                    putString(MESSAGE_ID, messageId)
                }
                keyword?.let {
                    putString(KEY_WORD, keyword)
                }
                putString(CONVERSATION_ID, conversationId)
                putString(RECIPIENT_ID, recipientId)
                unreadCount?.let {
                    putInt(UNREAD_COUNT, unreadCount)
                }
            }

        fun newInstance(bundle: Bundle) = ConversationFragment().apply { arguments = bundle }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun newInstance(bundle: Bundle, testRegistry: ActivityResultRegistry) = ConversationFragment(testRegistry).apply { arguments = bundle }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var callState: CallStateLiveData

    @Inject
    lateinit var audioSwitch: AudioSwitch

    private val chatViewModel by viewModels<ConversationViewModel>()

    private var unreadTipCount: Int = 0
    private val chatAdapter: ConversationAdapter by lazy {
        ConversationAdapter(requireActivity(), keyword, onItemListener, isGroup, !isPlainMessage(), isBot).apply {
            registerAdapterDataObserver(chatAdapterDataObserver)
        }
    }

    private val chatAdapterDataObserver =
        object : RecyclerView.AdapterDataObserver() {
            var oldSize = 0

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                chatAdapter.currentList?.let {
                    oldSize = it.size
                }
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!isAdded) return

                when {
                    isFirstLoad -> {
                        isFirstLoad = false
                        chatViewModel.viewModelScope.launch {
                            delay(100)
                            messageId?.let { id ->
                                RxBus.publish(BlinkEvent(id))
                            }
                        }
                        if (context?.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                            ?.getBoolean(conversationId, false) == true
                        ) {
                            chatViewModel.viewModelScope.launch {
                                group_desc.text = chatViewModel.getAnnouncementByConversationId(conversationId)
                                group_desc.collapse()
                                group_desc.requestFocus()
                            }
                            group_flag.isVisible = true
                            driver.isVisible = true
                        }
                        val position = if (messageId != null) {
                            unreadCount + 1
                        } else {
                            unreadCount
                        }
                        if (position >= itemCount - 1) {
                            (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                itemCount - 1,
                                0
                            )
                        } else {
                            (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                position,
                                chat_rv.measuredHeight * 3 / 4
                            )
                        }
                        chat_rv.isVisible = true
                    }
                    isBottom -> {
                        if (chatAdapter.currentList != null && chatAdapter.currentList!!.size > oldSize) {
                            chat_rv.layoutManager?.scrollToPosition(0)
                        }
                    }
                    else -> {
                        if (unreadTipCount > 0) {
                            flag_layout.bottomCountFlag = true
                            flag_layout.unreadCount = unreadTipCount
                        } else {
                            flag_layout.bottomCountFlag = false
                        }
                    }
                }
                chatAdapter.currentList?.let {
                    oldSize = it.size
                }
            }
        }

    private fun voiceCall() {
        if (LinkState.isOnline(linkState.state)) {
            if (isGroup) {
                if (callState.getGroupCallStateOrNull(conversationId) != null) {
                    receiveInvite(requireContext(), conversationId, playRing = false)
                } else {
                    GroupUsersBottomSheetDialogFragment.newInstance(conversationId)
                        .showNow(parentFragmentManager, GroupUsersBottomSheetDialogFragment.TAG)
                }
            } else {
                createConversation {
                    outgoingCall(requireContext(), conversationId, recipient!!)
                }
            }
        } else {
            toast(R.string.error_no_connection)
        }
    }

    private val onItemListener: ConversationAdapter.OnItemListener by lazy {
        object : ConversationAdapter.OnItemListener() {
            override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
                if (isSelect) {
                    chatAdapter.addSelect(messageItem)
                } else {
                    chatAdapter.removeSelect(messageItem)
                }
                tool_view.count_tv.text = chatAdapter.selectSet.size.toString()
                when {
                    chatAdapter.selectSet.isEmpty() -> tool_view.fadeOut()
                    chatAdapter.selectSet.size == 1 -> {
                        try {
                            if (chatAdapter.selectSet.valueAt(0)?.type == MessageCategory.SIGNAL_TEXT.name ||
                                chatAdapter.selectSet.valueAt(0)?.type == MessageCategory.PLAIN_TEXT.name
                            ) {
                                tool_view.copy_iv.visibility = VISIBLE
                            } else {
                                tool_view.copy_iv.visibility = GONE
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            tool_view.copy_iv.visibility = GONE
                        }
                        if (chatAdapter.selectSet.valueAt(0)?.supportSticker() == true) {
                            tool_view.add_sticker_iv.visibility = VISIBLE
                        } else {
                            tool_view.add_sticker_iv.visibility = GONE
                        }
                        if (chatAdapter.selectSet.valueAt(0)?.canNotReply() == true) {
                            tool_view.reply_iv.visibility = GONE
                        } else {
                            tool_view.reply_iv.visibility = VISIBLE
                        }
                    }
                    else -> {
                        tool_view.forward_iv.visibility = VISIBLE
                        tool_view.reply_iv.visibility = GONE
                        tool_view.copy_iv.visibility = GONE
                        tool_view.add_sticker_iv.visibility = GONE
                    }
                }
                if (chatAdapter.selectSet.find { it.canNotForward() } != null) {
                    tool_view.forward_iv.visibility = GONE
                } else {
                    tool_view.forward_iv.visibility = VISIBLE
                }
                chatAdapter.notifyDataSetChanged()
            }

            override fun onLongClick(messageItem: MessageItem, position: Int): Boolean {
                val b = chatAdapter.addSelect(messageItem)
                tool_view.count_tv.text = chatAdapter.selectSet.size.toString()
                if (b) {
                    if (messageItem.type == MessageCategory.SIGNAL_TEXT.name ||
                        messageItem.type == MessageCategory.PLAIN_TEXT.name
                    ) {
                        tool_view.copy_iv.visibility = VISIBLE
                    } else {
                        tool_view.copy_iv.visibility = GONE
                    }

                    if (messageItem.supportSticker()) {
                        tool_view.add_sticker_iv.visibility = VISIBLE
                    } else {
                        tool_view.add_sticker_iv.visibility = GONE
                    }

                    if (chatAdapter.selectSet.find { it.canNotForward() } != null) {
                        tool_view.forward_iv.visibility = GONE
                    } else {
                        tool_view.forward_iv.visibility = VISIBLE
                    }
                    if (chatAdapter.selectSet.find { it.canNotReply() } != null) {
                        tool_view.reply_iv.visibility = GONE
                    } else {
                        tool_view.reply_iv.visibility = VISIBLE
                    }
                    chatAdapter.notifyDataSetChanged()
                    tool_view.fadeIn()
                }
                return b
            }

            @SuppressLint("MissingPermission")
            override fun onRetryDownload(messageId: String) {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                chatViewModel.retryDownload(messageId)
                            } else {
                                context?.openPermissionSetting()
                            }
                        },
                        {
                        }
                    )
            }

            @SuppressLint("MissingPermission")
            override fun onRetryUpload(messageId: String) {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                chatViewModel.retryUpload(messageId) {
                                    toast(R.string.error_retry_upload)
                                }
                            } else {
                                context?.openPermissionSetting()
                            }
                        },
                        {
                        }
                    )
            }

            override fun onCancel(id: String) {
                chatViewModel.cancel(id)
            }

            override fun onAudioClick(messageItem: MessageItem) {
                when {
                    chat_control.isRecording -> showRecordingAlert()
                    AudioPlayer.isPlay(messageItem.messageId) -> AudioPlayer.pause()
                    else -> {
                        AudioPlayer.play(messageItem) {
                            chatViewModel.downloadAttachment(it)
                        }
                    }
                }
            }

            override fun onImageClick(messageItem: MessageItem, view: View) {
                starTransition = true
                if (messageItem.isLive()) {
                    MediaPagerActivity.show(
                        requireActivity(),
                        view,
                        messageItem.conversationId,
                        messageItem.messageId
                    )
                    return
                }
                val path = messageItem.mediaUrl?.toUri()?.getFilePath()
                if (path == null) {
                    toast(R.string.error_file_exists)
                    return
                }
                val file = File(path)
                if (file.exists()) {
                    MediaPagerActivity.show(
                        requireActivity(),
                        view,
                        messageItem.conversationId,
                        messageItem.messageId
                    )
                } else {
                    toast(R.string.error_file_exists)
                }
            }

            @TargetApi(Build.VERSION_CODES.O)
            override fun onFileClick(messageItem: MessageItem) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O &&
                    messageItem.mediaMimeType.equals(
                            "application/vnd.android.package-archive",
                            true
                        )
                ) {
                    if (requireContext().packageManager.canRequestPackageInstalls()) {
                        requireContext().openMedia(messageItem)
                    } else {
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                    }
                } else if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
                    showBottomSheet(messageItem)
                } else {
                    requireContext().openMedia(messageItem)
                }
            }

            override fun onAudioFileClick(messageItem: MessageItem) {
                if (!MimeTypes.isAudio(messageItem.mediaMimeType)) return
                when {
                    chat_control.isRecording -> showRecordingAlert()
                    AudioPlayer.isPlay(messageItem.messageId) -> AudioPlayer.pause()
                    else -> AudioPlayer.play(messageItem)
                }
            }

            override fun onUserClick(userId: String) {
                chatViewModel.getUserById(userId).autoDispose(stopScope).subscribe(
                    {
                        it?.let {
                            UserBottomSheetDialogFragment.newInstance(it, conversationId)
                                .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                    },
                    {
                        Timber.e(it)
                    }
                )
            }

            override fun onUrlClick(url: String) {
                url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
            }

            override fun onUrlLongClick(url: String) {
                val builder = BottomSheet.Builder(requireActivity())
                val view = View.inflate(
                    ContextThemeWrapper(requireActivity(), R.style.Custom),
                    R.layout.view_url_bottom,
                    null
                )
                builder.setCustomView(view)
                val bottomSheet = builder.create()
                view.url_tv.text = url
                view.open_tv.setOnClickListener {
                    url.openAsUrlOrWeb(
                        requireContext(),
                        conversationId,
                        parentFragmentManager,
                        lifecycleScope
                    )
                    bottomSheet.dismiss()
                }
                view.copy_tv.setOnClickListener {
                    requireContext().getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    requireContext().toast(R.string.copy_success)
                    bottomSheet.dismiss()
                }
                bottomSheet.show()
            }

            override fun onMentionClick(identityNumber: String) {
                chatViewModel.viewModelScope.launch {
                    chatViewModel.findUserByIdentityNumberSuspend(identityNumber)?.let { user ->
                        UserBottomSheetDialogFragment.newInstance(user, conversationId)
                            .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                    }
                }
            }

            override fun onAddClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.ADD.name,
                            user.fullName
                        )
                    )
                }
            }

            override fun onBlockClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.BLOCK.name,
                            user.fullName
                        )
                    )
                }
            }

            override fun onActionClick(action: String, userId: String) {
                if (openInputAction(action)) return

                lifecycleScope.launch {
                    val app = chatViewModel.findAppById(userId)
                    action.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope, app)
                }
            }

            override fun onAppCardClick(appCard: AppCardData, userId: String) {
                if (openInputAction(appCard.action)) return

                open(appCard.action, null, appCard)
            }

            override fun onBillClick(messageItem: MessageItem) {
                activity?.addFragment(
                    this@ConversationFragment,
                    TransactionFragment.newInstance(
                        assetId = messageItem.assetId,
                        snapshotId = messageItem.snapshotId
                    ),
                    TransactionFragment.TAG
                )
            }

            override fun onContactCardClick(userId: String) {
                if (userId == Session.getAccountId()) {
                    ProfileBottomSheetDialogFragment.newInstance().showNow(
                        parentFragmentManager,
                        UserBottomSheetDialogFragment.TAG
                    )
                    return
                }
                chatViewModel.getUserById(userId).autoDispose(stopScope).subscribe(
                    {
                        it?.let {
                            UserBottomSheetDialogFragment.newInstance(it, conversationId)
                                .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                    },
                    {
                        Timber.e(it)
                    }
                )
            }

            override fun onQuoteMessageClick(messageId: String, quoteMessageId: String?) {
                quoteMessageId?.let { quoteMsg ->
                    scrollToMessage(quoteMsg) {
                        positionBeforeClickQuote = messageId
                    }
                }
            }

            override fun onPostClick(view: View, messageItem: MessageItem) {
                MarkdownActivity.show(requireActivity(), messageItem.content!!, conversationId)
            }

            override fun onSayHi() {
                sendMessage("Hi")
            }

            override fun onOpenHomePage() {
                openBotHome()
            }

            override fun onLocationClick(messageItem: MessageItem) {
                val location = GsonHelper.customGson.fromJson(messageItem.content, LocationPayload::class.java)
                if (requireContext().isGooglePlayServicesAvailable()) {
                    LocationActivity.show(requireContext(), location)
                } else {
                    try {
                        requireActivity().startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.error_open_location)
                    }
                }
            }

            override fun onCallClick(messageItem: MessageItem) {
                if (callState.isNotIdle()) {
                    if (recipient != null && callState.user?.userId == recipient?.userId) {
                        CallActivity.show(requireContext())
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
                        .autoDispose(stopScope)
                        .subscribe(
                            { granted ->
                                if (granted) {
                                    voiceCall()
                                } else {
                                    context?.openPermissionSetting()
                                }
                            },
                            {
                            }
                        )
                }
            }
        }
    }

    private val decoration by lazy {
        MixinHeadersDecoration(chatAdapter)
    }

    private val mentionAdapter: MentionAdapter by lazy {
        MentionAdapter(
            object : OnUserClickListener {
                @SuppressLint("SetTextI18n")
                override fun onUserClick(user: User) {
                    val text = chat_control.chat_et.text ?: return
                    chat_control.chat_et.setText(mentionReplace(text, user))
                    chat_control.chat_et.setSelection(chat_control.chat_et.text!!.length)
                    mentionAdapter.submitList(null)
                    floating_layout.hideMention()
                }
            }
        )
    }

    private var imageUri: Uri? = null
    private fun createImageUri() = Uri.fromFile(context?.getOtherPath()?.createImageTemp())

    private val conversationId: String by lazy<String> {
        var cid = requireArguments().getString(CONVERSATION_ID)
        if (cid.isNullOrBlank()) {
            isFirstMessage = true
            cid = generateConversationId(sender.userId, recipient!!.userId)
        }
        cid
    }

    private var recipient: User? = null

    private val isGroup: Boolean by lazy {
        recipient == null
    }

    private val isBot: Boolean by lazy {
        recipient?.isBot() == true
    }

    private val messageId: String? by lazy {
        requireArguments().getString(MESSAGE_ID, null)
    }

    private val initialPositionMessageId: String? by lazy {
        requireArguments().getString(INITIAL_POSITION_MESSAGE_ID, null)
    }

    private val keyword: String? by lazy {
        requireArguments().getString(KEY_WORD, null)
    }

    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private var app: App? = null

    private var isFirstMessage = false
    private var isFirstLoad = true
    private var isBottom = true
        set(value) {
            field = value
            flag_layout.bottomFlag = !value
        }
    private var positionBeforeClickQuote: String? = null

    private var botWebBottomSheet: WebFragment? = null

    private val sensorManager: SensorManager by lazy {
        requireContext().getSystemService()!!
    }

    private val powerManager: PowerManager by lazy {
        requireContext().getSystemService()!!
    }

    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "mixin")
    }
    private val aodWakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "mixin"
        )
    }

    // for testing
    private lateinit var resultRegistry: ActivityResultRegistry

    // testing constructor
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        testRegistry: ActivityResultRegistry,
    ) : this() {
        resultRegistry = testRegistry
    }

    // for testing
    var selectItem: SelectItem? = null

    lateinit var getForwardResult: ActivityResultLauncher<Pair<ArrayList<ForwardMessage<ForwardCategory>>, String?>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getForwardResult = registerForActivityResult(ForwardActivity.ForwardContract(), resultRegistry, ::callbackForward)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!anyCallServiceRunning(requireContext())) {
            audioSwitch.start { audioDevices, selectedAudioDevice ->
                Timber.d("$TAG_AUDIO audioDevices: $audioDevices, selectedAudioDevice: $selectedAudioDevice")
            }
        }
        recipient = requireArguments().getParcelable(RECIPIENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        AudioPlayer.setStatusListener(this)
        RxBus.listen(ExitEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe {
                if (it.conversationId == conversationId) {
                    activity?.finish()
                }
            }
        RxBus.listen(MentionReadEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { event ->
                chatViewModel.viewModelScope.launch {
                    chatViewModel.markMentionRead(event.messageId, event.conversationId)
                }
            }
        RxBus.listen(CallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { event ->
                if (event.conversationId == conversationId) {
                    alertDialogBuilder()
                        .setMessage(getString(R.string.call_group_full, GROUP_VOICE_MAX_COUNT))
                        .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }

        checkPeerIfNeeded()
    }

    private var paused = false
    private var starTransition = false

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        input_layout.addOnKeyboardShownListener(this)
        input_layout.addOnKeyboardHiddenListener(this)
        MixinApplication.conversationId = conversationId
        if (isGroup) {
            RxBus.listen(GroupEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe {
                    if (it.conversationId == conversationId) {
                        chatViewModel.viewModelScope.launch {
                            group_desc.text = chatViewModel.getAnnouncementByConversationId(conversationId)
                            group_desc.collapse()
                            group_desc.requestFocus()
                        }
                        group_flag.isVisible = true
                        driver.isVisible = true
                    }
                }
        }
        if (paused) {
            paused = false
            chat_rv.adapter?.notifyDataSetChanged()
        }
        RxBus.listen(RecallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe { event ->
                if (chatAdapter.selectSet.any { it.messageId == event.messageId }) {
                    closeTool()
                }
                reply_view.messageItem?.let {
                    if (it.messageId == event.messageId) {
                        reply_view.fadeOut(isGone = true)
                        reply_view.messageItem = null
                    }
                }
            }
    }

    private var lastReadMessage: String? = null
    override fun onPause() {
        sensorManager.unregisterListener(this)
        lifecycleScope.launch {
            if (!isAdded) return@launch
            lastReadMessage = chatViewModel.findLastMessage(conversationId)
        }
        deleteDialog?.dismiss()
        super.onPause()
        paused = true
        input_layout.removeOnKeyboardShownListener(this)
        input_layout.removeOnKeyboardHiddenListener(this)
        MixinApplication.conversationId = null
    }

    @SuppressLint("WakelockTimeout")
    override fun onStatusChange(status: Int) {
        if (isNearToSensor) return
        if (status == STATUS_PLAY) {
            if (!aodWakeLock.isHeld) {
                aodWakeLock.acquire()
            }
        } else {
            if (aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private var isNearToSensor: Boolean = false
    override fun onSensorChanged(event: SensorEvent?) {
        if (callState.isNotIdle()) return

        val values = event?.values ?: return
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            isNearToSensor = values[0] < 5.0f && values[0] != sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY).maximumRange
            if (AudioPlayer.isEnd() || AudioPlayer.audioFilePlaying() || audioSwitch.isBluetoothHeadsetOrWiredHeadset()) {
                leaveDevice()
            } else if (!audioSwitch.isBluetoothHeadsetOrWiredHeadset()) {
                if (isNearToSensor) {
                    nearDevice()
                } else {
                    leaveDevice()
                }
            }
        }
    }

    override fun onStop() {
        markRead()
        AudioPlayer.pause()
        val draftText = chat_control.chat_et.text
        if (draftText != null) {
            chatViewModel.saveDraft(conversationId, draftText.toString())
        }
        if (OpusAudioRecorder.state != STATE_NOT_INIT) {
            OpusAudioRecorder.get(conversationId).stop()
        }
        if (chat_control?.isRecording == true) {
            chat_control?.cancelExternal()
        }
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        if (aodWakeLock.isHeld) {
            aodWakeLock.release()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        chat_rv?.let { rv ->
            rv.children.forEach {
                val vh = rv.getChildViewHolder(it)
                if (vh != null && vh is BaseViewHolder) {
                    vh.stopListen()
                }
            }
        }
        if (isAdded) {
            chatAdapter.unregisterAdapterDataObserver(chatAdapterDataObserver)
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.supportFragmentManager?.let { fm ->
            val fragments = fm.fragments
            if (fragments.size > 0) {
                fm.inTransaction {
                    fragments.indices
                        .map { fragments[it] }
                        .filter { it != null && it !is ConversationFragment }
                        .forEach { remove(it) }
                }
            }
        }
        AudioPlayer.release()
        context?.let {
            if (!anyCallServiceRunning(it)) {
                audioSwitch.safeStop()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            chat_control.isRecording -> {
                alertDialogBuilder()
                    .setTitle(getString(R.string.chat_audio_discard_warning_title))
                    .setMessage(getString(R.string.chat_audio_discard_warning))
                    .setNeutralButton(getString(R.string.chat_audio_discard_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.chat_audio_discard_ok)) { dialog, _ ->
                        activity?.finish()
                        dialog.dismiss()
                    }
                    .show()
                true
            }
            tool_view.visibility == VISIBLE -> {
                closeTool()
                true
            }
            chat_control.getVisibleContainer()?.isVisible == true -> {
                chat_control.reset()
                true
            }
            chat_control.isRecording -> {
                OpusAudioRecorder.get(conversationId).stopRecording(false)
                chat_control.cancelExternal()
                true
            }
            reply_view.visibility == VISIBLE -> {
                reply_view.fadeOut(isGone = true)
                true
            }
            else -> false
        }
    }

    private fun hideIfShowBottomSheet() {
        if (sticker_container.isVisible &&
            menu_container.isVisible &&
            gallery_container.isVisible
        ) {
            chat_control.reset()
        }
        if (reply_view.isVisible) {
            reply_view.fadeOut(isGone = true)
        }
    }

    private fun closeTool() {
        chatAdapter.selectSet.clear()
        chatAdapter.notifyDataSetChanged()
        tool_view.fadeOut()
    }

    private fun markRead() {
        chatAdapter.markRead()
    }

    private var firstPosition = 0

    private fun initView() {
        chat_rv.visibility = INVISIBLE
        if (chat_rv.adapter == null) {
            chat_rv.adapter = chatAdapter
            chatAdapter.listen(destroyScope)
        }
        chat_control.callback = chatControlCallback
        chat_control.activity = requireActivity()
        chat_control.inputLayout = input_layout
        chat_control.stickerContainer = sticker_container
        chat_control.menuContainer = menu_container
        chat_control.galleryContainer = gallery_container
        chat_control.recordTipView = record_tip_tv
        chat_control.setCircle(record_circle)
        chat_control.chat_et.setCommitContentListener(
            object :
                ContentEditText.OnCommitContentListener {
                override fun onCommitContent(
                    inputContentInfo: InputContentInfoCompat?,
                    flags: Int,
                    opts: Bundle?
                ): Boolean {
                    if (inputContentInfo != null) {
                        val url = inputContentInfo.contentUri.getFilePath(requireContext())
                            ?: return false
                        sendImageMessage(url.toUri())
                    }
                    return true
                }
            }
        )
        chat_rv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
        chat_rv.addItemDecoration(decoration)
        chat_rv.itemAnimator = null

        chat_rv.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    firstPosition = (chat_rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (firstPosition > 0) {
                        if (isBottom) {
                            isBottom = false
                        }
                    } else {
                        if (!isBottom) {
                            isBottom = true
                        }
                        unreadTipCount = 0
                        flag_layout.bottomCountFlag = false
                    }
                }
            }
        )
        chat_rv.callback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val currentContainer = chat_control.getDraggableContainer()
                if (currentContainer != null) {
                    dragChatControl(dis)
                }
            }

            override fun onRelease(fling: Int) {
                releaseChatControl(fling)
            }
        }

        chat_rv.setScrollingTouchSlop(SWAP_SLOT)

        initTouchHelper()

        action_bar.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }

        if (isGroup) {
            renderGroup()
        } else {
            renderUser(recipient!!)
        }

        flag_layout.down_flag_layout.setOnClickListener {
            if (chat_rv.scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                chat_rv.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL,
                        0f,
                        0f,
                        0
                    )
                )
            }
            if (positionBeforeClickQuote != null) {
                scrollToMessage(positionBeforeClickQuote!!) {
                    positionBeforeClickQuote = null
                }
            } else {
                scrollTo(0)
                unreadTipCount = 0
                flag_layout.bottomCountFlag = false
            }
        }
        chatViewModel.searchConversationById(conversationId)
            .autoDispose(stopScope).subscribe(
                {
                    it?.draft?.let { str ->
                        if (isAdded) {
                            chat_control.chat_et.setText(str)
                        }
                    }
                },
                {
                    Timber.e(it)
                }
            )
        tool_view.close_iv.setOnClickListener { activity?.onBackPressed() }
        tool_view.delete_iv.setOnClickListener {
            chatAdapter.selectSet.filter { it.type.endsWith("_AUDIO") }.forEach {
                if (AudioPlayer.isPlay(it.messageId)) {
                    AudioPlayer.pause()
                }
            }
            deleteMessage(chatAdapter.selectSet.toList())
            closeTool()
        }
        reply_view.reply_close_iv.setOnClickListener {
            reply_view.messageItem = null
            reply_view.fadeOut(isGone = true)
        }
        tool_view.copy_iv.setOnClickListener {
            try {
                context?.getClipboardManager()?.setPrimaryClip(
                    ClipData.newPlainText(null, chatAdapter.selectSet.valueAt(0)?.content)
                )
                context?.toast(R.string.copy_success)
            } catch (e: ArrayIndexOutOfBoundsException) {
            }
            closeTool()
        }
        tool_view.forward_iv.setOnClickListener {
            lifecycleScope.launch {
                val list = chatViewModel.getSortMessagesByIds(chatAdapter.selectSet)
                getForwardResult.launch(Pair(list, null))
                closeTool()
            }
        }
        tool_view.add_sticker_iv.setOnClickListener {
            if (chatAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            val messageItem = chatAdapter.selectSet.valueAt(0)
            messageItem?.let { m ->
                val isSticker = messageItem.type.endsWith("STICKER")
                if (isSticker && m.stickerId != null) {
                    addSticker(m)
                } else {
                    val url = m.mediaUrl
                    url?.let {
                        val uri = url.toUri()
                        val mimeType = getMimeType(uri, true)
                        if (mimeType?.isImageSupport() == true) {
                            StickerActivity.show(requireContext(), url = it, showAdd = true)
                        } else {
                            requireContext().toast(R.string.sticker_add_invalid_format)
                        }
                    }
                }
            }
        }

        tool_view.reply_iv.setOnClickListener {
            if (chatAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            chatAdapter.selectSet.valueAt(0)?.let {
                reply_view.bind(it)
            }
            displayReplyView()
            closeTool()
        }

        group_desc.movementMethod = LinkMovementMethod()
        group_desc.addAutoLinkMode(AutoLinkMode.MODE_URL)
        group_desc.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        group_desc.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
        }
        group_flag.setOnClickListener {
            group_desc.expand()
        }
        group_desc.setOnClickListener {
            group_desc.expand()
        }
        group_close.setOnClickListener {
            requireActivity().sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(conversationId, false)
            group_flag.isVisible = false
            driver.isVisible = false
        }
        tap_join_view.setOnClickListener {
            if (!requireContext().networkConnected()) {
                toast(R.string.error_network)
                return@setOnClickListener
            }
            val isBusy = callState.isBusy(requireContext())
            if (isBusy) {
                alertDialogBuilder()
                    .setMessage(getString(R.string.chat_call_warning_call))
                    .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }
            receiveInvite(requireContext(), conversationId, playRing = false)
        }
        callState.observe(
            viewLifecycleOwner,
            { state ->
                chat_control.calling = state != CallService.CallState.STATE_IDLE
                if (isGroup) {
                    tap_join_view.isVisible = callState.isPendingGroupCall(conversationId)
                } else {
                    tap_join_view.isVisible = false
                }
            }
        )
        bindData()
    }

    lateinit var itemTouchHelper: ItemTouchHelper
    private fun initTouchHelper() {
        val callback =
            ChatItemCallback(
                requireContext(),
                object : ChatItemCallback.ItemCallbackListener {
                    override fun onSwiped(position: Int) {
                        chatAdapter.getItem(position)?.let {
                            reply_view.bind(it)
                        }
                        itemTouchHelper.attachToRecyclerView(null)
                        itemTouchHelper.attachToRecyclerView(chat_rv)
                        displayReplyView()
                        closeTool()
                    }
                }
            )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(chat_rv)
    }

    private fun addSticker(m: MessageItem) = lifecycleScope.launch(Dispatchers.IO) {
        if (!isAdded) return@launch

        val request = StickerAddRequest(stickerId = m.stickerId)
        val r = try {
            chatViewModel.addStickerAsync(request).await()
        } catch (e: Exception) {
            ErrorHandler.handleError(e)
            return@launch
        }
        if (r.isSuccess) {
            val personalAlbum = chatViewModel.getPersonalAlbums()
            if (personalAlbum == null) { // not add any personal sticker yet
                chatViewModel.refreshStickerAlbums()
            } else {
                chatViewModel.addStickerLocal(r.data as Sticker, personalAlbum.albumId)
            }
            withContext(Dispatchers.Main) {
                closeTool()
                requireContext().toast(R.string.add_success)
            }
        } else {
            ErrorHandler.handleMixinError(
                r.errorCode,
                r.errorDescription,
                getString(R.string.sticker_add_failed)
            )
        }
    }

    private var deleteDialog: AlertDialog? = null
    private fun deleteMessage(messages: List<MessageItem>) {
        deleteDialog?.dismiss()
        val showRecall = messages.all { item ->
            item.userId == sender.userId && item.status != MessageStatus.SENDING.name && !item.createdAt.lateOneHours() && item.canRecall()
        }
        val deleteDialogLayout = generateDeleteDialogLayout()
        deleteDialog = alertDialogBuilder()
            .setMessage(getString(R.string.chat_delete_message, messages.size))
            .setView(deleteDialogLayout)
            .create()
        if (showRecall) {
            deleteDialogLayout.delete_everyone.setOnClickListener {
                if (defaultSharedPreferences.getBoolean(Constants.Account.PREF_RECALL_SHOW, true)) {
                    deleteDialog?.dismiss()
                    deleteAlert(messages)
                    defaultSharedPreferences.putBoolean(Constants.Account.PREF_RECALL_SHOW, false)
                } else {
                    chatViewModel.sendRecallMessage(conversationId, sender, messages)
                    deleteDialog?.dismiss()
                }
            }
            deleteDialogLayout.delete_everyone.visibility = VISIBLE
        } else {
            deleteDialogLayout.delete_everyone.visibility = GONE
        }
        deleteDialogLayout.delete_me.setOnClickListener {
            chatViewModel.deleteMessages(messages)
            deleteDialog?.dismiss()
        }
        deleteDialog?.show()
    }

    private fun generateDeleteDialogLayout(): View {
        return LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_delete, null, false)
            .apply {
                this.delete_cancel.setOnClickListener {
                    deleteDialog?.dismiss()
                }
            }
    }

    private var deleteAlertDialog: AlertDialog? = null
    private fun deleteAlert(messages: List<MessageItem>) {
        deleteAlertDialog?.dismiss()
        deleteDialog = alertDialogBuilder()
            .setMessage(getString(R.string.chat_recall_delete_alert))
            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                chatViewModel.sendRecallMessage(conversationId, sender, messages)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.chat_recall_delete_more) { dialog, _ ->
                context?.openUrl(getString(R.string.chat_delete_url))
                dialog.dismiss()
            }
            .create()
        deleteDialog?.show()
    }

    private fun liveDataMessage(unreadCount: Int, unreadMessageId: String?) {
        var oldCount: Int = -1
        chatViewModel.getMessages(conversationId, unreadCount)
            .observe(
                viewLifecycleOwner,
                { list ->
                    if (oldCount == -1) {
                        oldCount = list.size
                    } else if (!isFirstLoad && !isBottom && list.size > oldCount) {
                        unreadTipCount += (list.size - oldCount)
                        oldCount = list.size
                    } else if (isBottom) {
                        unreadTipCount = 0
                        oldCount = list.size
                    }
                    chatViewModel.viewModelScope.launch {
                        chatAdapter.hasBottomView = (
                            (isBot && list.isEmpty()) ||
                                (!isGroup && (!list.isEmpty()) && chatViewModel.isSilence(conversationId, sender.userId))
                            ) &&
                            recipient?.relationship == UserRelationship.STRANGER.name
                    }
                    if (isFirstLoad && messageId == null && unreadCount > 0) {
                        chatAdapter.unreadMsgId = unreadMessageId
                    } else if (lastReadMessage != null) {
                        chatViewModel.viewModelScope.launch {
                            lastReadMessage?.let { id ->
                                val unreadMsgId = chatViewModel.findUnreadMessageByMessageId(
                                    conversationId,
                                    sender.userId,
                                    id
                                )
                                if (unreadMsgId != null) {
                                    chatAdapter.unreadMsgId = unreadMsgId
                                    lastReadMessage = null
                                }
                            }
                        }
                    }
                    if (list.size > 0) {
                        if (isFirstMessage) {
                            isFirstMessage = false
                        }
                        chatViewModel.markMessageRead(conversationId, sender.userId)
                    }
                    chatAdapter.submitList(list)
                }
            )
    }

    private var unreadCount = 0
    private fun bindData() {
        unreadCount = requireArguments().getInt(UNREAD_COUNT, 0)
        liveDataMessage(unreadCount, initialPositionMessageId)

        chatViewModel.getUnreadMentionMessageByConversationId(conversationId).observe(
            viewLifecycleOwner,
            { mentionMessages ->
                flag_layout.mentionCount = mentionMessages.size
                flag_layout.mention_flag_layout.setOnClickListener {
                    lifecycleScope.launch {
                        if (mentionMessages.isEmpty()) {
                            return@launch
                        }
                        val messageId = mentionMessages.first().messageId
                        scrollToMessage(messageId) {
                            lifecycleScope.launch {
                                chatViewModel.markMentionRead(messageId, conversationId)
                            }
                        }
                    }
                }
            }
        )

        if (isBot) {
            chatViewModel.updateRecentUsedBots(defaultSharedPreferences, recipient!!.userId)
            chat_control.showBot()
        } else {
            chat_control.hideBot()
        }
        liveDataAppList()
    }

    private fun liveDataAppList() {
        chatViewModel.getBottomApps(conversationId, recipient?.userId)?.observe(
            viewLifecycleOwner,
            { list ->
                appList = list
                appList?.let {
                    (parentFragmentManager.findFragmentByTag(MenuFragment.TAG) as? MenuFragment)?.setAppList(it)
                }
            }
        )
    }

    private var appList: List<AppItem>? = null

    private inline fun createConversation(crossinline action: () -> Unit) {
        if (isFirstMessage) {
            doAsync {
                chatViewModel.initConversation(conversationId, recipient!!, sender)
                isFirstMessage = false

                uiThread {
                    if (isAdded) {
                        action()
                    }
                }
            }
        } else {
            action()
        }
    }

    private fun isPlainMessage(): Boolean {
        return if (isGroup) {
            false
        } else {
            this.isBot
        }
    }

    private fun sendImageMessage(uri: Uri, mimeType: String? = null) {
        createConversation {
            lifecycleScope.launch {
                val code = withContext(Dispatchers.IO) {
                    chatViewModel.sendImageMessage(
                        conversationId,
                        sender,
                        uri,
                        isPlainMessage(),
                        mimeType,
                        getRelyMessage()
                    )
                }
                when (code) {
                    0 -> {
                        scrollToDown()
                        markRead()
                    }
                    -1 -> context?.toast(R.string.error_image)
                    -2 -> context?.toast(R.string.error_format)
                }
            }
        }
    }

    private fun sendGiphy(image: Image, previewUrl: String) {
        createConversation {
            chatViewModel.sendGiphyMessage(
                conversationId,
                sender.userId,
                image,
                isPlainMessage(),
                previewUrl
            )
            chat_rv.postDelayed(
                {
                    scrollToDown()
                },
                1000
            )
        }
    }

    override fun onCancel() {
        chat_control?.cancelExternal()
    }

    override fun sendAudio(messageId: String, file: File, duration: Long, waveForm: ByteArray) {
        if (duration < 500) {
            file.deleteOnExit()
        } else {
            createConversation {
                chatViewModel.sendAudioMessage(
                    conversationId,
                    messageId,
                    sender,
                    file,
                    duration,
                    waveForm,
                    isPlainMessage(),
                    getRelyMessage()
                )
                scrollToDown()
            }
        }
    }

    private fun sendVideoMessage(uri: Uri) {
        createConversation {
            chatViewModel.sendVideoMessage(
                conversationId,
                sender.userId,
                uri,
                isPlainMessage(),
                replyMessage = getRelyMessage()
            )
            chat_rv.postDelayed(
                {
                    scrollToDown()
                },
                1000
            )
        }
    }

    private fun sendAttachmentMessage(attachment: Attachment) {
        createConversation {
            chatViewModel.sendAttachmentMessage(
                conversationId,
                sender,
                attachment,
                isPlainMessage(),
                getRelyMessage()
            )

            scrollToDown()
            markRead()
        }
    }

    private fun sendStickerMessage(stickerId: String) {
        createConversation {
            chatViewModel.sendStickerMessage(
                conversationId,
                sender,
                StickerMessagePayload(stickerId),
                isPlainMessage()
            )
            scrollToDown()
            markRead()
        }
    }

    private fun sendContactMessage(userId: String) {
        createConversation {
            chatViewModel.sendContactMessage(conversationId, sender, userId, isPlainMessage(), getRelyMessage())
            scrollToDown()
            markRead()
        }
    }

    private fun getRelyMessage(): MessageItem? {
        if (isAdded) {
            val messageItem = reply_view.messageItem
            if (reply_view.isVisible) {
                reply_view.fadeOut(isGone = true)
                reply_view.messageItem = null
            }
            return messageItem
        }
        return null
    }

    private fun sendMessage(message: String) {
        if (message.isNotBlank()) {
            chat_control.chat_et.setText("")
            createConversation {
                chatViewModel.sendTextMessage(conversationId, sender, message, isPlainMessage())
                scrollToDown()
                markRead()
            }
        }
    }

    private fun sendLocation(location: LocationPayload) {
        createConversation {
            chatViewModel.sendLocationMessage(conversationId, sender.userId, location, isPlainMessage())
            scrollToDown()
            markRead()
        }
    }

    private fun sendReplyTextMessage(message: String) {
        if (message.isNotBlank() && reply_view.messageItem != null) {
            chat_control.chat_et.setText("")
            createConversation {
                chatViewModel.sendReplyTextMessage(
                    conversationId,
                    sender,
                    message,
                    reply_view.messageItem!!,
                    isPlainMessage()
                )
                reply_view.fadeOut(isGone = true)
                reply_view.messageItem = null
                scrollToDown()
                markRead()
            }
        }
    }

    private var groupName: String? = null
    private var groupNumber: Int = 0

    @SuppressLint("SetTextI18n")
    private fun renderGroup() {
        action_bar.avatar_iv.visibility = VISIBLE
        action_bar.avatar_iv.setOnClickListener {
            showGroupBottomSheet(false)
        }
        chatViewModel.getConversationById(conversationId).observe(
            viewLifecycleOwner,
            {
                it?.let {
                    groupName = it.name
                    action_bar.setSubTitle(
                        groupName
                            ?: "",
                        getString(R.string.title_participants, groupNumber)
                    )
                    action_bar.avatar_iv.setGroup(it.iconUrl)
                }
            }
        )
        chatViewModel.getGroupParticipantsLiveData(conversationId)
            .observe(
                viewLifecycleOwner,
                { users ->
                    users?.let { u ->
                        groupNumber = u.size
                        action_bar.setSubTitle(
                            groupName ?: "",
                            getString(R.string.title_participants, groupNumber)
                        )
                        val userIds = arrayListOf<String>()
                        users.mapTo(userIds) { it.userId }
                        if (userIds.contains(Session.getAccountId())) {
                            chat_control.visibility = VISIBLE
                            bottom_cant_send.visibility = GONE
                        } else {
                            chat_control.visibility = INVISIBLE
                            bottom_cant_send.visibility = VISIBLE
                            chat_control.chat_et.hideKeyboard()
                        }
                    }
                }
            )
        mention_rv.adapter = mentionAdapter
        mention_rv.layoutManager = LinearLayoutManager(context)
    }

    @Suppress("SameParameterValue")
    private fun showGroupBottomSheet(expand: Boolean) {
        hideIfShowBottomSheet()
        val bottomSheetDialogFragment = GroupBottomSheetDialogFragment.newInstance(
            conversationId = conversationId,
            expand = expand
        )
        bottomSheetDialogFragment.showNow(parentFragmentManager, GroupBottomSheetDialogFragment.TAG)
        bottomSheetDialogFragment.callback = object : GroupBottomSheetDialogFragment.Callback {
            override fun onDelete() {
                activity?.finish()
            }
        }
    }

    override fun onKeyboardHidden() {
        chat_control.toggleKeyboard(false)
    }

    override fun onKeyboardShown() {
        chat_control.toggleKeyboard(true)
    }

    private fun renderUser(user: User) {
        chatAdapter.recipient = user
        renderUserInfo(user)
        chatViewModel.findUserById(user.userId).observe(
            viewLifecycleOwner,
            {
                it?.let { u ->
                    recipient = u
                    if (u.isBot()) {
                        renderBot(u)
                    }
                    renderUserInfo(u)
                }
            }
        )
        action_bar.avatar_iv.setOnClickListener {
            hideIfShowBottomSheet()
            UserBottomSheetDialogFragment.newInstance(user, conversationId)
                .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
        }
        bottom_unblock.setOnClickListener {
            recipient?.let { user ->
                chatViewModel.updateRelationship(
                    RelationshipRequest(
                        user.userId,
                        RelationshipAction.UNBLOCK.name,
                        user.fullName
                    )
                )
            }
        }
        if (user.isBot()) {
            renderBot(user)
        }
    }

    private fun renderBot(user: User) = lifecycleScope.launch {
        if (!isAdded) return@launch

        app = chatViewModel.findAppById(user.appId!!)
        if (app != null && app!!.creatorId == Session.getAccountId()) {
            val menuFragment = parentFragmentManager.findFragmentByTag(MenuFragment.TAG)
            if (menuFragment == null) {
                initMenuLayout(true)
            }
        }
    }

    private fun renderUserInfo(user: User) {
        action_bar.setSubTitle(user.fullName ?: "", user.identityNumber)
        action_bar.avatar_iv.visibility = VISIBLE
        action_bar.avatar_iv.setTextSize(16f)
        action_bar.avatar_iv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.let {
            if (it.relationship == UserRelationship.BLOCKING.name) {
                chat_control.visibility = INVISIBLE
                bottom_unblock.visibility = VISIBLE
                chat_control.chat_et.hideKeyboard()
            } else {
                chat_control.visibility = VISIBLE
                bottom_unblock.visibility = GONE
            }
        }
        if (user.isScam == true) {
            val closeScamTime = scamPreferences.getLong(user.userId, 0)
            if (System.currentTimeMillis() > closeScamTime) {
                scam_flag.isVisible = true
                driver.isVisible = true
                warning_close.setOnClickListener {
                    scamPreferences.putLong(user.userId, System.currentTimeMillis() + INTERVAL_24_HOURS)
                    scam_flag.isVisible = false
                    driver.isVisible = false
                }
            } else {
                scam_flag.isVisible = false
                driver.isVisible = false
            }
        }
    }

    private fun open(url: String, app: App?, appCard: AppCardData? = null) {
        chat_control.chat_et.hideKeyboard()
        url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope, app, appCard)
    }

    private fun openInputAction(action: String): Boolean {
        if (action.startsWith("input:") && action.length > 6) {
            val msg = action.substring(6).trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
            }
            return true
        }
        return false
    }

    private fun clickSticker() {
        val stickerAlbumFragment = parentFragmentManager.findFragmentByTag(StickerAlbumFragment.TAG)
        if (stickerAlbumFragment == null) {
            initStickerLayout()
        }
    }

    private fun clickMenu() {
        val menuFragment = parentFragmentManager.findFragmentByTag(MenuFragment.TAG)
        if (menuFragment == null) {
            initMenuLayout()
        }
    }

    private fun clickGallery() {
        val galleryAlbumFragment = parentFragmentManager.findFragmentByTag(GalleryAlbumFragment.TAG)
        if (galleryAlbumFragment == null) {
            initGalleryLayout()
        }
    }

    private fun initGalleryLayout() {
        val galleryAlbumFragment = GalleryAlbumFragment.newInstance()
        galleryAlbumFragment.callback = object : GalleryCallback {
            override fun onItemClick(pos: Int, uri: Uri, isVideo: Boolean) {
                if (isVideo) {
                    sendVideoMessage(uri)
                } else {
                    sendImageMessage(uri)
                }
                releaseChatControl(FLING_DOWN)
            }

            override fun onCameraClick() {
                openCamera()
            }
        }
        galleryAlbumFragment.rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val currentContainer = chat_control.getDraggableContainer()
                if (currentContainer != null) {
                    dragChatControl(dis)
                }
            }

            override fun onRelease(fling: Int) {
                releaseChatControl(fling)
            }
        }
        activity?.replaceFragment(
            galleryAlbumFragment,
            R.id.gallery_container,
            GalleryAlbumFragment.TAG
        )
    }

    private fun initMenuLayout(isSelfCreatedBot: Boolean = false) {
        val menuFragment = MenuFragment.newInstance(isGroup, isBot, isSelfCreatedBot)
        activity?.replaceFragment(menuFragment, R.id.menu_container, MenuFragment.TAG)
        appList?.let {
            menuFragment.setAppList(it)
        }
        menuFragment.callback = object : MenuFragment.Callback {
            override fun onMenuClick(menu: Menu) {
                if (!isGroup) {
                    jobManager.addJobInBackground(FavoriteAppJob(sender.userId, recipient?.userId))
                }
                when (menu.type) {
                    MenuType.Camera -> {
                        openCamera()
                    }
                    MenuType.File -> {
                        RxPermissions(requireActivity())
                            .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .subscribe(
                                { granted ->
                                    if (granted) {
                                        selectDocument()
                                    } else {
                                        context?.openPermissionSetting()
                                    }
                                },
                                {
                                }
                            )
                    }
                    MenuType.Transfer -> {
                        chat_control.reset()
                        if (Session.getAccount()?.hasPin == true) {
                            recipient?.let {
                                TransferFragment.newInstance(it.userId, supportSwitchAsset = true)
                                    .showNow(parentFragmentManager, TransferFragment.TAG)
                            }
                        } else {
                            parentFragmentManager.inTransaction {
                                setCustomAnimations(
                                    R.anim.slide_in_bottom,
                                    R.anim.slide_out_bottom,
                                    R
                                        .anim.slide_in_bottom,
                                    R.anim.slide_out_bottom
                                )
                                    .add(
                                        R.id.container,
                                        WalletPasswordFragment.newInstance(),
                                        WalletPasswordFragment.TAG
                                    )
                                    .addToBackStack(null)
                            }
                        }
                    }
                    MenuType.Contact -> {
                        parentFragmentManager.inTransaction {
                            setCustomAnimations(
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_bottom,
                                R
                                    .anim.slide_in_bottom,
                                R.anim.slide_out_bottom
                            )
                                .add(
                                    R.id.container,
                                    FriendsFragment.newInstance(conversationId).apply {
                                        setOnFriendClick {
                                            sendContactMessage(it.userId)
                                        }
                                    },
                                    FriendsFragment.TAG
                                )
                                .addToBackStack(null)
                        }
                    }
                    MenuType.Voice -> {
                        chat_control.reset()
                        if (callState.isNotIdle()) {
                            if ((recipient != null && callState.user?.userId == recipient?.userId) ||
                                (recipient == null && callState.conversationId == conversationId)
                            ) {
                                CallActivity.show(requireContext())
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
                                .subscribe(
                                    { granted ->
                                        if (granted) {
                                            voiceCall()
                                        } else {
                                            context?.openPermissionSetting()
                                        }
                                    },
                                    {
                                    }
                                )
                        }
                    }
                    MenuType.Location -> {
                        if (requireContext().isGooglePlayServicesAvailable()) {
                            RxPermissions(requireActivity())
                                .request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                                .autoDispose(stopScope)
                                .subscribe { granted ->
                                    if (granted) {
                                        LocationActivity.show(this@ConversationFragment)
                                    } else {
                                        context?.openPermissionSetting()
                                    }
                                }
                        } else {
                            toast(R.string.location_google_error)
                        }
                    }
                    MenuType.App -> {
                        menu.app?.let { app ->
                            chat_control.chat_et.hideKeyboard()
                            WebActivity.show(
                                requireActivity(),
                                app.homeUri,
                                conversationId,
                                app.toApp()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initStickerLayout() {
        val stickerAlbumFragment = StickerAlbumFragment.newInstance()
        activity?.replaceFragment(
            stickerAlbumFragment,
            R.id.sticker_container,
            StickerAlbumFragment.TAG
        )
        stickerAlbumFragment.setCallback(
            object : StickerAlbumFragment.Callback {
                override fun onStickerClick(stickerId: String) {
                    if (isAdded) {
                        if (sticker_container.height != input_layout.keyboardHeight) {
                            sticker_container.animateHeight(
                                sticker_container.height,
                                input_layout.keyboardHeight
                            )
                        }
                        sendStickerMessage(stickerId)
                    }
                }

                override fun onGiphyClick(image: Image, previewUrl: String) {
                    if (isAdded) {
                        sendGiphy(image, previewUrl)
                    }
                }
            }
        )
        stickerAlbumFragment.rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val currentContainer = chat_control.getDraggableContainer()
                if (currentContainer != null) {
                    dragChatControl(dis)
                }
            }

            override fun onRelease(fling: Int) {
                releaseChatControl(fling)
            }
        }
    }

    private fun scrollToDown() {
        chat_rv.layoutManager?.scrollToPosition(0)
        if (firstPosition > PAGE_SIZE * 6) {
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun scrollTo(
        position: Int,
        offset: Int = -1,
        delay: Long = 30,
        action: (() -> Unit)? = null
    ) {
        chat_rv.postDelayed(
            {
                if (isAdded) {
                    if (position == 0 && offset == 0) {
                        chat_rv.layoutManager?.scrollToPosition(0)
                    } else if (offset == -1) {
                        (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            0
                        )
                    } else {
                        (chat_rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            offset
                        )
                    }
                    action?.let { it() }
                    if (abs(firstPosition - position) > PAGE_SIZE * 6) {
                        chatAdapter.notifyDataSetChanged()
                    }
                }
            },
            delay
        )
    }

    private fun scrollToMessage(
        messageId: String,
        findMessageAction: ((index: Int) -> Unit)? = null
    ) = lifecycleScope.launch {
        if (!isAdded) return@launch

        val index = chatViewModel.findMessageIndex(conversationId, messageId)
        findMessageAction?.invoke(index)
        if (index == 0) {
            scrollTo(
                0,
                chat_rv.measuredHeight * 3 / 4,
                action = {
                    requireContext().mainThreadDelayed(
                        {
                            RxBus.publish(BlinkEvent(messageId))
                        },
                        60
                    )
                }
            )
        } else {
            chatAdapter.loadAround(index)
            if (index == chatAdapter.itemCount - 1) {
                scrollTo(
                    index,
                    0,
                    action = {
                        requireContext().mainThreadDelayed(
                            {
                                RxBus.publish(BlinkEvent(messageId))
                            },
                            60
                        )
                    }
                )
            } else {
                val lm = (chat_rv.layoutManager as LinearLayoutManager)
                val lastPosition = lm.findLastCompletelyVisibleItemPosition()
                val firstPosition = lm.findFirstVisibleItemPosition()
                if (index in firstPosition..lastPosition) {
                    requireContext().mainThreadDelayed(
                        {
                            RxBus.publish(BlinkEvent(messageId))
                        },
                        60
                    )
                } else {
                    scrollTo(
                        index + 1,
                        chat_rv.measuredHeight * 3 / 4,
                        action = {
                            requireContext().mainThreadDelayed(
                                {
                                    RxBus.publish(BlinkEvent(messageId))
                                },
                                60
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                if (data.hasExtra(IS_VIDEO)) {
                    sendVideoMessage(it)
                } else {
                    sendImageMessage(it)
                }
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri?.let { imageUri ->
                showPreview(imageUri) { sendImageMessage(it) }
            }
        } else if (requestCode == REQUEST_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val attachment = context?.getAttachment(uri)
            if (attachment != null) {
                alertDialogBuilder()
                    .setMessage(
                        if (isGroup) {
                            requireContext().getString(
                                R.string.send_file_group,
                                attachment.filename,
                                groupName
                            )
                        } else {
                            requireContext().getString(
                                R.string.send_file,
                                attachment.filename,
                                recipient?.fullName
                            )
                        }
                    )
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.send) { dialog, _ ->
                        sendAttachmentMessage(attachment)
                        dialog.dismiss()
                    }.show()
            } else {
                toast(R.string.error_file_exists)
            }
        } else if (requestCode == REQUEST_LOCATION && resultCode == Activity.RESULT_OK) {
            val intent = data ?: return
            val location = LocationActivity.getResult(intent) ?: return
            sendLocation(location)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun openCamera() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe(
                { granted ->
                    if (granted) {
                        imageUri = createImageUri()
                        imageUri?.let {
                            openCamera(it)
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                },
                {
                }
            )
    }

    private var previewDialogFragment: PreviewDialogFragment? = null

    private fun showPreview(uri: Uri, action: (Uri) -> Unit) {
        if (previewDialogFragment == null) {
            previewDialogFragment = PreviewDialogFragment.newInstance()
        }
        previewDialogFragment?.show(parentFragmentManager, uri, action)
    }

    private val voiceAlert by lazy {
        alertDialogBuilder()
            .setMessage(getString(R.string.chat_call_warning_voice))
            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }.create()
    }

    private fun showVoiceWarning() {
        if (!voiceAlert.isShowing) {
            voiceAlert.show()
        }
    }

    private fun dragChatControl(dis: Float) {
        val currentContainer = chat_control.getDraggableContainer() ?: return
        val params = currentContainer.layoutParams
        val targetH = params.height - dis.toInt()
        val total = (requireContext().screenHeight() * 2) / 3
        if (targetH <= 0 || targetH >= total) return

        params.height = targetH
        currentContainer.layoutParams = params
    }

    private fun releaseChatControl(fling: Int) {
        if (!isAdded) return

        val currentContainer = chat_control.getDraggableContainer() ?: return
        val curH = currentContainer.height
        val max = (requireContext().screenHeight() * 2) / 3
        val maxMid = input_layout.keyboardHeight + (max - input_layout.keyboardHeight) / 2
        val minMid = input_layout.keyboardHeight / 2
        val targetH = if (curH > input_layout.keyboardHeight) {
            if (fling == FLING_UP) {
                max
            } else if (fling == FLING_DOWN) {
                input_layout.keyboardHeight
            } else {
                if (curH <= maxMid) {
                    input_layout.keyboardHeight
                } else {
                    max
                }
            }
        } else if (curH < input_layout.keyboardHeight) {
            if (fling == FLING_UP) {
                input_layout.keyboardHeight
            } else if (fling == FLING_DOWN) {
                0
            } else {
                if (curH > minMid) {
                    input_layout.keyboardHeight
                } else {
                    0
                }
            }
        } else {
            when (fling) {
                FLING_UP -> {
                    max
                }
                FLING_DOWN -> {
                    0
                }
                else -> {
                    input_layout.keyboardHeight
                }
            }
        }
        if (targetH == 0) {
            chat_control.reset()
        }
        currentContainer.animateHeight(curH, targetH)
        RxBus.publish(DragReleaseEvent(targetH == max))
    }

    private fun showBottomSheet(messageItem: MessageItem) {
        var bottomSheet: BottomSheet? = null
        val builder = BottomSheet.Builder(requireActivity())
        val items = arrayListOf<BottomSheetItem>()
        if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
            items.add(
                BottomSheetItem(
                    getString(R.string.save_to_music),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    }
                )
            )
        } else if (MimeTypes.isVideo(messageItem.mediaMimeType) ||
            messageItem.mediaMimeType?.isImageSupport() == true
        ) {
            items.add(
                BottomSheetItem(
                    getString(R.string.save_to_gallery),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    }
                )
            )
        } else {
            items.add(
                BottomSheetItem(
                    getString(R.string.save_to_downloads),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    }
                )
            )
        }
        items.add(
            BottomSheetItem(
                getString(R.string.open),
                {
                    requireContext().openMedia(messageItem)
                    bottomSheet?.dismiss()
                }
            )
        )
        val view = buildBottomSheetView(requireContext(), items)
        builder.setCustomView(view)
        bottomSheet = builder.create()
        bottomSheet.show()
    }

    private fun checkWritePermissionAndSave(messageItem: MessageItem) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .autoDispose(stopScope)
            .subscribe(
                { granted ->
                    if (granted) {
                        messageItem.saveToLocal(requireContext())
                    } else {
                        context?.openPermissionSetting()
                    }
                },
                {
                }
            )
    }

    private fun showRecordingAlert() {
        alertDialogBuilder()
            .setMessage(getString(R.string.chat_audio_warning))
            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun nearDevice() {
        if (callState.isNotIdle() || audioSwitch.isBluetoothHeadsetOrWiredHeadset()) return

        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L)
        }
        Timber.d("$TAG_AUDIO${audioSwitch.selectedAudioDevice}")
        audioSwitch.selectEarpiece()
        audioSwitch.safeActivate()
    }

    private fun leaveDevice() {
        if (callState.isNotIdle() || audioSwitch.isBluetoothHeadsetOrWiredHeadset()) return

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        Timber.d("$TAG_AUDIO leaveDevice ${audioSwitch.selectedAudioDevice}")
        audioSwitch.deactivate()
        audioSwitch.selectSpeakerphone()
    }

    private fun openBotHome() {
        hideIfShowBottomSheet()
        recipient?.userId?.let { id ->
            chatViewModel.refreshUser(id, true)
        }
        app?.let {
            open(it.homeUri, it, null)
        }
    }

    private val chatControlCallback = object : ChatControlView.Callback {
        override fun onStickerClick() {
            clickSticker()
        }

        override fun onSendClick(text: String) {
            if (reply_view.isVisible && reply_view.messageItem != null) {
                sendReplyTextMessage(text)
            } else {
                sendMessage(text)
            }
        }

        override fun onRecordStart(audio: Boolean) {
            AudioPlayer.pause()
            OpusAudioRecorder.get(conversationId).startRecording(this@ConversationFragment)
            if (!isNearToSensor) {
                if (!aodWakeLock.isHeld) {
                    aodWakeLock.acquire()
                }
            }
        }

        override fun isReady(): Boolean {
            return OpusAudioRecorder.state == STATE_RECORDING
        }

        override fun onRecordEnd() {
            OpusAudioRecorder.get(conversationId).stopRecording(true)
            if (!isNearToSensor && aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }

        override fun onRecordCancel() {
            OpusAudioRecorder.get(conversationId).stopRecording(false)
            if (!isNearToSensor && aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }

        override fun onRecordLocked() {
            // Left Empty
        }

        override fun onCalling() {
            showVoiceWarning()
        }

        override fun onMenuClick() {
            clickMenu()
        }

        override fun onBotClick() {
            openBotHome()
        }

        override fun onGalleryClick() {
            clickGallery()
        }

        override fun onDragChatControl(dis: Float) {
            dragChatControl(dis)
        }

        override fun onReleaseChatControl(fling: Int) {
            releaseChatControl(fling)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (isGroup) {
                if (mention_rv.adapter != null && !s.isNullOrEmpty() && mentionDisplay(s)) {
                    searchMentionUser(s.toString())
                    mention_rv.layoutManager?.smoothScrollToPosition(mention_rv, null, 0)
                } else {
                    floating_layout.hideMention()
                }
            }
        }

        override fun onDelete() {}
    }

    private fun searchMentionUser(keyword: String) {
        chatViewModel.viewModelScope.launch {
            val mention = mentionEnd(keyword)
            val users = chatViewModel.fuzzySearchUser(conversationId, mention)
            mentionAdapter.keyword = mention
            mentionAdapter.submitList(users)
            if (mention_rv.isGone) {
                floating_layout.showMention(users.size)
            } else {
                floating_layout.animate2RightHeight(users.size)
            }
        }
    }

    private fun checkPeerIfNeeded() = lifecycleScope.launch {
        if (!isGroup) return@launch

        delay(1000)
        if (callState.getGroupCallStateOrNull(conversationId) != null) return@launch

        checkPeers(requireContext(), conversationId)
    }

    private fun callbackForward(data: Intent?) {
        val selectItems = data?.getParcelableArrayListExtra<SelectItem>(ARGS_RESULT)
        if (selectItems.isNullOrEmpty()) return

        val selectItem = selectItems[0]
        this.selectItem = selectItem
        Snackbar.make(chat_rv, getString(R.string.forward_success), Snackbar.LENGTH_LONG)
            .setAction(R.string.chat_go_check) {
                ConversationActivity.show(requireContext(), selectItem.conversationId, selectItem.userId)
            }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                this.view.setBackgroundResource(R.color.call_btn_icon_checked)
                (this.view.findViewById(R.id.snackbar_text) as TextView)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }.show()
    }

    private fun displayReplyView() {
        if (!reply_view.isVisible) {
            reply_view.fadeIn()
            chat_control.reset()
            if (chat_control.isRecording) {
                OpusAudioRecorder.get(conversationId).stopRecording(false)
                chat_control.cancelExternal()
            }
            chat_control.chat_et.showKeyboard()
            chat_control.chat_et.requestFocus()
        }
    }
}
