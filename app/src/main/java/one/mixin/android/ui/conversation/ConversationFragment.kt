package one.mixin.android.ui.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import android.support.v4.media.MediaDescriptionCompat.STATUS_DOWNLOADED
import android.text.method.LinkMovementMethod
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
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
import one.mixin.android.databinding.DialogDeleteBinding
import one.mixin.android.databinding.DialogForwardBinding
import one.mixin.android.databinding.DialogImportMessageBinding
import one.mixin.android.databinding.FragmentConversationBinding
import one.mixin.android.databinding.ViewUrlBottomBinding
import one.mixin.android.event.BlinkEvent
import one.mixin.android.event.CallEvent
import one.mixin.android.event.ExitEvent
import one.mixin.android.event.GroupEvent
import one.mixin.android.event.MentionReadEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.REQUEST_FILE
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alert
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.config
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isBluetoothHeadsetOrWiredHeadset
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.lateOneHours
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.observeOnce
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
import one.mixin.android.extension.selectDocument
import one.mixin.android.extension.selectEarpiece
import one.mixin.android.extension.selectSpeakerphone
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.showPipPermissionNotification
import one.mixin.android.extension.supportsNougat
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.FavoriteAppJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.media.AudioEndStatus
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
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.ui.conversation.adapter.MentionAdapter.OnUserClickListener
import one.mixin.android.ui.conversation.adapter.Menu
import one.mixin.android.ui.conversation.adapter.MenuType
import one.mixin.android.ui.conversation.chat.ChatItemCallback
import one.mixin.android.ui.conversation.chat.ChatItemCallback.Companion.SWAP_SLOT
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity.Companion.JUMP_ID
import one.mixin.android.ui.conversation.chathistory.ChatHistoryContract
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.location.LocationActivity
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.ui.conversation.preview.PreviewDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.imageeditor.ImageEditorActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.MediaItemData
import one.mixin.android.ui.player.MusicActivity
import one.mixin.android.ui.player.MusicViewModel
import one.mixin.android.ui.player.collapse
import one.mixin.android.ui.player.internal.MusicServiceConnection
import one.mixin.android.ui.player.provideMusicViewModel
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.sticker.StickerActivity
import one.mixin.android.ui.sticker.StickerPreviewBottomSheetFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.Attachment
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MusicPlayer
import one.mixin.android.util.debug.FileLogTree
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.util.import.ImportChatUtil
import one.mixin.android.util.mention.mentionDisplay
import one.mixin.android.util.mention.mentionEnd
import one.mixin.android.util.mention.mentionReplace
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.AppItem
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptData
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.canRecall
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.getEncryptedCategory
import one.mixin.android.vo.giphy.Image
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.saveToLocal
import one.mixin.android.vo.supportSticker
import one.mixin.android.vo.toApp
import one.mixin.android.vo.toTranscript
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.ERROR_ROOM_FULL
import one.mixin.android.webrtc.SelectItem
import one.mixin.android.webrtc.TAG_AUDIO
import one.mixin.android.webrtc.anyCallServiceRunning
import one.mixin.android.webrtc.checkPeers
import one.mixin.android.webrtc.outgoingCall
import one.mixin.android.webrtc.receiveInvite
import one.mixin.android.websocket.LIST_KRAKEN_PEERS
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.PinAction
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetItem
import one.mixin.android.widget.ChatControlView
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.buildBottomSheetView
import one.mixin.android.widget.gallery.internal.entity.Item
import one.mixin.android.widget.gallery.ui.GalleryActivity.Companion.IS_VIDEO
import one.mixin.android.widget.keyboard.KeyboardLayout.OnKeyboardHiddenListener
import one.mixin.android.widget.keyboard.KeyboardLayout.OnKeyboardShownListener
import one.mixin.android.widget.linktext.AutoLinkMode
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
@SuppressLint("InvalidWakeLockTag")
class ConversationFragment() :
    LinkFragment(),
    OnKeyboardShownListener,
    OnKeyboardHiddenListener,
    SensorEventListener,
    OpusAudioRecorder.Callback,
    AudioPlayer.StatusListener {

    companion object {
        const val TAG = "ConversationFragment"

        const val CONVERSATION_ID = "conversation_id"
        const val RECIPIENT_ID = "recipient_id"
        const val RECIPIENT = "recipient"
        const val MESSAGE_ID = "message_id"
        const val INITIAL_POSITION_MESSAGE_ID = "initial_position_message_id"
        const val UNREAD_COUNT = "unread_count"
        const val TRANSCRIPT_DATA = "transcript_data"
        private const val KEY_WORD = "key_word"

        fun putBundle(
            conversationId: String?,
            recipientId: String?,
            messageId: String?,
            keyword: String?,
            unreadCount: Int? = null,
            transcriptData: TranscriptData? = null
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
                putParcelable(TRANSCRIPT_DATA, transcriptData)
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

    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

    private val musicViewModel by viewModels<MusicViewModel> {
        provideMusicViewModel(musicServiceConnection, conversationId)
    }

    private val transcriptData: TranscriptData? by lazy {
        requireArguments().getParcelable(TRANSCRIPT_DATA)
    }

    private var unreadTipCount: Int = 0
    private val conversationAdapter: ConversationAdapter by lazy {
        ConversationAdapter(requireActivity(), keyword, onItemListener, isGroup, encryptCategory() != EncryptCategory.PLAIN, isBot).apply {
            registerAdapterDataObserver(chatAdapterDataObserver)
        }
    }

    private fun showPreview(uri: Uri, okText: String? = null, isVideo: Boolean, action: (Uri) -> Unit) {
        val previewDialogFragment = PreviewDialogFragment.newInstance(isVideo)
        previewDialogFragment.show(parentFragmentManager, uri, okText, action)
    }

    fun updateConversationInfo(messageId: String?, keyword: String?, unreadCount: Int) {
        this.keyword = keyword
        conversationAdapter.keyword = keyword
        val currentList = conversationAdapter.currentList
        if (currentList != null) {
            (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPosition(unreadCount)
            lifecycleScope.launch {
                delay(160)
                (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    unreadCount,
                    binding.chatRv.measuredHeight * 3 / 4
                )
                messageId?.let { id ->
                    RxBus.publish(BlinkEvent(id))
                }
            }
        } else {
            this.messageId = messageId
            this.unreadCount = unreadCount
            isFirstLoad = true
            liveDataMessage(unreadCount, messageId, !keyword.isNullOrEmpty())
        }
    }

    private val chatAdapterDataObserver =
        object : RecyclerView.AdapterDataObserver() {
            var oldSize = 0

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                conversationAdapter.currentList?.let {
                    oldSize = it.size
                }
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (viewDestroyed()) return

                when {
                    isFirstLoad -> {
                        isFirstLoad = false
                        lifecycleScope.launch {
                            delay(100)
                            messageId?.let { id ->
                                RxBus.publish(BlinkEvent(id))
                            }
                        }
                        if (context?.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                            ?.getBoolean(conversationId, false) == true
                        ) {
                            lifecycleScope.launch {
                                if (viewDestroyed()) return@launch

                                binding.groupDesc.text = chatViewModel.getAnnouncementByConversationId(conversationId)
                                binding.groupDesc.collapse()
                                binding.groupDesc.requestFocus()
                            }
                            binding.groupFlag.isVisible = true
                        }
                        val position = if (messageId != null) {
                            unreadCount + 1
                        } else {
                            unreadCount
                        }
                        if (position >= itemCount - 1) {
                            (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                itemCount - 1,
                                0
                            )
                        } else {
                            (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                position,
                                binding.chatRv.measuredHeight * 3 / 4
                            )
                        }
                        binding.chatRv.isVisible = true
                    }
                    isBottom -> {
                        if (conversationAdapter.currentList != null && conversationAdapter.currentList!!.size > oldSize) {
                            binding.chatRv.layoutManager?.scrollToPosition(0)
                        }
                    }
                    else -> {
                        if (unreadTipCount > 0) {
                            binding.flagLayout.bottomCountFlag = true
                            binding.flagLayout.unreadCount = unreadTipCount
                        } else {
                            binding.flagLayout.bottomCountFlag = false
                        }
                    }
                }
                conversationAdapter.currentList?.let {
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

    private fun checkPinMessage() {
        if (conversationAdapter.selectSet.valueAt(0)?.canNotPin() == true) {
            binding.toolView.pinIv.visibility = GONE
        } else {
            conversationAdapter.selectSet.valueAt(0)?.messageId?.let { messageId ->
                lifecycleScope.launch {
                    if (isGroup) {
                        val role = withContext(Dispatchers.IO) {
                            chatViewModel.findParticipantById(
                                conversationId,
                                Session.getAccountId()!!
                            )?.role
                        }
                        if (role != ParticipantRole.OWNER.name && role != ParticipantRole.ADMIN.name) {
                            binding.toolView.pinIv.visibility = GONE
                            return@launch
                        }
                    }
                    val pinMessage = chatViewModel.findPinMessageById(messageId)
                    if (pinMessage == null) {
                        binding.toolView.pinIv.tag = PinAction.PIN
                        binding.toolView.pinIv.setImageResource(R.drawable.ic_message_pin)
                        binding.toolView.pinIv.visibility = VISIBLE
                    } else {
                        binding.toolView.pinIv.tag = PinAction.UNPIN
                        binding.toolView.pinIv.setImageResource(R.drawable.ic_message_unpin)
                        binding.toolView.pinIv.visibility = VISIBLE
                    }
                }
            }
        }
    }

    private val onItemListener: ConversationAdapter.OnItemListener by lazy {
        object : ConversationAdapter.OnItemListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {
                if (isSelect) {
                    conversationAdapter.addSelect(messageItem)
                } else {
                    conversationAdapter.removeSelect(messageItem)
                }
                binding.toolView.countTv.text = conversationAdapter.selectSet.size.toString()
                when {
                    conversationAdapter.selectSet.isEmpty() -> binding.toolView.fadeOut()
                    conversationAdapter.selectSet.size == 1 -> {
                        try {
                            if (conversationAdapter.selectSet.valueAt(0)?.isText() == true) {
                                binding.toolView.copyIv.visibility = VISIBLE
                            } else {
                                binding.toolView.copyIv.visibility = GONE
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            binding.toolView.copyIv.visibility = GONE
                        }
                        if (conversationAdapter.selectSet.valueAt(0)?.isData() == true) {
                            binding.toolView.shareIv.visibility = VISIBLE
                        } else {
                            binding.toolView.shareIv.visibility = GONE
                        }
                        if (conversationAdapter.selectSet.valueAt(0)?.supportSticker() == true) {
                            binding.toolView.addStickerIv.visibility = VISIBLE
                        } else {
                            binding.toolView.addStickerIv.visibility = GONE
                        }
                        if (conversationAdapter.selectSet.valueAt(0)?.canNotReply() == true) {
                            binding.toolView.replyIv.visibility = GONE
                        } else {
                            binding.toolView.replyIv.visibility = VISIBLE
                        }
                        checkPinMessage()
                    }
                    else -> {
                        binding.toolView.forwardIv.visibility = VISIBLE
                        binding.toolView.replyIv.visibility = GONE
                        binding.toolView.copyIv.visibility = GONE
                        binding.toolView.addStickerIv.visibility = GONE
                        binding.toolView.shareIv.visibility = GONE
                        binding.toolView.pinIv.visibility = GONE
                    }
                }
                if (conversationAdapter.selectSet.size > 99 || conversationAdapter.selectSet.any { it.canNotForward() }) {
                    binding.toolView.forwardIv.visibility = GONE
                } else {
                    binding.toolView.forwardIv.visibility = VISIBLE
                }
                conversationAdapter.notifyDataSetChanged()
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onLongClick(messageItem: MessageItem, position: Int): Boolean {
                val b = conversationAdapter.addSelect(messageItem)
                binding.toolView.countTv.text = conversationAdapter.selectSet.size.toString()
                if (b) {
                    if (messageItem.isText()) {
                        binding.toolView.copyIv.visibility = VISIBLE
                    } else {
                        binding.toolView.copyIv.visibility = GONE
                    }
                    if (messageItem.isData()) {
                        binding.toolView.shareIv.visibility = VISIBLE
                    } else {
                        binding.toolView.shareIv.visibility = GONE
                    }

                    if (messageItem.supportSticker()) {
                        binding.toolView.addStickerIv.visibility = VISIBLE
                    } else {
                        binding.toolView.addStickerIv.visibility = GONE
                    }

                    if (conversationAdapter.selectSet.any { it.canNotForward() }) {
                        binding.toolView.forwardIv.visibility = GONE
                    } else {
                        binding.toolView.forwardIv.visibility = VISIBLE
                    }
                    if (conversationAdapter.selectSet.any { it.canNotReply() }) {
                        binding.toolView.replyIv.visibility = GONE
                    } else {
                        binding.toolView.replyIv.visibility = VISIBLE
                    }
                    checkPinMessage()
                    conversationAdapter.notifyDataSetChanged()
                    binding.toolView.fadeIn()
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
                    binding.chatControl.isRecording -> showRecordingAlert()
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
                    getMediaResult.launch(
                        MediaPagerActivity.MediaParam(
                            messageItem.conversationId,
                            messageItem.messageId,
                            messageItem,
                            MediaPagerActivity.MediaSource.Chat,
                        ),
                        MediaPagerActivity.getOptions(requireActivity(), view)
                    )
                    return
                }
                val path = messageItem.absolutePath()?.toUri()?.getFilePath()
                if (path == null) {
                    toast(R.string.error_file_exists)
                    return
                }
                val file = File(path)
                if (file.exists()) {
                    getMediaResult.launch(
                        MediaPagerActivity.MediaParam(
                            messageItem.conversationId,
                            messageItem.messageId,
                            messageItem,
                            MediaPagerActivity.MediaSource.Chat,
                        ),
                        MediaPagerActivity.getOptions(requireActivity(), view)
                    )
                } else {
                    toast(R.string.error_file_exists)
                }
            }

            override fun onStickerClick(messageItem: MessageItem) {
                StickerPreviewBottomSheetFragment.newInstance(requireNotNull(messageItem.stickerId))
                    .showNow(parentFragmentManager, StickerPreviewBottomSheetFragment.TAG)
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
                    binding.chatControl.isRecording -> showRecordingAlert()
                    MusicPlayer.isPlay(messageItem.messageId) -> MusicPlayer.pause()
                    else -> {
                        if (!MusicPlayer.sameChatAudioFilePlaying(conversationId)) {
                            MusicPlayer.playMusic(messageItem)
                            FloatingPlayer.getInstance().hide()
                        } else {
                            if (checkFloatingPermission()) {
                                collapse(conversationId)
                            }
                        }
                        FloatingPlayer.getInstance().conversationId = conversationId

                        musicViewModel.playMedia(
                            MediaItemData(
                                messageItem.messageId,
                                messageItem.mediaName ?: "",
                                "",
                                Uri.EMPTY,
                                false,
                                STATUS_DOWNLOADED
                            )
                        ) {
                            if (viewDestroyed()) return@playMedia
                            if (checkFloatingPermission()) {
                                if (MixinApplication.get().activityInForeground) {
                                    collapse(conversationId)
                                }
                            } else {
                                requireActivity().showPipPermissionNotification(MusicActivity::class.java, getString(R.string.web_floating_permission))
                            }
                        }
                    }
                }
            }

            override fun onUserClick(userId: String) {
                chatViewModel.getUserById(userId).autoDispose(stopScope).subscribe(
                    {
                        it?.let {
                            showUserBottom(parentFragmentManager, it, conversationId)
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
                val viewBinding = ViewUrlBottomBinding.bind(view)
                builder.setCustomView(view)
                val bottomSheet = builder.create()
                viewBinding.urlTv.text = url
                viewBinding.openTv.setOnClickListener {
                    url.openAsUrlOrWeb(
                        requireContext(),
                        conversationId,
                        parentFragmentManager,
                        lifecycleScope
                    )
                    bottomSheet.dismiss()
                }
                viewBinding.copyTv.setOnClickListener {
                    requireContext().getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    toast(R.string.copied_to_clipboard)
                    bottomSheet.dismiss()
                }
                bottomSheet.show()
            }

            override fun onMentionClick(identityNumber: String) {
                lifecycleScope.launch {
                    chatViewModel.findUserByIdentityNumberSuspend(identityNumber.replace("@", ""))?.let { user ->
                        if (user.userId == Session.getAccountId()!!) {
                            ProfileBottomSheetDialogFragment.newInstance()
                                .showNow(parentFragmentManager, ProfileBottomSheetDialogFragment.TAG)
                        } else {
                            showUserBottom(parentFragmentManager, user, conversationId)
                        }
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
                            showUserBottom(parentFragmentManager, it, conversationId)
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
                binding.chatControl.chatEt.hideKeyboard()
                MarkdownActivity.show(requireActivity(), messageItem.content!!, conversationId)
            }

            override fun onTranscriptClick(messageItem: MessageItem) {
                binding.chatControl.chatEt.hideKeyboard()
                ChatHistoryActivity.show(requireActivity(), messageItem.messageId, messageItem.conversationId, encryptCategory())
            }

            override fun onSayHi() {
                sendTextMessage("Hi")
            }

            override fun onOpenHomePage() {
                openBotHome()
            }

            override fun onLocationClick(messageItem: MessageItem) {
                val location = GsonHelper.customGson.fromJson(messageItem.content, LocationPayload::class.java)
                LocationActivity.show(requireContext(), location)
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
                    checkVoicePermissions {
                        initAudioSwitch()
                        voiceCall()
                    }
                }
            }

            override fun onTextDoubleClick(messageItem: MessageItem) {
                TextPreviewActivity.show(requireContext(), messageItem)
            }
        }
    }

    private val decoration by lazy {
        MixinHeadersDecoration(conversationAdapter)
    }

    private val mentionAdapter: MentionAdapter by lazy {
        MentionAdapter(
            object : OnUserClickListener {
                @SuppressLint("SetTextI18n")
                override fun onUserClick(user: User) {
                    binding.chatControl.chatEt.text ?: return
                    val selectionEnd = binding.chatControl.chatEt.selectionEnd
                    mentionReplace(binding.chatControl.chatEt, user, selectionEnd)
                    mentionAdapter.submitList(null)
                    binding.floatingLayout.hideMention()
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

    private var messageId: String? = null

    private val initialPositionMessageId: String? by lazy {
        requireArguments().getString(INITIAL_POSITION_MESSAGE_ID, null)
    }

    private var keyword: String? = null

    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private var app: App? = null

    private var isFirstMessage = false
    private var isFirstLoad = true
    private var isBottom = true
        set(value) {
            field = value
            binding.flagLayout.bottomFlag = !value
        }
    private var positionBeforeClickQuote: String? = null
    private var inGroup = true

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

    private lateinit var getForwardResult: ActivityResultLauncher<Pair<ArrayList<ForwardMessage>, String?>>
    private lateinit var getCombineForwardResult: ActivityResultLauncher<ArrayList<TranscriptMessage>>
    private lateinit var getChatHistoryResult: ActivityResultLauncher<Pair<String, Boolean>>
    private lateinit var getMediaResult: ActivityResultLauncher<MediaPagerActivity.MediaParam>
    lateinit var getEditorResult: ActivityResultLauncher<Pair<Uri, String?>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getForwardResult = registerForActivityResult(ForwardActivity.ForwardContract(), resultRegistry, ::callbackForward)
        getCombineForwardResult = registerForActivityResult(ForwardActivity.CombineForwardContract(), resultRegistry, ::callbackForward)
        getChatHistoryResult = registerForActivityResult(ChatHistoryContract(), resultRegistry, ::callbackChatHistory)
        getMediaResult = registerForActivityResult(MediaPagerActivity.MediaContract(), resultRegistry, ::callbackChatHistory)
        getEditorResult = registerForActivityResult(ImageEditorActivity.ImageEditorContract(), resultRegistry, ::callbackEditor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!anyCallServiceRunning(requireContext())) {
            initAudioSwitch()
        }
        recipient = requireArguments().getParcelable(RECIPIENT)
        messageId = requireArguments().getString(MESSAGE_ID, null)
        keyword = requireArguments().getString(KEY_WORD, null)
    }

    private val binding by viewBinding(FragmentConversationBinding::bind)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_conversation, container, false)

    override fun getContentView(): View = binding.root

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
                lifecycleScope.launch {
                    chatViewModel.markMentionRead(event.messageId, event.conversationId)
                }
            }
        RxBus.listen(CallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { event ->
                if (event.conversationId == conversationId) {
                    if (event.errorCode == ERROR_ROOM_FULL) {
                        alertDialogBuilder()
                            .setMessage(getString(R.string.call_group_full, GROUP_VOICE_MAX_COUNT))
                            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else if (event.errorCode == FORBIDDEN && event.action == LIST_KRAKEN_PEERS) {
                        if (viewDestroyed()) return@subscribe

                        binding.tapJoinView.root.isVisible = false
                    }
                }
            }

        checkPeerIfNeeded()
        checkTranscript()
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
        supportsNougat {
            binding.inputLayout.onMultiWindowModeChanged(requireActivity().isInMultiWindowMode)
        }
        binding.inputLayout.setOnKeyboardShownListener(this)
        binding.inputLayout.setOnKeyBoardHiddenListener(this)
        MixinApplication.conversationId = conversationId
        if (isGroup) {
            RxBus.listen(GroupEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe {
                    if (it.conversationId == conversationId) {
                        lifecycleScope.launch {
                            binding.groupDesc.text = chatViewModel.getAnnouncementByConversationId(conversationId)
                            binding.groupDesc.collapse()
                            binding.groupDesc.requestFocus()
                        }
                        binding.groupFlag.isVisible = true
                    }
                }
        }
        if (paused) {
            paused = false
            binding.chatRv.adapter?.notifyDataSetChanged()
        }
        if (binding.chatControl.getVisibleContainer() == null) {
            ViewCompat.getRootWindowInsets(binding.inputArea)?.let { windowInsetsCompat ->
                val imeHeight = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (imeHeight > 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    binding.inputLayout.openInputArea(binding.chatControl.chatEt)
                } else {
                    binding.inputLayout.forceClose(binding.chatControl.chatEt)
                }
            }
        }
        RxBus.listen(RecallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe { event ->
                if (conversationAdapter.selectSet.any { it.messageId == event.messageId }) {
                    closeTool()
                }
                binding.chatControl.replyView.messageItem?.let {
                    if (it.messageId == event.messageId) {
                        binding.chatControl.replyView.animateHeight(53.dp, 0)
                        binding.chatControl.replyView.messageItem = null
                    }
                }
            }
    }

    private var lastReadMessage: String? = null
    override fun onPause() {
        sensorManager.unregisterListener(this)
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            lastReadMessage = chatViewModel.findLastMessage(conversationId)
        }
        deleteDialog?.dismiss()
        super.onPause()
        paused = true
        binding.inputLayout.setOnKeyboardShownListener(null)
        binding.inputLayout.setOnKeyBoardHiddenListener(null)
        if (binding.chatControl.getVisibleContainer() == null) {
            ViewCompat.getRootWindowInsets(binding.inputArea)?.let { windowInsetsCompat ->
                val imeHeight = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (imeHeight <= 0) {
                    binding.inputLayout.forceClose()
                }
            }
        }
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
        val draftText = binding.chatControl.chatEt.text
        if (draftText != null) {
            chatViewModel.saveDraft(conversationId, draftText.toString())
        }
        if (OpusAudioRecorder.state != STATE_NOT_INIT) {
            OpusAudioRecorder.get(conversationId).stop()
        }
        if (binding.chatControl.isRecording) {
            binding.chatControl.cancelExternal()
        }
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        if (aodWakeLock.isHeld) {
            aodWakeLock.release()
        }
        snackbar?.dismiss()
        binding.chatRv.let { rv ->
            rv.children.forEach {
                val vh = rv.getChildViewHolder(it)
                if (vh != null && vh is BaseViewHolder) {
                    vh.stopListen()
                }
            }
        }
        super.onStop()
    }

    override fun onDestroyView() {
        chatViewModel.keyLivePagedListBuilder = null
        audioFile?.deleteOnExit()
        audioFile = null
        if (isAdded) {
            conversationAdapter.unregisterAdapterDataObserver(chatAdapterDataObserver)
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
        AudioPlayer.setStatusListener(null)
        AudioPlayer.release()
        context?.let {
            if (!anyCallServiceRunning(it)) {
                audioSwitch.safeStop()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            binding.chatControl.isRecording -> {
                alertDialogBuilder()
                    .setTitle(getString(R.string.chat_audio_discard_warning_title))
                    .setMessage(getString(R.string.chat_audio_discard_warning))
                    .setNeutralButton(getString(R.string.chat_capital_audio_discard_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.chat_capital_audio_discard_ok)) { dialog, _ ->
                        activity?.finish()
                        dialog.dismiss()
                    }
                    .show()
                true
            }
            binding.toolView.visibility == VISIBLE -> {
                closeTool()
                true
            }
            binding.chatControl.getVisibleContainer()?.isVisible == true -> {
                binding.chatControl.reset()
                true
            }
            binding.chatControl.isRecording -> {
                OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.CANCEL)
                binding.chatControl.cancelExternal()
                true
            }
            binding.chatControl.replyView.visibility == VISIBLE -> {
                binding.chatControl.replyView.animateHeight(53.dp, 0)
                true
            }
            else -> false
        }
    }

    private fun hideIfShowBottomSheet() {
        if (binding.stickerContainer.isVisible &&
            binding.menuContainer.isVisible &&
            binding.galleryContainer.isVisible
        ) {
            binding.chatControl.reset()
        }
        if (binding.chatControl.replyView.isVisible) {
            binding.chatControl.replyView.animateHeight(53.dp, 0)
        }
    }

    private fun closeTool() {
        conversationAdapter.selectSet.clear()
        if (!binding.chatRv.isComputingLayout) {
            conversationAdapter.notifyDataSetChanged()
        }
        binding.toolView.fadeOut()
    }

    private fun markRead() {
        conversationAdapter.markRead()
    }

    private var firstPosition = 0

    private fun initView() {
        if (requireActivity().booleanFromAttribute(R.attr.flag_night)) {
            binding.inputLayout.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_night)
        } else {
            binding.inputLayout.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat)
        }
        binding.chatRv.visibility = INVISIBLE
        if (binding.chatRv.adapter == null) {
            binding.chatRv.adapter = conversationAdapter
            conversationAdapter.listen(destroyScope)
        }
        binding.chatControl.callback = chatControlCallback
        binding.chatControl.activity = requireActivity()
        binding.chatControl.inputLayout = binding.inputLayout
        binding.chatControl.stickerContainer = binding.stickerContainer
        binding.chatControl.menuContainer = binding.menuContainer
        binding.chatControl.galleryContainer = binding.galleryContainer
        binding.chatControl.recordTipView = binding.recordTipTv
        binding.chatControl.setCircle(binding.recordCircle)
        binding.chatControl.chatEt.setCommitContentListener(
            object :
                ContentEditText.OnCommitContentListener {
                override fun commitContentAsync(
                    inputContentInfo: InputContentInfoCompat?,
                    flags: Int,
                    opts: Bundle?
                ) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (inputContentInfo != null) {
                            sendImageMessage(inputContentInfo.contentUri)
                        }
                    }
                }
            }
        )
        binding.chatRv.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
        binding.chatRv.addItemDecoration(decoration)
        binding.chatRv.itemAnimator = null

        binding.chatRv.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    setVisibleKey(recyclerView)
                    firstPosition = (binding.chatRv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (firstPosition > 0) {
                        if (isBottom) {
                            isBottom = false
                        }
                    } else {
                        if (!isBottom) {
                            isBottom = true
                        }
                        unreadTipCount = 0
                        binding.flagLayout.bottomCountFlag = false
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    setVisibleKey(recyclerView)
                }
            }
        )
        binding.chatRv.callback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val currentContainer = binding.chatControl.getDraggableContainer()
                if (currentContainer != null) {
                    dragChatControl(dis)
                }
            }

            override fun onRelease(fling: Int) {
                releaseChatControl(fling)
            }
        }

        binding.chatRv.setScrollingTouchSlop(SWAP_SLOT)

        initTouchHelper()

        binding.actionBar.leftIb.setOnClickListener {
            activity?.onBackPressed()
        }

        if (isGroup) {
            renderGroup()
        } else {
            renderUser(recipient!!)
        }
        binding.mentionRv.adapter = mentionAdapter
        binding.mentionRv.layoutManager = LinearLayoutManager(context)

        binding.flagLayout.downFlagLayout.setOnClickListener {
            if (binding.chatRv.scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                binding.chatRv.dispatchTouchEvent(
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
                binding.flagLayout.bottomCountFlag = false
            }
        }
        chatViewModel.searchConversationById(conversationId)
            .autoDispose(stopScope).subscribe(
                {
                    it?.draft?.let { str ->
                        if (isAdded) {
                            binding.chatControl.chatEt.setText(str)
                        }
                    }
                },
                {
                    Timber.e(it)
                }
            )
        binding.toolView.closeIv.setOnClickListener { activity?.onBackPressed() }
        binding.toolView.deleteIv.setOnClickListener {
            conversationAdapter.selectSet.filter { it.isAudio() }.forEach {
                if (AudioPlayer.isPlay(it.messageId)) {
                    AudioPlayer.pause()
                }
            }
            deleteMessage(conversationAdapter.selectSet.toList())
            closeTool()
        }
        binding.chatControl.replyView.replyCloseIv.setOnClickListener {
            binding.chatControl.replyView.messageItem = null
            binding.chatControl.replyView.animateHeight(53.dp, 0)
        }
        binding.toolView.copyIv.setOnClickListener {
            try {
                context?.getClipboardManager()?.setPrimaryClip(
                    ClipData.newPlainText(null, conversationAdapter.selectSet.valueAt(0)?.content)
                )
                toast(R.string.copied_to_clipboard)
            } catch (e: ArrayIndexOutOfBoundsException) {
            }
            closeTool()
        }
        binding.toolView.forwardIv.setOnClickListener {
            showForwardDialog()
        }
        binding.toolView.addStickerIv.setOnClickListener {
            if (conversationAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            val messageItem = conversationAdapter.selectSet.valueAt(0)
            messageItem?.let { m ->
                if (messageItem.isSticker() && m.stickerId != null) {
                    addSticker(m)
                } else if (messageItem.isImage()) {
                    val url = m.absolutePath(requireContext())
                    url?.let {
                        val uri = url.toUri()
                        val mimeType = getMimeType(uri, true)
                        if (mimeType?.isImageSupport() == true) {
                            StickerActivity.show(requireContext(), url = it, showAdd = true)
                        } else {
                            toast(R.string.sticker_add_invalid_format)
                        }
                    }
                }
            }
        }

        binding.toolView.replyIv.setOnClickListener {
            if (conversationAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            conversationAdapter.selectSet.valueAt(0)?.let {
                binding.chatControl.replyView.bind(it)
            }
            displayReplyView()
            closeTool()
        }

        binding.toolView.pinIv.setOnClickListener {
            if (conversationAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val action = (binding.toolView.pinIv.tag as PinAction?) ?: PinAction.PIN
                chatViewModel.sendPinMessage(
                    conversationId,
                    sender,
                    (binding.toolView.pinIv.tag as PinAction?) ?: PinAction.PIN,
                    conversationAdapter.selectSet
                )
                toast(
                    if (action == PinAction.PIN) {
                        R.string.pin_success
                    } else {
                        R.string.unpin_success
                    }
                )
                closeTool()
            }
        }
        binding.toolView.shareIv.setOnClickListener {
            val messageItem = conversationAdapter.selectSet.valueAt(0)
            Intent().apply {
                var uri: Uri? = try {
                    messageItem?.absolutePath()?.toUri()
                } catch (e: NullPointerException) {
                    null
                }
                if (messageItem == null || uri == null || uri.path == null) {
                    closeTool()
                    return@setOnClickListener
                }
                if (ContentResolver.SCHEME_CONTENT != uri.scheme) {
                    uri = requireContext().getUriForFile(File(uri.path!!))
                }
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val extraMimeTypes = arrayOf("text/plain", "audio/*", "image/*", "video/*")
                putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
                type = "application/*"

                val resInfoList = requireContext().packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    requireContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(Intent.createChooser(this, messageItem.mediaName))
                } catch (ignored: ActivityNotFoundException) {
                }
            }
            closeTool()
        }

        binding.groupDesc.movementMethod = LinkMovementMethod()
        binding.groupDesc.addAutoLinkMode(AutoLinkMode.MODE_URL)
        binding.groupDesc.setUrlModeColor(BaseViewHolder.LINK_COLOR)
        binding.groupDesc.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
        }
        binding.groupFlag.setOnClickListener {
            binding.groupDesc.expand()
        }
        binding.groupDesc.setOnClickListener {
            binding.groupDesc.expand()
        }
        binding.groupClose.setOnClickListener {
            requireActivity().sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(conversationId, false)
            binding.groupFlag.isVisible = false
        }
        binding.tapJoinView.root.setOnClickListener {
            if (!requireContext().networkConnected()) {
                toast(R.string.error_network)
                return@setOnClickListener
            }
            val isBusy = callState.isBusy()
            if (isBusy) {
                alertDialogBuilder()
                    .setMessage(getString(R.string.chat_call_warning_call))
                    .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }
            checkBlueToothConnectPermissions {
                initAudioSwitch()
                receiveInvite(requireContext(), conversationId, playRing = false)
            }
        }
        callState.observe(viewLifecycleOwner) { state ->
            if (!inGroup) {
                return@observe
            }
            binding.chatControl.calling = state != CallService.CallState.STATE_IDLE
            if (isGroup) {
                binding.tapJoinView.root.isVisible = callState.isPendingGroupCall(conversationId)
            } else {
                binding.tapJoinView.root.isVisible = false
            }
        }
        debugLongClick(
            binding.actionBar.titleContainer,
            {
                requireContext().getClipboardManager()
                    .setPrimaryClip(ClipData.newPlainText(null, conversationId))
            },
            {
                if (recipient?.identityNumber !in arrayOf("26832", "31911", "47762")) {
                    return@debugLongClick
                }

                val logFile = FileLogTree.getLogFile()
                if (logFile == null || logFile.length() <= 0) {
                    toast(R.string.error_file_exists)
                    return@debugLongClick
                }
                val attachment = Attachment(logFile.toUri(), logFile.name, "text/plain", logFile.length())
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
                    .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.action_send) { dialog, _ ->
                        sendAttachmentMessage(attachment)
                        dialog.dismiss()
                    }.show()
            }
        )
        bindData()
        bindPinMessage()
    }

    lateinit var itemTouchHelper: ItemTouchHelper
    private fun initTouchHelper() {
        val callback =
            ChatItemCallback(
                requireContext(),
                object : ChatItemCallback.ItemCallbackListener {
                    override fun onSwiped(position: Int) {
                        if (binding.chatRv.itemAnimator?.isRunning == true) return

                        try {
                            itemTouchHelper.attachToRecyclerView(null)
                            itemTouchHelper.attachToRecyclerView(binding.chatRv)
                        } catch (e: IllegalStateException) {
                            // workaround with RecyclerView.assertNotInLayoutOrScroll
                            Timber.w(e)
                        }
                        if (position >= 0) {
                            conversationAdapter.getItem(position)?.let {
                                binding.chatControl.replyView.bind(it)
                            }

                            displayReplyView()
                            closeTool()
                        }
                    }
                }
            )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.chatRv)
    }

    private fun addSticker(m: MessageItem) = lifecycleScope.launch(Dispatchers.IO) {
        if (viewDestroyed()) return@launch

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
                toast(R.string.add_success)
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
        val deleteDialogLayoutBinding = generateDeleteDialogLayout()
        deleteDialog = alertDialogBuilder()
            .setMessage(requireContext().resources.getQuantityString(R.plurals.chat_delete_message, messages.size, messages.size))
            .setView(deleteDialogLayoutBinding.root)
            .create()
        if (showRecall) {
            deleteDialogLayoutBinding.deleteEveryone.setOnClickListener {
                if (defaultSharedPreferences.getBoolean(Constants.Account.PREF_RECALL_SHOW, true)) {
                    deleteDialog?.dismiss()
                    deleteAlert(messages)
                    defaultSharedPreferences.putBoolean(Constants.Account.PREF_RECALL_SHOW, false)
                } else {
                    chatViewModel.sendRecallMessage(conversationId, sender, messages)
                    deleteDialog?.dismiss()
                }
            }
            deleteDialogLayoutBinding.deleteEveryone.visibility = VISIBLE
        } else {
            deleteDialogLayoutBinding.deleteEveryone.visibility = GONE
        }
        deleteDialogLayoutBinding.deleteMe.setOnClickListener {
            chatViewModel.deleteMessages(messages)
            deleteDialog?.dismiss()
        }
        deleteDialog?.show()
    }

    private var permissionAlert: AlertDialog? = null
    private fun checkFloatingPermission() =
        requireContext().checkInlinePermissions {
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions

            permissionAlert = AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.web_floating_permission)
                .setPositiveButton(R.string.live_setting) { dialog, _ ->
                    try {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${requireContext().packageName}")
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    dialog.dismiss()
                }.show()
        }

    private fun generateDeleteDialogLayout(): DialogDeleteBinding {
        return DialogDeleteBinding.inflate(LayoutInflater.from(requireActivity()), null, false)
            .apply {
                this.deleteCancel.setOnClickListener {
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

    private var messageLiveData: LiveData<PagedList<MessageItem>>? = null
    private lateinit var messageObserver: Observer<PagedList<MessageItem>>

    private fun bindLiveData(liveData: LiveData<PagedList<MessageItem>>, countable: Boolean) {
        messageLiveData = liveData
        if (countable) {
            messageLiveData?.observe(viewLifecycleOwner, messageObserver)
        } else {
            messageLiveData?.observeOnce(viewLifecycleOwner, messageObserver)
        }
    }

    private fun liveDataMessage(unreadCount: Int, unreadMessageId: String?, countable: Boolean) {
        var oldCount: Int = -1
        messageObserver = Observer { list ->
            if (Session.getAccount() == null) return@Observer

            if (oldCount == -1) {
                oldCount = list.size
            } else if (!isFirstLoad && !isBottom && list.size > oldCount) {
                unreadTipCount += (list.size - oldCount)
                oldCount = list.size
            } else if (isBottom) {
                unreadTipCount = 0
                oldCount = list.size
            }
            lifecycleScope.launch {
                conversationAdapter.hasBottomView = recipient?.relationship == UserRelationship.STRANGER.name &&
                    (
                        (isBot && list.isEmpty()) ||
                            (!isGroup && (!list.isEmpty()) && chatViewModel.isSilence(conversationId, sender.userId))
                        )
            }
            if (isFirstLoad && messageId == null && unreadCount > 0) {
                conversationAdapter.unreadMsgId = unreadMessageId
            } else if (lastReadMessage != null) {
                lifecycleScope.launch {
                    lastReadMessage?.let { id ->
                        val unreadMsgId = chatViewModel.findUnreadMessageByMessageId(
                            conversationId,
                            sender.userId,
                            id
                        )
                        if (unreadMsgId != null) {
                            conversationAdapter.unreadMsgId = unreadMsgId
                            lastReadMessage = null
                        }
                    }
                }
            }
            if (list.size > 0) {
                if (isFirstMessage) {
                    isFirstMessage = false
                }
                chatViewModel.markMessageRead(conversationId, sender.userId, (activity as? BubbleActivity)?.isBubbled == true)
            }
            conversationAdapter.submitList(list) {
                if (countable) return@submitList

                liveDataMessage(unreadCount, unreadMessageId, true)
            }
        }
        bindLiveData(chatViewModel.getMessages(conversationId, unreadCount, countable), countable)
    }

    private var unreadCount = 0
    private fun bindData() {
        unreadCount = requireArguments().getInt(UNREAD_COUNT, 0)
        liveDataMessage(unreadCount, initialPositionMessageId, !keyword.isNullOrEmpty())

        chatViewModel.getUnreadMentionMessageByConversationId(conversationId).observe(
            viewLifecycleOwner
        ) { mentionMessages ->
            binding.flagLayout.mentionCount = mentionMessages.size
            binding.flagLayout.mentionFlagLayout.setOnClickListener {
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

        if (isBot) {
            chatViewModel.updateRecentUsedBots(defaultSharedPreferences, recipient!!.userId)
            binding.chatControl.showBot(encryptCategory())
        } else {
            binding.chatControl.hideBot()
        }
        liveDataAppList()
    }

    private fun bindPinMessage() {
        binding.pinMessageLayout.conversationId = conversationId
        chatViewModel.getLastPinMessages(conversationId)
            .observe(viewLifecycleOwner) { messageItem ->
                if (messageItem != null) {
                    binding.pinMessageLayout.bind(messageItem) { messageId ->
                        scrollToMessage(messageId)
                    }
                    binding.pinMessageLayout.pin.setOnClickListener {
                        getChatHistoryResult.launch(Pair(conversationId, isGroup))
                    }
                }
            }
        chatViewModel.countPinMessages(conversationId)
            .observe(viewLifecycleOwner) { count ->
                binding.pinMessageLayout.isVisible = count > 0
            }
    }

    private fun liveDataAppList() {
        chatViewModel.getBottomApps(conversationId, recipient?.userId)?.observe(
            viewLifecycleOwner
        ) { list ->
            appList = list
            appList?.let {
                (parentFragmentManager.findFragmentByTag(MenuFragment.TAG) as? MenuFragment)?.setAppList(
                    it
                )
            }
        }
    }

    private var appList: List<AppItem>? = null

    private inline fun createConversation(crossinline action: () -> Unit) {
        if (isFirstMessage) {
            lifecycleScope.launch(Dispatchers.IO) {
                chatViewModel.initConversation(conversationId, recipient!!, sender)
                isFirstMessage = false

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        action()
                    }
                }
            }
        } else {
            action()
        }
    }

    private fun encryptCategory(): EncryptCategory = getEncryptedCategory(isBot, app)

    private fun sendImageMessage(uri: Uri, mimeType: String? = null) {
        createConversation {
            lifecycleScope.launch {
                val code = withContext(Dispatchers.IO) {
                    chatViewModel.sendImageMessage(
                        conversationId,
                        sender,
                        uri,
                        encryptCategory(),
                        mimeType,
                        getRelyMessage()
                    )
                }
                when (code) {
                    0 -> {
                        scrollToDown()
                        markRead()
                    }
                    -1 -> toast(R.string.error_image)
                    -2 -> toast(R.string.error_format)
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
                encryptCategory(),
                previewUrl
            )
            binding.chatRv.postDelayed(
                {
                    scrollToDown()
                },
                1000
            )
        }
    }

    override fun onCancel() {
        binding.chatControl.cancelExternal()
    }
    private var audioFile: File? = null
    override fun previewAudio(messageId: String, file: File, duration: Long, waveForm: ByteArray) {
        if (duration < 500) {
            file.deleteOnExit()
        } else {
            audioFile = file
            binding.chatControl.previewAudio(file, waveForm, duration) {
                sendAudio(messageId, file, duration, waveForm)
            }
        }
    }

    override fun sendAudio(messageId: String, file: File, duration: Long, waveForm: ByteArray) {
        if (duration < 500) {
            file.deleteOnExit()
        } else {
            audioFile = null
            createConversation {
                chatViewModel.sendAudioMessage(
                    conversationId,
                    messageId,
                    sender,
                    file,
                    duration,
                    waveForm,
                    encryptCategory(),
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
                encryptCategory(),
                replyMessage = getRelyMessage()
            )
            binding.chatRv.postDelayed(
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
                encryptCategory(),
                getRelyMessage()
            )

            scrollToDown()
            markRead()
        }
    }

    private fun sendStickerMessage(stickerId: String, albumId: String?) {
        createConversation {
            chatViewModel.sendStickerMessage(
                conversationId,
                sender,
                StickerMessagePayload(stickerId, albumId),
                encryptCategory()
            )
            scrollToDown()
            markRead()
        }
    }

    private fun sendContactMessage(userId: String) {
        createConversation {
            chatViewModel.sendContactMessage(conversationId, sender, userId, encryptCategory(), getRelyMessage())
            scrollToDown()
            markRead()
        }
    }

    private fun getRelyMessage(): MessageItem? {
        if (isAdded) {
            val messageItem = binding.chatControl.replyView.messageItem
            if (binding.chatControl.replyView.isVisible) {
                lifecycleScope.launch {
                    binding.chatControl.replyView.animateHeight(53.dp, 0)
                }
                binding.chatControl.replyView.messageItem = null
            }
            return messageItem
        }
        return null
    }

    private fun sendTextMessage(message: String, isSilentMessage: Boolean? = null) {
        if (message.isNotBlank()) {
            binding.chatControl.chatEt.setText("")
            createConversation {
                chatViewModel.sendTextMessage(conversationId, sender, message, encryptCategory(), isSilentMessage)
                scrollToDown()
                markRead()
            }
        }
    }

    private fun sendLocation(location: LocationPayload) {
        createConversation {
            chatViewModel.sendLocationMessage(conversationId, sender.userId, location, encryptCategory())
            scrollToDown()
            markRead()
        }
    }

    private fun sendReplyTextMessage(message: String, isSilentMessage: Boolean? = null) {
        if (message.isNotBlank() && binding.chatControl.replyView.messageItem != null) {
            binding.chatControl.chatEt.setText("")
            createConversation {
                chatViewModel.sendReplyTextMessage(
                    conversationId,
                    sender,
                    message,
                    binding.chatControl.replyView.messageItem!!,
                    encryptCategory(),
                    isSilentMessage
                )
                binding.chatControl.replyView.animateHeight(53.dp, 0)
                binding.chatControl.replyView.messageItem = null
                scrollToDown()
                markRead()
            }
        }
    }

    private var groupName: String? = null
    private var groupNumber: Int = 0

    @SuppressLint("SetTextI18n")
    private fun renderGroup() {
        binding.actionBar.avatarIv.visibility = VISIBLE
        binding.actionBar.avatarIv.setOnClickListener {
            showGroupBottomSheet(false)
        }
        chatViewModel.getConversationById(conversationId).observe(
            viewLifecycleOwner
        ) {
            it?.let {
                groupName = it.name
                binding.actionBar.setSubTitle(
                    groupName ?: "", requireContext().resources.getQuantityString(R.plurals.title_participants, groupNumber, groupNumber)
                )
                binding.actionBar.avatarIv.setGroup(it.iconUrl)
            }
        }
        chatViewModel.observeParticipantsCount(conversationId)
            .observe(
                viewLifecycleOwner
            ) { count ->
                groupNumber = count
                binding.actionBar.setSubTitle(
                    groupName ?: "", requireContext().resources.getQuantityString(R.plurals.title_participants, groupNumber, groupNumber)
                )

                lifecycleScope.launch {
                    val p = withContext(Dispatchers.IO) {
                        chatViewModel.findParticipantById(conversationId, Session.getAccountId()!!)
                    }
                    if (p != null) {
                        binding.chatControl.visibility = VISIBLE
                        binding.bottomCantSend.visibility = GONE
                    } else {
                        binding.chatControl.visibility = INVISIBLE
                        binding.bottomCantSend.visibility = VISIBLE
                        binding.chatControl.chatEt.hideKeyboard()
                    }

                    inGroup = p != null
                    if (!inGroup && callState.conversationId == conversationId && callState.isNotIdle()) {
                        callState.handleHangup(requireContext())
                    }
                }
            }
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
        binding.chatControl.toggleKeyboard(false)
    }

    override fun onKeyboardShown(height: Int) {
        binding.chatControl.toggleKeyboard(true)
    }

    private fun renderUser(user: User) {
        conversationAdapter.recipient = user
        renderUserInfo(user)
        chatViewModel.findUserById(user.userId).observe(
            viewLifecycleOwner
        ) {
            it?.let { u ->
                recipient = u
                if (u.isBot()) {
                    renderBot(u)
                }
                renderUserInfo(u)
            }
        }
        binding.actionBar.avatarIv.setOnClickListener {
            hideIfShowBottomSheet()
            showUserBottom(parentFragmentManager, user, conversationId)
        }
        binding.bottomUnblock.setOnClickListener {
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
        if (viewDestroyed()) return@launch

        app = chatViewModel.findAppById(user.appId!!)
        binding.chatControl.hintEncrypt(encryptCategory())
        if (app != null && app!!.creatorId == Session.getAccountId()) {
            val menuFragment = parentFragmentManager.findFragmentByTag(MenuFragment.TAG)
            if (menuFragment == null) {
                initMenuLayout(true)
            }
        }
    }

    private fun renderUserInfo(user: User) {
        binding.actionBar.setSubTitle(user.fullName ?: "", user.identityNumber)
        binding.actionBar.avatarIv.visibility = VISIBLE
        binding.actionBar.avatarIv.setTextSize(16f)
        binding.actionBar.avatarIv.setInfo(user.fullName, user.avatarUrl, user.userId)
        user.let {
            if (it.relationship == UserRelationship.BLOCKING.name) {
                binding.chatControl.visibility = INVISIBLE
                binding.bottomUnblock.visibility = VISIBLE
                binding.chatControl.chatEt.hideKeyboard()
            } else {
                binding.chatControl.visibility = VISIBLE
                binding.bottomUnblock.visibility = GONE
            }
        }
        if (user.isScam == true) {
            val closeScamTime = scamPreferences.getLong(user.userId, 0)
            if (System.currentTimeMillis() > closeScamTime) {
                binding.scamFlag.isVisible = true
                binding.warningClose.setOnClickListener {
                    scamPreferences.putLong(user.userId, System.currentTimeMillis() + INTERVAL_24_HOURS)
                    binding.scamFlag.isVisible = false
                }
            } else {
                binding.scamFlag.isVisible = false
            }
        }
    }

    private fun open(url: String, app: App?, appCard: AppCardData? = null) {
        binding.chatControl.chatEt.hideKeyboard()
        url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope, app, appCard)
    }

    private fun openInputAction(action: String): Boolean {
        if (action.startsWith("input:") && action.length > 6) {
            val msg = action.substring(6).trim()
            if (msg.isNotEmpty()) {
                sendTextMessage(msg)
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
            override fun onItemClick(pos: Int, item: Item, send: Boolean) {
                val uri = item.uri
                if (item.isVideo) {
                    if (send) {
                        sendVideoMessage(uri)
                    } else {
                        showPreview(uri, getString(R.string.action_send), true) { sendVideoMessage(uri) }
                    }
                } else if (item.isGif || item.isWebp) {
                    if (send) {
                        sendImageMessage(uri)
                    } else {
                        showPreview(uri, getString(R.string.action_send), false) { sendImageMessage(uri) }
                    }
                } else {
                    if (send) {
                        sendImageMessage(uri)
                    } else {
                        getEditorResult.launch(Pair(uri, getString(R.string.action_send)))
                    }
                }
                releaseChatControl(FLING_DOWN)
            }

            override fun onCameraClick() {
                openCamera()
            }
        }
        galleryAlbumFragment.rvCallback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                val currentContainer = binding.chatControl.getDraggableContainer()
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
                            .autoDispose(stopScope)
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
                        binding.chatControl.reset()
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
                                            parentFragmentManager.popBackStackImmediate()
                                        }
                                    },
                                    FriendsFragment.TAG
                                )
                                .addToBackStack(null)
                        }
                    }
                    MenuType.Voice -> {
                        binding.chatControl.reset()
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
                            checkVoicePermissions {
                                initAudioSwitch()
                                voiceCall()
                            }
                        }
                    }
                    MenuType.Location -> {
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
                    }
                    MenuType.App -> {
                        menu.app?.let { app ->
                            binding.chatControl.chatEt.hideKeyboard()
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
                override fun onStickerClick(stickerId: String, albumId: String?) {
                    if (isAdded) {
                        if (binding.stickerContainer.height != binding.inputLayout.keyboardHeight) {
                            binding.inputLayout.openInputArea(binding.chatControl.chatEt)
                        }
                        sendStickerMessage(stickerId, albumId)
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
                val currentContainer = binding.chatControl.getDraggableContainer()
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
        binding.chatRv.layoutManager?.scrollToPosition(0)
        if (firstPosition > PAGE_SIZE * 6) {
            conversationAdapter.notifyDataSetChanged()
        }
    }

    private fun scrollTo(
        position: Int,
        offset: Int = -1,
        delay: Long = 30,
        action: (() -> Unit)? = null
    ) {
        binding.chatRv.postDelayed(
            {
                if (isAdded) {
                    if (position == 0 && offset == 0) {
                        binding.chatRv.layoutManager?.scrollToPosition(0)
                    } else if (offset == -1) {
                        (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            0
                        )
                    } else {
                        (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            offset
                        )
                    }
                    if (abs(firstPosition - position) > PAGE_SIZE * 3) {
                        binding.chatRv.postDelayed(
                            {
                                (binding.chatRv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                    position,
                                    offset
                                )
                                action?.let { it() }
                            },
                            160
                        )
                    } else {
                        action?.let { it() }
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
        if (viewDestroyed()) return@launch

        val index = chatViewModel.findMessageIndex(conversationId, messageId)
        findMessageAction?.invoke(index)
        if (index == 0) {
            scrollTo(
                0,
                binding.chatRv.measuredHeight * 3 / 4,
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
            conversationAdapter.loadAround(index)
            if (index == conversationAdapter.itemCount - 1) {
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
                val lm = (binding.chatRv.layoutManager as LinearLayoutManager)
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
                        binding.chatRv.measuredHeight * 3 / 4,
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
                    getEditorResult.launch(Pair(it, getString(R.string.action_send)))
                }
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri?.let { imageUri ->
                getEditorResult.launch(Pair(imageUri, getString(R.string.action_send)))
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
                    .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.action_send) { dialog, _ ->
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
        binding.chatControl.getDraggableContainer() ?: return
        binding.inputLayout.drag(dis)
    }

    private fun releaseChatControl(fling: Int) {
        if (viewDestroyed()) return
        binding.chatControl.getDraggableContainer() ?: return
        binding.inputLayout.releaseDrag(fling) {
            binding.chatControl.reset()
        }
    }

    private fun showBottomSheet(messageItem: MessageItem) {
        var bottomSheet: BottomSheet? = null
        val builder = BottomSheet.Builder(requireActivity())
        val items = arrayListOf<BottomSheetItem>()
        if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
            items.add(
                BottomSheetItem(
                    getString(R.string.action_save_to_music),
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
                    getString(R.string.action_save_to_gallery),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    }
                )
            )
        } else {
            items.add(
                BottomSheetItem(
                    getString(R.string.action_save_to_downloads),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    }
                )
            )
        }
        items.add(
            BottomSheetItem(
                getString(R.string.action_open),
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

        @SuppressLint("InflateParams")
        override fun onSendClick(text: String) {
            if (binding.chatControl.replyView.isVisible && binding.chatControl.replyView.messageItem != null) {
                sendReplyTextMessage(text)
            } else {
                sendTextMessage(text)
            }
        }

        override fun onSendLongClick(text: String) {
            val popupWindow = PopupWindow(requireContext())
            popupWindow.isOutsideTouchable = true
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.contentView = LayoutInflater.from(requireContext()).inflate(R.layout.view_silence, null, false).apply {
                setOnClickListener {
                    if (binding.chatControl.replyView.isVisible && binding.chatControl.replyView.messageItem != null) {
                        sendReplyTextMessage(text, true)
                    } else {
                        sendTextMessage(text, true)
                    }
                    popupWindow.dismiss()
                }
            }
            requireContext().clickVibrate()
            PopupWindowCompat.showAsDropDown(popupWindow, binding.chatControl.anchorView, -200.dp, -110.dp, Gravity.END)
        }

        override fun onRecordStart(audio: Boolean) {
            AudioPlayer.pause()
            MusicPlayer.pause()
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

        override fun onRecordSend() {
            OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.SEND)
            if (!isNearToSensor && aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }

        override fun onRecordPreview() {
            OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.PREVIEW)
            if (!isNearToSensor && aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }

        override fun onRecordCancel() {
            OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.CANCEL)
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
            if (isGroup || isBot) {
                if (s.isNullOrEmpty()) {
                    binding.floatingLayout.hideMention()
                    return
                }
                val selectionStart = binding.chatControl.chatEt.selectionStart
                val selectionEnd = binding.chatControl.chatEt.selectionEnd
                if (selectionStart != selectionEnd || selectionStart <= 0) return
                val text = s.substring(0, selectionStart)
                if (binding.mentionRv.adapter != null && text.isNotEmpty() && mentionDisplay(text)) {
                    searchMentionUser(text)
                    binding.mentionRv.layoutManager?.smoothScrollToPosition(binding.mentionRv, null, 0)
                } else {
                    binding.floatingLayout.hideMention()
                }
            }
        }

        override fun onDelete() {}
    }

    private fun searchMentionUser(keyword: String) {
        lifecycleScope.launch {
            val mention = mentionEnd(keyword)
            val users = if (isBot) {
                chatViewModel.fuzzySearchBotGroupUser(conversationId, mention)
            } else {
                chatViewModel.fuzzySearchUser(conversationId, mention)
            }
            mentionAdapter.keyword = mention
            mentionAdapter.submitList(users)
            if (binding.mentionRv.isGone) {
                binding.floatingLayout.showMention(users.size)
            } else {
                binding.floatingLayout.animate2RightHeight(users.size)
            }
        }
    }

    private fun checkPeerIfNeeded() = lifecycleScope.launch {
        if (!isGroup) return@launch

        delay(1000)
        if (callState.getGroupCallStateOrNull(conversationId) != null) return@launch

        checkPeers(requireContext(), conversationId)
    }

    private var snackbar: Snackbar? = null
    private fun callbackForward(data: Intent?) {
        val selectItems = data?.getParcelableArrayListExtra<SelectItem>(ARGS_RESULT)
        if (selectItems.isNullOrEmpty()) return

        val selectItem = selectItems[0]
        this.selectItem = selectItem
        snackbar = Snackbar.make(binding.barLayout, getString(R.string.forward_success), Snackbar.LENGTH_LONG)
            .setAction(R.string.chat_go_check) {
                ConversationActivity.show(requireContext(), selectItem.conversationId, selectItem.userId)
            }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                (view.findViewById<TextView>(R.id.snackbar_text)).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }.apply {
                snackbar?.config(binding.barLayout.context)
            }
        snackbar?.show()
    }
    private fun callbackChatHistory(data: Intent?) {
        data?.getStringExtra(JUMP_ID)?.let { messageId ->
            binding.chatRv.postDelayed({
                scrollToMessage(messageId) {
                    positionBeforeClickQuote = null
                }
            }, 100)
        }
    }

    private fun callbackEditor(data: Intent?) {
        val uri = data?.getParcelableExtra<Uri>(ImageEditorActivity.ARGS_EDITOR_RESULT)
        if (uri != null) {
            sendImageMessage(uri)
        }
    }

    private fun displayReplyView() {
        if (!binding.chatControl.replyView.isVisible) binding.chatControl.replyView.animateHeight(0, 53.dp)
        if (binding.chatControl.isRecording) {
            OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.CANCEL)
            binding.chatControl.cancelExternal()
        }
        binding.chatControl.chatEt.showKeyboard()
    }

    private fun setVisibleKey(rv: RecyclerView, unreadCount: Int = 0) {
        val lm = rv.layoutManager as LinearLayoutManager
        val firstVisiblePosition: Int = lm.findFirstVisibleItemPosition()
        val firstKeyToLoad: Int = if (unreadCount <= 0) firstVisiblePosition else unreadCount
        chatViewModel.keyLivePagedListBuilder?.setFirstKeyToLoad(firstKeyToLoad)
    }

    private var transcriptDialog: AlertDialog? = null
    private fun generateTranscriptDialogLayout(): DialogImportMessageBinding {
        return DialogImportMessageBinding.inflate(LayoutInflater.from(requireActivity()), null, false)
            .apply {
                this.cancel.setOnClickListener {
                    deleteDialog?.dismiss()
                }
            }
    }

    private fun checkTranscript() {
        transcriptData?.let { transcriptData ->
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        transcriptDialog?.dismiss()
                        val transcriptDialogLayoutBinding = generateTranscriptDialogLayout()
                        transcriptDialog = alertDialogBuilder()
                            .setMessage(getString(R.string.chat_import_content, groupName ?: conversationAdapter.recipient?.fullName ?: ""))
                            .setView(transcriptDialogLayoutBinding.root)
                            .create()
                        transcriptDialogLayoutBinding.importChat.setOnClickListener {
                            sendTranscript(transcriptData)
                            transcriptDialog?.dismiss()
                        }
                        transcriptDialogLayoutBinding.sendChat.setOnClickListener {
                            sendFile(transcriptData)
                            transcriptDialog?.dismiss()
                        }
                        transcriptDialog?.show()
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
    }

    private fun sendTranscript(transcriptData: TranscriptData) {
        try {
            val importChatUtil = ImportChatUtil.get()
            val content = importChatUtil.generateTranscriptMessage(requireContext(), transcriptData.chatUri, transcriptData.documentUris)
            // todo
            // content?.notEmptyWithElse({ sendTranscriptMessage(content) }, { sendFileAlert(transcriptData) })
        } catch (e: Exception) {
            Timber.e(e)
            sendFileAlert(transcriptData)
        }
    }

    private fun sendFileAlert(transcriptData: TranscriptData) {
        alert(getString(R.string.chat_import_fail_content))
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.chat_send_file) { dialog, _ ->
                sendFile(transcriptData)
                dialog.dismiss()
            }
    }

    private fun sendFile(transcriptData: TranscriptData) {
        // todo
    }

    private var forwardDialog: AlertDialog? = null
    private fun generateForwardDialogLayout(): DialogForwardBinding {
        return DialogForwardBinding.inflate(LayoutInflater.from(requireActivity()), null, false)
            .apply {
                this.cancel.setOnClickListener {
                    forwardDialog?.dismiss()
                }
            }
    }

    private fun showForwardDialog() {
        forwardDialog?.dismiss()
        val unShareable = conversationAdapter.selectSet.find { it.isShareable() == false }
        if (unShareable != null) {
            toast(
                if (unShareable.isLive()) {
                    R.string.live_shareable_false
                } else {
                    R.string.app_card_shareable_false
                }
            )
            return
        }
        if (conversationAdapter.selectSet.size == 1) {
            forward()
        } else {
            val forwardDialogLayoutBinding = generateForwardDialogLayout()
            forwardDialog = alertDialogBuilder()
                .setMessage(getString(R.string.chat_forward_title))
                .setView(forwardDialogLayoutBinding.root)
                .create()
            forwardDialogLayoutBinding.forward.setOnClickListener {
                forward()
                forwardDialog?.dismiss()
            }
            // disable combine transcript
            forwardDialogLayoutBinding.combineForward.isVisible = !conversationAdapter.selectSet.any { it.isTranscript() }
            forwardDialogLayoutBinding.combineForward.setOnClickListener {
                combineForward()
                forwardDialog?.dismiss()
            }
            forwardDialog?.show()
        }
    }

    private fun forward() {
        lifecycleScope.launch {
            val list = chatViewModel.getSortMessagesByIds(conversationAdapter.selectSet)
            getForwardResult.launch(Pair(list, null))
            closeTool()
        }
    }

    private fun combineForward() {
        lifecycleScope.launch {
            val transcriptId = UUID.randomUUID().toString()
            val messages = conversationAdapter.selectSet.filter { m -> !m.canNotForward() }
                .sortedWith(compareBy { it.createdAt })
                .map { it.toTranscript(transcriptId) }
            val nonExistent = withContext(Dispatchers.IO) {
                messages.filter { m -> m.isAttachment() }
                    .mapNotNull { m -> Uri.parse(m.absolutePath()).path }.any { path ->
                        !File(path).exists()
                    }
            }
            if (nonExistent) {
                toast(R.string.error_file_exists)
            } else if (messages.isNotEmpty()) {
                getCombineForwardResult.launch(ArrayList(messages))
            }
            closeTool()
        }
    }

    private fun checkBlueToothConnectPermissions(callback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RxPermissions(this)
                .request(Manifest.permission.BLUETOOTH_CONNECT)
                .autoDispose(stopScope).subscribe({ granted ->
                    if (granted) {
                        callback.invoke()
                    } else {
                        context?.openPermissionSetting()
                    }
                }, {
                    reportException(it)
                })
        } else {
            callback.invoke()
        }
    }

    private fun checkVoicePermissions(callback: () -> Unit) {
        RxPermissions(this)
            .request(* (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT) } else { arrayOf(Manifest.permission.RECORD_AUDIO) }))
            .autoDispose(stopScope).subscribe({ granted ->
                if (granted) {
                    callback.invoke()
                } else {
                    context?.openPermissionSetting()
                }
            }, {
                reportException(it)
            })
    }

    private var initAudioSwitch = false
    private fun initAudioSwitch() {
        if (!initAudioSwitch && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            initAudioSwitch = true
            audioSwitch.start()
        }
    }
}
