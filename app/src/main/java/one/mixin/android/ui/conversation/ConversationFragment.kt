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
import android.view.WindowManager
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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.twilio.audioswitch.AudioSwitch
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.service.UtxoService
import one.mixin.android.databinding.DialogDeleteBinding
import one.mixin.android.databinding.DialogForwardBinding
import one.mixin.android.databinding.DialogImportMessageBinding
import one.mixin.android.databinding.FragmentConversationBinding
import one.mixin.android.databinding.ViewUrlBottomBinding
import one.mixin.android.db.fetcher.MessageFetcher
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.flow.MessageFlow.ANY_ID
import one.mixin.android.event.BlinkEvent
import one.mixin.android.event.CallEvent
import one.mixin.android.event.ExitEvent
import one.mixin.android.event.GroupEvent
import one.mixin.android.event.MentionReadEvent
import one.mixin.android.event.MessageEventAction
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.REQUEST_FILE
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.REQUEST_LOCATION
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.callPhone
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
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isAuto
import one.mixin.android.extension.isBluetoothHeadsetOrWiredHeadset
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.isStickerSupport
import one.mixin.android.extension.lateOneHours
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openEmail
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
import one.mixin.android.tip.Tip
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment.Companion.GROUP_VOICE_MAX_COUNT
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.LinkFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.buildEmptyTransferBiometricItem
import one.mixin.android.ui.common.message.ChatRoomHelper
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.adapter.GalleryCallback
import one.mixin.android.ui.conversation.adapter.MentionAdapter
import one.mixin.android.ui.conversation.adapter.MentionAdapter.OnUserClickListener
import one.mixin.android.ui.conversation.adapter.Menu
import one.mixin.android.ui.conversation.adapter.MenuType
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.chat.ChatItemCallback
import one.mixin.android.ui.conversation.chat.ChatItemCallback.Companion.SWAP_SLOT
import one.mixin.android.ui.conversation.chat.CompressedList
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
import one.mixin.android.ui.media.SharedMediaActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.oldwallet.OldTransactionFragment
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.MusicActivity
import one.mixin.android.ui.player.MusicService
import one.mixin.android.ui.player.collapse
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.setting.WallpaperManager
import one.mixin.android.ui.sticker.StickerActivity
import one.mixin.android.ui.sticker.StickerPreviewBottomSheetFragment
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.Attachment
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MusicPlayer
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.util.markdown.MarkwonUtil.Companion.getMiniMarkwon
import one.mixin.android.util.mention.mentionDisplay
import one.mixin.android.util.mention.mentionEnd
import one.mixin.android.util.mention.mentionReplace
import one.mixin.android.util.reportException
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.AppItem
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.PinMessageData
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
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.mediaExists
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
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetItem
import one.mixin.android.widget.ChatControlView
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import one.mixin.android.widget.ContentEditText
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.FLING_DOWN
import one.mixin.android.widget.LinearSmoothScrollerCustom
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
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
        const val MESSAGE_ID = "initial_position_message_id"
        const val TRANSCRIPT_DATA = "transcript_data"
        private const val KEY_WORD = "key_word"

        fun putBundle(
            conversationId: String?,
            recipientId: String?,
            keyword: String?,
            messageId: String? = null,
            transcriptData: TranscriptData? = null,
        ): Bundle =
            Bundle().apply {
                require(!(conversationId == null && recipientId == null)) { "lose data" }
                keyword?.let {
                    putString(KEY_WORD, keyword)
                }
                putString(CONVERSATION_ID, conversationId)
                putString(RECIPIENT_ID, recipientId)
                putString(MESSAGE_ID, messageId)
                putParcelable(TRANSCRIPT_DATA, transcriptData)
            }

        fun newInstance(bundle: Bundle) = ConversationFragment().apply { arguments = bundle }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun newInstance(
            bundle: Bundle,
            testRegistry: ActivityResultRegistry,
        ) = ConversationFragment(testRegistry).apply { arguments = bundle }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var callState: CallStateLiveData

    @Inject
    lateinit var chatRoomHelper: ChatRoomHelper

    @Inject
    lateinit var audioSwitch: AudioSwitch

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var utxoService: UtxoService

    private val chatViewModel by viewModels<ConversationViewModel>()

    private val transcriptData: TranscriptData? by lazy {
        requireArguments().getParcelableCompat(TRANSCRIPT_DATA, TranscriptData::class.java)
    }

    private fun showPreview(
        uri: Uri,
        okText: String? = null,
        isVideo: Boolean,
        action: (Uri, Float, Float) -> Unit,
    ) {
        val previewDialogFragment = PreviewDialogFragment.newInstance(isVideo)
        previewDialogFragment.show(parentFragmentManager, uri, okText, action)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateConversationInfo(
        messageId: String?,
        keyword: String?,
    ) {
        if (!::messageAdapter.isInitialized) {
            return
        }
        this.keyword = keyword
        messageAdapter.keyword = keyword
        if (messageId != null) {
            scrollToMessage(messageId)
            messageAdapter.notifyDataSetChanged()
        } else {
            // Force refresh of data
            lifecycleScope.launch {
                val (_, data) = messageFetcher.initMessages(conversationId, null, true)
                messageAdapter.refreshData(data)
                messageLayoutManager.scrollWithOffset(messageAdapter.itemCount - 1, messageRvOffset)
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
            toast(R.string.No_network_connection)
        }
    }

    private fun checkPinMessage() {
        if (messageAdapter.selectSet.valueAt(0).canNotPin()) {
            binding.toolView.pinIv.visibility = GONE
        } else {
            messageAdapter.selectSet.valueAt(0).messageId.let { messageId ->
                lifecycleScope.launch {
                    if (isGroup) {
                        val role =
                            withContext(Dispatchers.IO) {
                                chatViewModel.findParticipantById(
                                    conversationId,
                                    Session.getAccountId()!!,
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

    private val onItemListener: MessageAdapter.OnItemListener by lazy {
        @UnstableApi object : MessageAdapter.OnItemListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSelect(
                isSelect: Boolean,
                messageItem: MessageItem,
                position: Int,
            ) {
                if (isSelect) {
                    messageAdapter.addSelect(messageItem)
                } else {
                    messageAdapter.removeSelect(messageItem)
                }
                binding.toolView.countTv.text = messageAdapter.selectSet.size.toString()
                when {
                    messageAdapter.selectSet.isEmpty() -> binding.toolView.fadeOut()
                    messageAdapter.selectSet.size == 1 -> {
                        try {
                            if (messageAdapter.selectSet.valueAt(0).isText()) {
                                binding.toolView.copyIv.visibility = VISIBLE
                            } else {
                                binding.toolView.copyIv.visibility = GONE
                            }
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            binding.toolView.copyIv.visibility = GONE
                        }
                        if (messageAdapter.selectSet.valueAt(0).isData()) {
                            binding.toolView.shareIv.visibility = VISIBLE
                        } else {
                            binding.toolView.shareIv.visibility = GONE
                        }
                        if (messageAdapter.selectSet.valueAt(0).supportSticker()) {
                            binding.toolView.addStickerIv.visibility = VISIBLE
                        } else {
                            binding.toolView.addStickerIv.visibility = GONE
                        }
                        if (messageAdapter.selectSet.valueAt(0).canNotReply()) {
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
                if (messageAdapter.selectSet.size > 99 || messageAdapter.selectSet.any { it.canNotForward() }) {
                    binding.toolView.forwardIv.visibility = GONE
                } else {
                    binding.toolView.forwardIv.visibility = VISIBLE
                }
                messageAdapter.notifyDataSetChanged()
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onLongClick(
                messageItem: MessageItem,
                position: Int,
            ): Boolean {
                val b = messageAdapter.addSelect(messageItem)
                binding.toolView.countTv.text = messageAdapter.selectSet.size.toString()
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

                    if (messageAdapter.selectSet.any { it.canNotForward() }) {
                        binding.toolView.forwardIv.visibility = GONE
                    } else {
                        binding.toolView.forwardIv.visibility = VISIBLE
                    }
                    if (messageAdapter.selectSet.any { it.canNotReply() }) {
                        binding.toolView.replyIv.visibility = GONE
                    } else {
                        binding.toolView.replyIv.visibility = VISIBLE
                    }
                    checkPinMessage()
                    messageAdapter.notifyDataSetChanged()
                    binding.toolView.fadeIn()
                }
                return b
            }

            @SuppressLint("MissingPermission")
            override fun onRetryDownload(messageId: String) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
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
                            },
                        )
                } else {
                    chatViewModel.retryDownload(messageId)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onRetryUpload(messageId: String) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    RxPermissions(requireActivity())
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .autoDispose(stopScope)
                        .subscribe(
                            { granted ->
                                if (granted) {
                                    chatViewModel.retryUpload(messageId) {
                                        toast(R.string.Retry_upload_failed)
                                    }
                                } else {
                                    context?.openPermissionSetting()
                                }
                            },
                            {
                            },
                        )
                } else {
                    chatViewModel.retryUpload(messageId) {
                        toast(R.string.Retry_upload_failed)
                    }
                }
            }

            override fun onCancel(id: String) {
                chatViewModel.cancel(id, conversationId)
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

            override fun onImageClick(
                messageItem: MessageItem,
                view: View,
            ) {
                starTransition = true
                if (messageItem.isLive()) {
                    getMediaResult.launch(
                        MediaPagerActivity.MediaParam(
                            messageItem.conversationId,
                            messageItem.messageId,
                            messageItem,
                            MediaPagerActivity.MediaSource.Chat,
                        ),
                        MediaPagerActivity.getOptions(requireActivity(), view),
                    )
                    return
                }
                if (messageItem.mediaExists()) {
                    getMediaResult.launch(
                        MediaPagerActivity.MediaParam(
                            messageItem.conversationId,
                            messageItem.messageId,
                            messageItem,
                            MediaPagerActivity.MediaSource.Chat,
                        ),
                        MediaPagerActivity.getOptions(requireActivity(), view),
                    )
                } else {
                    toast(R.string.File_does_not_exist)
                }
            }

            override fun onStickerClick(messageItem: MessageItem) {
                messageItem.stickerId?.let { stickerId ->
                    StickerPreviewBottomSheetFragment.newInstance(stickerId)
                        .showNow(parentFragmentManager, StickerPreviewBottomSheetFragment.TAG)
                }
            }

            @TargetApi(Build.VERSION_CODES.O)
            override fun onFileClick(messageItem: MessageItem) {
                showBottomSheet(messageItem)
            }

            override fun onAudioFileClick(messageItem: MessageItem) {
                if (!MimeTypes.isAudio(messageItem.mediaMimeType)) return
                when {
                    binding.chatControl.isRecording -> showRecordingAlert()
                    MusicPlayer.isPlay(messageItem.messageId) -> MusicPlayer.pause()
                    else -> {
                        FloatingPlayer.getInstance().conversationId = conversationId
                        MusicService.playConversation(requireContext(), conversationId, messageItem.messageId)
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

            override fun onUserClick(userId: String) {
                chatViewModel.getUserById(userId).autoDispose(stopScope).subscribe(
                    {
                        it?.let {
                            showUserBottom(
                                parentFragmentManager,
                                it,
                                conversationId,
                                if (it.userId == recipient?.userId) {
                                    { getShareMediaResult.launch(Pair(conversationId, true)) }
                                } else {
                                    null
                                },
                            )
                        }
                    },
                    {
                        Timber.e(it)
                    },
                )
            }

            override fun onUrlClick(url: String) {
                url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
            }

            override fun onUrlLongClick(url: String) {
                val builder = BottomSheet.Builder(requireActivity())
                val view =
                    View.inflate(
                        ContextThemeWrapper(requireActivity(), R.style.Custom),
                        R.layout.view_url_bottom,
                        null,
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
                        lifecycleScope,
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
                            showUserBottom(
                                parentFragmentManager,
                                user,
                                conversationId,
                                if (user.userId == recipient?.userId) {
                                    { getShareMediaResult.launch(Pair(conversationId, true)) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }

            override fun onEmailClick(email: String) {
                requireContext().openEmail(email)
            }

            override fun onPhoneClick(phoneNumber: String) {
                requireContext().callPhone(phoneNumber)
            }

            override fun onAddClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.ADD.name,
                            user.fullName,
                        ),
                    )
                }
            }

            override fun onBlockClick() {
                recipient?.let { user ->
                    chatViewModel.updateRelationship(
                        RelationshipRequest(
                            user.userId,
                            RelationshipAction.BLOCK.name,
                            user.fullName,
                        ),
                    )
                }
            }

            override fun onActionClick(
                action: String,
                userId: String,
            ) {
                if (openInputAction(action)) return

                lifecycleScope.launch {
                    val app = chatViewModel.findAppById(userId)
                    action.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope, app)
                }
            }

            override fun onAppCardClick(
                appCard: AppCardData,
                userId: String,
            ) {
                if (openInputAction(appCard.action)) return

                open(appCard.action, null, appCard)
            }

            override fun onBillClick(messageItem: MessageItem) {
                if (messageItem.type == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name) {
                    activity?.addFragment(
                        this@ConversationFragment,
                        TransactionFragment.newInstance(
                            assetId = messageItem.assetId,
                            snapshotId = messageItem.snapshotId,
                        ),
                        TransactionFragment.TAG,
                    )
                } else {
                    activity?.addFragment(
                        this@ConversationFragment,
                        OldTransactionFragment.newInstance(
                            assetId = messageItem.assetId,
                            snapshotId = messageItem.snapshotId,
                        ),
                        OldTransactionFragment.TAG,
                    )
                }
            }

            override fun onContactCardClick(userId: String) {
                if (userId == Session.getAccountId()) {
                    ProfileBottomSheetDialogFragment.newInstance().showNow(
                        parentFragmentManager,
                        UserBottomSheetDialogFragment.TAG,
                    )
                    return
                }
                chatViewModel.getUserById(userId).autoDispose(stopScope).subscribe(
                    {
                        it?.let {
                            showUserBottom(
                                parentFragmentManager,
                                it,
                                conversationId,
                                if (it.userId == recipient?.userId) {
                                    { getShareMediaResult.launch(Pair(conversationId, true)) }
                                } else {
                                    null
                                },
                            )
                        }
                    },
                    {
                        Timber.e(it)
                    },
                )
            }

            override fun onQuoteMessageClick(
                messageId: String,
                quoteMessageId: String?,
            ) {
                quoteMessageId?.let { quoteMsg ->
                    scrollToMessage(quoteMsg) {
                        positionBeforeClickQuote = messageId
                    }
                }
            }

            override fun onPostClick(
                view: View,
                messageItem: MessageItem,
            ) {
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
                            .setMessage(getString(R.string.call_on_another_call_hint))
                            .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                } else {
                    if (checkFloatingPermission()) {
                        checkVoicePermissions {
                            initAudioSwitch()
                            voiceCall()
                        }
                    } else {
                        requireActivity().showPipPermissionNotification(MusicActivity::class.java, getString(R.string.web_floating_permission))
                    }
                }
            }

            override fun onTextDoubleClick(messageItem: MessageItem) {
                TextPreviewActivity.show(requireContext(), messageItem)
            }
        }
    }

    private val decoration by lazy {
        MixinHeadersDecoration(messageAdapter)
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
            },
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

    private val initialMessageId: String? by lazy {
        requireArguments().getString(MESSAGE_ID, null)
    }

    private var keyword: String? = null

    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private var app: App? = null

    private var isFirstMessage = false
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
        MixinApplication.appContext.getSystemService()!!
    }

    private val wakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "mixin")
    }
    private val aodWakeLock by lazy {
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "mixin",
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

    // for testing
    lateinit var getForwardResult: ActivityResultLauncher<Pair<ArrayList<ForwardMessage>, String?>>
    private lateinit var getCombineForwardResult: ActivityResultLauncher<ArrayList<TranscriptMessage>>
    private lateinit var getChatHistoryResult: ActivityResultLauncher<Triple<String, Boolean, Int>>
    private lateinit var getMediaResult: ActivityResultLauncher<MediaPagerActivity.MediaParam>
    private lateinit var getShareMediaResult: ActivityResultLauncher<Pair<String, Boolean>>
    lateinit var getEditorResult: ActivityResultLauncher<Pair<Uri, String?>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getForwardResult = registerForActivityResult(ForwardActivity.ForwardContract(), resultRegistry, ::callbackForward)
        getCombineForwardResult = registerForActivityResult(ForwardActivity.CombineForwardContract(), resultRegistry, ::callbackForward)
        getChatHistoryResult = registerForActivityResult(ChatHistoryContract(), resultRegistry, ::callbackChatHistory)
        getMediaResult = registerForActivityResult(MediaPagerActivity.MediaContract(), resultRegistry, ::callbackChatHistory)
        getShareMediaResult = registerForActivityResult(SharedMediaActivity.SharedMediaContract(), resultRegistry, ::callbackChatHistory)
        getEditorResult = registerForActivityResult(ImageEditorActivity.ImageEditorContract(), resultRegistry, ::callbackEditor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!anyCallServiceRunning(requireContext())) {
            initAudioSwitch()
        }
        recipient = requireArguments().getParcelableCompat(RECIPIENT, User::class.java)
        keyword = requireArguments().getString(KEY_WORD, null)
    }

    private val binding by viewBinding(FragmentConversationBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_conversation, container, false)

    override fun getContentView(): View = binding.root

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                chatRoomHelper.markMentionRead(event.messageId, event.conversationId)
            }
        RxBus.listen(CallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { event ->
                if (event.conversationId == conversationId) {
                    if (event.errorCode == ERROR_ROOM_FULL) {
                        alertDialogBuilder()
                            .setMessage(getString(R.string.Group_call_participants_limit_hint, GROUP_VOICE_MAX_COUNT))
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
        initMessageRecyclerView()
        bindData()
        bindPinMessage()
        checkPeerIfNeeded()
        checkTranscript()
    }

    private var paused = false
    private var starTransition = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
            SensorManager.SENSOR_DELAY_NORMAL,
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
                .autoDispose(pauseScope)
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
        }
        if (binding.chatControl.getVisibleContainer() == null) {
            ViewCompat.getRootWindowInsets(binding.inputArea)?.let { windowInsetsCompat ->
                val imeHeight = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (imeHeight <= 0) {
                    binding.inputLayout.forceClose(binding.chatControl.chatEt)
                }
            }
        }
        RxBus.listen(RecallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(pauseScope)
            .subscribe { event ->
                if (!::messageAdapter.isInitialized) {
                    return@subscribe
                }
                if (messageAdapter.selectSet.any { it.messageId == event.messageId }) {
                    closeTool()
                }
                binding.chatControl.replyView.messageItem?.let {
                    if (it.messageId == event.messageId) {
                        binding.chatControl.replyView.animateHeight(53.dp, 0)
                        binding.chatControl.replyView.messageItem = null
                    }
                }
            }
        chatRoomHelper.markMessageRead(conversationId)
        chatViewModel.markMessageRead(conversationId, (activity as? BubbleActivity)?.isBubbled == true)
    }

    private var lastReadMessage: String? = null

    override fun onPause() {
        // don't stop audio player if triggered by screen off
        if (powerManager.isInteractive) {
            AudioPlayer.pause()
        }
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
        MixinApplication.conversationId = null
    }

    @SuppressLint("WakelockTimeout")
    override fun onStatusChange(status: Int) {
        if (isNearToSensor) return
        if (status == STATUS_PLAY) {
            if (!aodWakeLock.isHeld) {
                aodWakeLock.acquire(10 * 60 * 1000L)
            }
        } else {
            if (aodWakeLock.isHeld) {
                aodWakeLock.release()
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
    }

    private var isNearToSensor: Boolean = false

    override fun onSensorChanged(event: SensorEvent?) {
        if (callState.isNotIdle()) return

        val values = event?.values ?: return
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            isNearToSensor = values[0] < 5.0f && values[0] != sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.maximumRange
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

    override fun onStart() {
        super.onStart()
        if (!binding.chatControl.isPreviewAudio()) {
            lifecycleScope.launch {
                val previewAudio =
                    withContext(Dispatchers.IO) {
                        OpusAudioRecorder.getAudioPreview(requireContext(), conversationId)
                    } ?: return@launch
                this@ConversationFragment.previewAudio(
                    previewAudio.messageId,
                    File(previewAudio.path),
                    previewAudio.duration,
                    previewAudio.waveForm,
                )
            }
        }
    }

    override fun onStop() {
        markRead()
        val draftText = binding.chatControl.chatEt.text?.toString() ?: ""
        chatRoomHelper.saveDraft(conversationId, draftText)

        if (OpusAudioRecorder.state != STATE_NOT_INIT) {
            OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.PREVIEW, vibrate = false, false)
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
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        snackBar?.dismiss()
        binding.messageRv.let { rv ->
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
        audioFile?.deleteOnExit()
        audioFile = null
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
        AudioPlayer.pause()
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
                    .setNeutralButton(getString(R.string.Continue)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.DISCARD)) { dialog, _ ->
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

    @SuppressLint("NotifyDataSetChanged")
    private fun closeTool() {
        messageAdapter.selectSet.clear()
        if (!binding.messageRv.isComputingLayout) {
            messageAdapter.notifyDataSetChanged()
        }
        binding.toolView.fadeOut()
    }

    private fun markRead() {
        if (!::messageAdapter.isInitialized) {
            return
        }
        messageAdapter.markRead()
    }

    @Inject
    lateinit var messageFetcher: MessageFetcher

    private lateinit var messageAdapter: MessageAdapter
    private val messageLayoutManager by lazy {
        object : LinearLayoutManager(requireContext()) {
            fun scrollWithOffset(
                position: Int,
                offset: Int,
            ) {
                scrollToPositionWithOffset(messageAdapter.layoutPosition(position), offset)
            }

            fun scroll() {
                val targetPosition = messageAdapter.itemCount
                findFirstVisibleItemPosition().let { firstPosition ->
                    if (abs(targetPosition - firstPosition) > MessageFetcher.PAGE_SIZE) {
                        scrollToPositionWithOffset(targetPosition - MessageFetcher.PAGE_SIZE / 2, 0)
                    }
                }
                val linearSmoothScroller =
                    LinearSmoothScrollerCustom(
                        requireContext(),
                        LinearSmoothScrollerCustom.POSITION_END,
                    )
                linearSmoothScroller.targetPosition = targetPosition
                startSmoothScroll(linearSmoothScroller)
            }
        }.apply {
            stackFromEnd = true
        }
    }

    private val messageRvOffset: Int
        get() {
            return binding.messageRv.measuredHeight / 4
        }

    private val previousAction = fun(id: String) {
        lifecycleScope.launch {
            val pageData = messageFetcher.previousPage(conversationId, id)
            if (pageData.isNotEmpty()) {
                if (viewDestroyed()) return@launch

                binding.messageRv.post {
                    messageAdapter.submitPrevious(pageData)
                }
            }
        }
    }

    private val nextAction = fun(id: String) {
        lifecycleScope.launch {
            val pageData = messageFetcher.nextPage(conversationId, id)
            if (pageData.isNotEmpty()) {
                if (viewDestroyed()) return@launch

                binding.messageRv.post {
                    messageAdapter.submitNext(pageData)
                }
            }
        }
    }

    private fun initView() {
        binding.inputLayout.backgroundImage = WallpaperManager.getWallpaper(requireContext())
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
                    opts: Bundle?,
                ) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (inputContentInfo != null) {
                            sendImageMessage(inputContentInfo.contentUri, false, null, true)
                        }
                    }
                }
            },
        )

        binding.messageRv.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    val lastPosition = (binding.messageRv.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (lastPosition != messageAdapter.itemCount - 1) {
                        if (isBottom) {
                            isBottom = false
                        }
                    } else {
                        if (!isBottom) {
                            isBottom = true
                        }
                        binding.flagLayout.unreadCount = 0
                        binding.flagLayout.bottomCountFlag = false
                    }
                }
            },
        )
        binding.messageRv.callback =
            object : DraggableRecyclerView.Callback {
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

        binding.messageRv.setScrollingTouchSlop(SWAP_SLOT)

        initTouchHelper()

        binding.actionBar.leftIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        if (isGroup) {
            renderGroup()
        } else {
            renderUser(recipient!!)
        }
        binding.mentionRv.adapter = mentionAdapter
        binding.mentionRv.layoutManager = LinearLayoutManager(context)

        binding.flagLayout.downFlagLayout.setOnClickListener {
            if (binding.messageRv.scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                binding.messageRv.dispatchTouchEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL,
                        0f,
                        0f,
                        0,
                    ),
                )
            }
            if (positionBeforeClickQuote != null) {
                scrollToMessage(positionBeforeClickQuote!!) {
                    positionBeforeClickQuote = null
                }
            } else {
                scrollToDown()
                binding.flagLayout.unreadCount = 0
                binding.flagLayout.bottomCountFlag = false
            }
        }
        lifecycleScope.launch {
            val conversationDraft =
                withContext(SINGLE_DB_THREAD) {
                    chatViewModel.getConversationDraftById(conversationId)
                } ?: ""
            if (!viewDestroyed() && conversationDraft.isNotEmpty()) {
                binding.chatControl.chatEt.setText(conversationDraft)
            }
        }
        binding.toolView.closeIv.setOnClickListener {
            if (binding.toolView.isVisible) {
                closeTool()
            } else {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
        binding.toolView.deleteIv.setOnClickListener {
            messageAdapter.selectSet.filter { it.isAudio() }.forEach {
                if (AudioPlayer.isPlay(it.messageId)) {
                    AudioPlayer.pause()
                }
            }
            deleteMessage(messageAdapter.selectSet.toList())
            closeTool()
        }
        binding.chatControl.replyView.replyCloseIv.setOnClickListener {
            binding.chatControl.replyView.messageItem = null
            binding.chatControl.replyView.animateHeight(53.dp, 0)
        }
        binding.toolView.copyIv.setOnClickListener {
            try {
                context?.getClipboardManager()?.setPrimaryClip(
                    ClipData.newPlainText(null, messageAdapter.selectSet.valueAt(0).content),
                )
                toast(R.string.copied_to_clipboard)
            } catch (_: ArrayIndexOutOfBoundsException) {
            }
            closeTool()
        }
        binding.toolView.forwardIv.setOnClickListener {
            showForwardDialog()
        }
        binding.toolView.addStickerIv.setOnClickListener {
            if (messageAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            val messageItem = messageAdapter.selectSet.valueAt(0)
            messageItem.let { m ->
                if (messageItem.isSticker() && m.stickerId != null) {
                    addSticker(m)
                } else if (messageItem.isImage()) {
                    val url = m.absolutePath(requireContext())
                    url?.let {
                        val uri = url.toUri()
                        val mimeType = getMimeType(uri, true)
                        if (mimeType?.isStickerSupport() == true) {
                            StickerActivity.show(requireContext(), url = it, showAdd = true)
                            closeTool()
                        } else {
                            toast(R.string.Invalid_sticker_format)
                        }
                    }
                }
            }
        }

        binding.toolView.replyIv.setOnClickListener {
            if (messageAdapter.selectSet.isEmpty()) {
                return@setOnClickListener
            }
            messageAdapter.selectSet.valueAt(0).let {
                binding.chatControl.replyView.bind(it)
            }
            displayReplyView()
            closeTool()
        }

        binding.toolView.pinIv.setOnClickListener {
            val pinMessages =
                messageAdapter.selectSet.map {
                    PinMessageData(it.messageId, it.conversationId, requireNotNull(it.type), it.content, nowInUtc())
                }
            val action = (binding.toolView.pinIv.tag as PinAction?) ?: PinAction.PIN
            if (pinMessages.isEmpty()) {
                return@setOnClickListener
            }
            lifecycleScope.launch {
                chatViewModel.sendPinMessage(
                    conversationId,
                    sender,
                    (binding.toolView.pinIv.tag as PinAction?) ?: PinAction.PIN,
                    pinMessages,
                )
                toast(
                    if (action == PinAction.PIN) {
                        R.string.Message_pinned
                    } else {
                        R.string.Message_unpinned
                    },
                )
                closeTool()
            }
        }
        binding.toolView.shareIv.setOnClickListener {
            val messageItem = messageAdapter.selectSet.valueAt(0)
            Intent().apply {
                var uri: Uri? =
                    try {
                        messageItem.absolutePath()?.toUri()
                    } catch (e: NullPointerException) {
                        null
                    }
                if (uri == null || uri.path == null) {
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
        binding.groupDesc.setUrlModeColor(LINK_COLOR)
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
                toast(R.string.Network_error)
                return@setOnClickListener
            }
            val isBusy = callState.isBusy()
            if (isBusy) {
                alertDialogBuilder()
                    .setMessage(getString(R.string.call_on_another_call_hint))
                    .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }
            if (checkFloatingPermission()) {
                checkBlueToothConnectPermissions {
                    initAudioSwitch()
                    receiveInvite(requireContext(), conversationId, playRing = false)
                }
            } else {
                requireActivity().showPipPermissionNotification(MusicActivity::class.java, getString(R.string.web_floating_permission))
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
            },
        )
    }

    lateinit var itemTouchHelper: ItemTouchHelper

    private fun initTouchHelper() {
        val callback =
            ChatItemCallback(
                requireContext(),
                object : ChatItemCallback.ItemCallbackListener {
                    override fun onSwiped(position: Int) {
                        if (binding.messageRv.itemAnimator?.isRunning == true) return

                        try {
                            itemTouchHelper.attachToRecyclerView(null)
                            itemTouchHelper.attachToRecyclerView(binding.messageRv)
                        } catch (e: IllegalStateException) {
                            // workaround with RecyclerView.assertNotInLayoutOrScroll
                            Timber.w(e)
                        }
                        if (position >= 0) {
                            messageAdapter.getItem(position)?.let {
                                binding.chatControl.replyView.bind(it)
                            }

                            displayReplyView()
                            closeTool()
                        }
                    }
                },
            )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.messageRv)
    }

    private fun addSticker(m: MessageItem) =
        lifecycleScope.launch(Dispatchers.IO) {
            if (viewDestroyed()) return@launch

            val request = StickerAddRequest(stickerId = m.stickerId)
            val r =
                try {
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
                    toast(R.string.Add_success)
                }
            } else {
                ErrorHandler.handleMixinError(
                    r.errorCode,
                    r.errorDescription,
                    getString(R.string.Add_sticker_failed),
                )
            }
        }

    private var deleteDialog: AlertDialog? = null

    private fun deleteMessage(messages: List<MessageItem>) {
        deleteDialog?.dismiss()
        val showRecall =
            messages.all { item ->
                item.userId == sender.userId && item.status != MessageStatus.SENDING.name && !item.createdAt.lateOneHours() && item.canRecall()
            }
        val deleteDialogLayoutBinding = generateDeleteDialogLayout()
        deleteDialog =
            alertDialogBuilder()
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

            permissionAlert =
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.web_floating_permission)
                    .setPositiveButton(R.string.Settings) { dialog, _ ->
                        try {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${requireContext().packageName}"),
                                ),
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
        deleteDialog =
            alertDialogBuilder()
                .setMessage(getString(R.string.chat_recall_delete_alert))
                .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                    chatViewModel.sendRecallMessage(conversationId, sender, messages)
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.Learn_More) { dialog, _ ->
                    context?.openUrl(getString(R.string.chat_delete_url))
                    dialog.dismiss()
                }
                .create()
        deleteDialog?.show()
    }

    private fun initMessageRecyclerView() {
        lifecycleScope.launch {
            // init message data
            val (position, data, unreadMessageId) = messageFetcher.initMessages(conversationId, initialMessageId)
            if (isFirstMessage && data.isNotEmpty()) {
                isFirstMessage = false
            }
            messageAdapter =
                MessageAdapter(
                    CompressedList(data),
                    getMiniMarkwon(requireActivity()),
                    onItemListener,
                    previousAction,
                    nextAction,
                    isGroup = isGroup,
                    unreadMessageId = if (initialMessageId != null) null else unreadMessageId,
                    recipient = recipient,
                    isBot = isBot,
                    isSecret = encryptCategory() != EncryptCategory.PLAIN,
                    keyword = keyword,
                )
            binding.messageRv.adapter = messageAdapter
            binding.messageRv.addItemDecoration(decoration)
            binding.messageRv.itemAnimator = null
            binding.messageRv.layoutManager = messageLayoutManager
            // Initialization RecyclerView position
            if (position >= 0) {
                if (unreadMessageId != null) {
                    messageLayoutManager.scrollWithOffset(position, messageRvOffset)
                }
                if (initialMessageId != null && unreadMessageId != null) {
                    launch {
                        delay(100)
                        RxBus.publish(BlinkEvent(unreadMessageId))
                    }
                }
            }
            // The first time the load
            chatRoomHelper.markMessageRead(conversationId)
            chatViewModel.markMessageRead(conversationId, (activity as? BubbleActivity)?.isBubbled == true)
            MessageFlow.collect({ event ->
                event.conversationId == ANY_ID || event.conversationId == conversationId
            }, { event ->
                if (viewDestroyed()) return@collect

                when (event.action) {
                    MessageEventAction.INSERT -> {
                        if (messageFetcher.isBottom()) {
                            val message = messageFetcher.findMessageById(event.ids)
                            if (message.isNotEmpty()) {
                                (binding.messageRv.adapter as MessageAdapter).insert(message)
                            }
                            if (isBottom) {
                                scrollToDown()
                            } else {
                                binding.flagLayout.unreadCount += event.ids.size
                                binding.flagLayout.bottomCountFlag = true
                            }
                            if (message.any { it.userId == sender.userId }) {
                                messageAdapter.hasBottomView = false
                            }
                        } else {
                            binding.flagLayout.unreadCount += event.ids.size
                            binding.flagLayout.bottomCountFlag = true
                        }
                        if (!paused) {
                            chatRoomHelper.markMessageRead(conversationId)
                            chatViewModel.markMessageRead(
                                conversationId,
                                (activity as? BubbleActivity)?.isBubbled == true,
                            )
                        }
                    }
                    MessageEventAction.UPDATE -> {
                        val messages = messageFetcher.findMessageById(event.ids)
                        if (messages.isNotEmpty()) {
                            (binding.messageRv.adapter as MessageAdapter).update(messages)
                        }
                    }
                    MessageEventAction.DELETE -> {
                        if (event.ids.isNotEmpty()) {
                            (binding.messageRv.adapter as MessageAdapter).delete(event.ids)
                        }
                    }
                    MessageEventAction.RELATIIONSHIP -> {
                        messageAdapter.hasBottomView = recipient?.relationship == UserRelationship.STRANGER.name &&
                            chatViewModel.isSilence(
                                conversationId,
                                sender.userId,
                            )
                    }
                }
            })

            messageAdapter.hasBottomView =
                recipient?.relationship == UserRelationship.STRANGER.name &&
                chatViewModel.isSilence(
                    conversationId,
                    sender.userId,
                )
        }
    }

    private fun bindData() {
        chatViewModel.getUnreadMentionMessageByConversationId(conversationId).observe(
            viewLifecycleOwner,
        ) { mentionMessages ->
            binding.flagLayout.mentionCount = mentionMessages.size
            binding.flagLayout.mentionFlagLayout.setOnClickListener {
                lifecycleScope.launch {
                    if (mentionMessages.isEmpty()) {
                        return@launch
                    }
                    val messageId = mentionMessages.first().messageId
                    scrollToMessage(messageId) {
                        chatRoomHelper.markMentionRead(messageId, conversationId)
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
                        getChatHistoryResult.launch(Triple(conversationId, isGroup, pinCount))
                    }
                }
            }
        chatViewModel.countPinMessages(conversationId)
            .observe(viewLifecycleOwner) { count ->
                pinCount = count
                binding.pinMessageLayout.isVisible = count > 0
            }
    }

    private fun liveDataAppList() {
        chatViewModel.getBottomApps(conversationId, recipient?.userId)?.observe(
            viewLifecycleOwner,
        ) { list ->
            appList = list
            appList?.let {
                (parentFragmentManager.findFragmentByTag(MenuFragment.TAG) as? MenuFragment)?.setAppList(
                    it,
                )
            }
        }
    }

    private var pinCount: Int = 0
    private var appList: List<AppItem>? = null

    private inline fun createConversation(crossinline action: () -> Unit) {
        if (isFirstMessage) {
            lifecycleScope.launch(Dispatchers.IO) {
                chatViewModel.initConversation(conversationId, recipient!!, sender)
                isFirstMessage = false

                withContext(Dispatchers.Main) {
                    if (!viewDestroyed()) {
                        action()
                    }
                }
            }
        } else {
            action()
        }
    }

    private fun encryptCategory(): EncryptCategory = getEncryptedCategory(isBot, app)

    private var lastSendMessageId: String? = null

    private fun sendImageMessage(
        uri: Uri,
        notCompress: Boolean = false,
        mimeType: String? = null,
        fromInput: Boolean = false,
    ) {
        createConversation {
            lifecycleScope.launch {
                val code =
                    withContext(Dispatchers.IO) {
                        val messageId = UUID.randomUUID().toString()
                        lastSendMessageId = messageId
                        chatViewModel.sendImageMessage(
                            conversationId,
                            sender,
                            uri,
                            encryptCategory(),
                            notCompress,
                            mimeType,
                            getRelyMessage(),
                            fromInput,
                            messageId = messageId,
                        )
                    }
                when (code) {
                    0 -> {
                        scrollToDown()
                        markRead()
                    }
                    -1 -> toast(R.string.File_error)
                    -2 -> toast(R.string.Format_not_supported)
                }
            }
        }
    }

    private fun sendGiphy(
        image: Image,
        previewUrl: String,
    ) {
        createConversation {
            chatViewModel.sendGiphyMessage(
                conversationId,
                sender.userId,
                image,
                encryptCategory(),
                previewUrl,
            )
            binding.messageRv.postDelayed(
                {
                    scrollToDown()
                },
                1000,
            )
        }
    }

    override fun onCancel() {
        binding.chatControl.cancelExternal()
    }

    private var audioFile: File? = null

    override fun previewAudio(
        messageId: String,
        file: File,
        duration: Long,
        waveForm: ByteArray,
    ) {
        if (duration < 500) {
            file.deleteOnExit()
        } else {
            audioFile = file
            binding.chatControl.previewAudio(conversationId, file, waveForm, duration) {
                sendAudio(messageId, file, duration, waveForm)
            }
        }
    }

    override fun sendAudio(
        messageId: String,
        file: File,
        duration: Long,
        waveForm: ByteArray,
    ) {
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
                    getRelyMessage(),
                )
                scrollToDown()
            }
        }
    }

    private fun sendVideoMessage(
        uri: Uri,
        start: Float,
        end: Float,
    ) {
        createConversation {
            chatViewModel.sendVideoMessage(
                conversationId,
                sender.userId,
                uri,
                start,
                end,
                encryptCategory(),
                replyMessage = getRelyMessage(),
            )
            if (!viewDestroyed()) {
                binding.messageRv.postDelayed(
                    {
                        scrollToDown()
                    },
                    1000,
                )
            }
        }
    }

    private fun sendAttachmentMessage(attachment: Attachment) {
        createConversation {
            chatViewModel.sendAttachmentMessage(
                conversationId,
                sender,
                attachment,
                encryptCategory(),
                getRelyMessage(),
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
                stickerId,
                encryptCategory(),
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
        if (!viewDestroyed()) {
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

    private fun sendTextMessage(
        message: String,
        isSilentMessage: Boolean? = null,
    ) {
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

    private fun sendReplyTextMessage(
        message: String,
        isSilentMessage: Boolean? = null,
    ) {
        if (message.isNotBlank() && binding.chatControl.replyView.messageItem != null) {
            binding.chatControl.chatEt.setText("")
            createConversation {
                chatViewModel.sendReplyTextMessage(
                    conversationId,
                    sender,
                    message,
                    binding.chatControl.replyView.messageItem!!,
                    encryptCategory(),
                    isSilentMessage,
                )
                binding.chatControl.replyView.animateHeight(53.dp, 0)
                binding.chatControl.replyView.messageItem = null
                scrollToDown()
                markRead()
            }
        }
    }

    private var groupName: String? = null

    @SuppressLint("SetTextI18n")
    private fun renderGroup() {
        binding.actionBar.avatarIv.visibility = VISIBLE
        binding.actionBar.avatarIv.setOnClickListener {
            showGroupBottomSheet(false)
        }
        chatViewModel.getConversationInfoById(
            conversationId,
            requireNotNull(Session.getAccountId()),
        ).observe(
            viewLifecycleOwner,
        ) { info ->
            info ?: return@observe
            groupName = info.name
            binding.actionBar.setSubTitle(
                info.name ?: "",
                requireContext().resources.getQuantityString(
                    R.plurals.title_participants,
                    info.count,
                    info.count,
                ),
            )
            binding.actionBar.avatarIv.setGroup(info.iconUrl)
            inGroup = info.isExist
            if (inGroup) {
                binding.chatControl.visibility = VISIBLE
                binding.bottomCantSend.visibility = GONE
                binding.tapJoinView.root.isVisible = callState.isPendingGroupCall(conversationId)
            } else {
                binding.chatControl.visibility = INVISIBLE
                binding.bottomCantSend.visibility = VISIBLE
                binding.tapJoinView.root.isVisible = false
                binding.chatControl.chatEt.hideKeyboard()
            }

            if (!inGroup && callState.conversationId == conversationId && callState.isNotIdle()) {
                callState.handleHangup(requireContext())
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun showGroupBottomSheet(expand: Boolean) {
        hideIfShowBottomSheet()
        val bottomSheetDialogFragment =
            GroupBottomSheetDialogFragment.newInstance(
                conversationId = conversationId,
                expand = expand,
            )
        bottomSheetDialogFragment.showNow(parentFragmentManager, GroupBottomSheetDialogFragment.TAG)
        bottomSheetDialogFragment.callback =
            object : GroupBottomSheetDialogFragment.Callback {
                override fun onDelete() {
                    activity?.finish()
                }
            }
        bottomSheetDialogFragment.sharedMediaCallback = {
            getShareMediaResult.launch(Pair(conversationId, true))
            bottomSheetDialogFragment.dismiss()
        }
    }

    override fun onKeyboardHidden() {
        binding.chatControl.toggleKeyboard(false)
    }

    override fun onKeyboardShown(height: Int) {
        binding.chatControl.toggleKeyboard(true)
    }

    private fun renderUser(user: User) {
        recipient = user
        renderUserInfo(user)
        chatViewModel.findUserById(user.userId).observe(
            viewLifecycleOwner,
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
            showUserBottom(
                parentFragmentManager,
                user,
                conversationId,
                if (user.userId == recipient?.userId) {
                    { getShareMediaResult.launch(Pair(conversationId, true)) }
                } else {
                    null
                },
            )
        }
        binding.bottomUnblock.setOnClickListener {
            recipient?.let { user ->
                chatViewModel.updateRelationship(
                    RelationshipRequest(
                        user.userId,
                        RelationshipAction.UNBLOCK.name,
                        user.fullName,
                    ),
                )
            }
        }
        if (user.isBot()) {
            renderBot(user)
        }
    }

    private fun renderBot(user: User) =
        lifecycleScope.launch {
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

    private fun open(
        url: String,
        app: App?,
        appCard: AppCardData? = null,
    ) {
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
        galleryAlbumFragment.callback =
            object : GalleryCallback {
                override fun onItemClick(
                    pos: Int,
                    item: Item,
                    send: Boolean,
                ) {
                    val uri = item.uri
                    if (item.isVideo) {
                        if (send) {
                            sendVideoMessage(uri, 0f, 1f)
                        } else {
                            showPreview(uri, getString(R.string.Send), true) { uri, start, end -> sendVideoMessage(uri, start, end) }
                        }
                    } else if (item.isGif || item.isWebp) {
                        if (send) {
                            sendImageMessage(uri)
                        } else {
                            showPreview(uri, getString(R.string.Send), false) { uri, _, _ -> sendImageMessage(uri) }
                        }
                    } else {
                        if (send) {
                            sendImageMessage(uri)
                        } else {
                            getEditorResult.launch(Pair(uri, getString(R.string.Send)))
                        }
                    }
                    releaseChatControl(FLING_DOWN)
                }

                override fun onCameraClick() {
                    openCamera()
                }
            }
        galleryAlbumFragment.rvCallback =
            object : DraggableRecyclerView.Callback {
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
            GalleryAlbumFragment.TAG,
        )
    }

    private fun initMenuLayout(isSelfCreatedBot: Boolean = false) {
        val menuFragment = MenuFragment.newInstance(isGroup, isBot, isSelfCreatedBot)
        activity?.replaceFragment(menuFragment, R.id.menu_container, MenuFragment.TAG)
        appList?.let {
            menuFragment.setAppList(it)
        }
        menuFragment.callback =
            object : MenuFragment.Callback {
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
                                .request(
                                    *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        mutableListOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
                                    } else {
                                        mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }.apply {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    }.toTypedArray(),
                                )
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
                                    },
                                )
                        }
                        MenuType.Transfer -> {
                            binding.chatControl.reset()
                            if (Session.getAccount()?.hasPin == true) {
                                recipient?.let {
                                    TransferFragment.newInstance(buildEmptyTransferBiometricItem(it))
                                        .showNow(parentFragmentManager, TransferFragment.TAG)
                                    // FIXME sync
                                    // jobManager.addJobInBackground(SyncOutputJob())
                                }
                            } else {
                                TipActivity.show(requireActivity(), TipType.Create, true)
                            }
                        }
                        MenuType.Contact -> {
                            parentFragmentManager.inTransaction {
                                setCustomAnimations(
                                    R.anim.slide_in_bottom,
                                    R.anim.slide_out_bottom,
                                    R
                                        .anim.slide_in_bottom,
                                    R.anim.slide_out_bottom,
                                )
                                    .add(
                                        R.id.container,
                                        FriendsFragment.newInstance(conversationId).apply {
                                            setOnFriendClick {
                                                sendContactMessage(it.userId)
                                                parentFragmentManager.popBackStackImmediate()
                                            }
                                        },
                                        FriendsFragment.TAG,
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
                                        .setMessage(getString(R.string.call_on_another_call_hint))
                                        .setNegativeButton(getString(android.R.string.ok)) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .show()
                                }
                            } else {
                                if (checkFloatingPermission()) {
                                    checkVoicePermissions {
                                        initAudioSwitch()
                                        voiceCall()
                                    }
                                } else {
                                    requireActivity().showPipPermissionNotification(MusicActivity::class.java, getString(R.string.web_floating_permission))
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
                                    app.toApp(),
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
            StickerAlbumFragment.TAG,
        )
        stickerAlbumFragment.setCallback(
            object : StickerAlbumFragment.Callback {
                override fun onStickerClick(stickerId: String) {
                    if (!viewDestroyed()) {
                        if (binding.stickerContainer.height != binding.inputLayout.keyboardHeight) {
                            binding.inputLayout.openInputArea(binding.chatControl.chatEt)
                        }
                        sendStickerMessage(stickerId)
                    }
                }

                override fun onGiphyClick(
                    image: Image,
                    previewUrl: String,
                ) {
                    if (!viewDestroyed()) {
                        sendGiphy(image, previewUrl)
                    }
                }
            },
        )
        stickerAlbumFragment.rvCallback =
            object : DraggableRecyclerView.Callback {
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

    @SuppressLint("NotifyDataSetChanged")
    private fun scrollToDown() {
        if (viewDestroyed()) return
        if (messageFetcher.isBottom()) {
            messageLayoutManager.scroll()
        } else {
            lifecycleScope.launch {
                val (_, data) = messageFetcher.initMessages(conversationId, null, true)
                messageAdapter.refreshData(data)
                messageLayoutManager.scroll()
            }
        }
    }

    private fun scrollToMessage(
        messageId: String,
        findMessageAction: ((index: Int) -> Unit)? = null,
    ) = lifecycleScope.launch {
        if (viewDestroyed() || !::messageAdapter.isInitialized) return@launch
        // Return position if it exists in the cache, otherwise refresh the data according to ID and return position
        val position = messageAdapter.indexMessage(messageId)
        if (position >= 0) {
            messageLayoutManager.scrollWithOffset(position, messageRvOffset)
            findMessageAction?.invoke(position)
        } else {
            // refresh to message Id
            val (p, data) = messageFetcher.initMessages(conversationId, messageId)
            if (p == -1) {
                toast(R.string.Message_not_found)
                return@launch
            }
            messageAdapter.refreshData(data)
            messageLayoutManager.scrollWithOffset(p, messageRvOffset)
            findMessageAction?.invoke(p)
        }
        delay(100)
        RxBus.publish(BlinkEvent(messageId))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                if (data.hasExtra(IS_VIDEO)) {
                    sendVideoMessage(it, 0f, 1f)
                } else {
                    getEditorResult.launch(Pair(it, getString(R.string.Send)))
                }
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri?.let { imageUri ->
                getEditorResult.launch(Pair(imageUri, getString(R.string.Send)))
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
                                groupName,
                            )
                        } else {
                            requireContext().getString(
                                R.string.send_file_group,
                                attachment.filename,
                                recipient?.fullName,
                            )
                        },
                    )
                    .setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.Send) { dialog, _ ->
                        sendAttachmentMessage(attachment)
                        dialog.dismiss()
                    }.show()
            } else {
                toast(R.string.File_does_not_exist)
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
                },
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
                    getString(R.string.Save_to_Music),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    },
                ),
            )
        } else if (MimeTypes.isVideo(messageItem.mediaMimeType) ||
            messageItem.mediaMimeType?.isImageSupport() == true
        ) {
            items.add(
                BottomSheetItem(
                    getString(R.string.Save_to_Gallery),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    },
                ),
            )
        } else {
            items.add(
                BottomSheetItem(
                    getString(R.string.Save_to_Downloads),
                    {
                        checkWritePermissionAndSave(messageItem)
                        bottomSheet?.dismiss()
                    },
                ),
            )
        }
        // Android O requires installation permissions
        if (!(messageItem.mediaMimeType.equals("application/vnd.android.package-archive", true) && Build.VERSION.SDK_INT > Build.VERSION_CODES.O)) {
            items.add(
                BottomSheetItem(
                    getString(R.string.Open),
                    {
                        requireContext().openMedia(messageItem)
                        bottomSheet?.dismiss()
                    },
                ),
            )
        }
        items.add(
            BottomSheetItem(
                getString(R.string.Cancel),
                {
                    bottomSheet?.dismiss()
                },
            ),
        )
        val view = buildBottomSheetView(requireContext(), items)
        builder.setCustomView(view)
        bottomSheet = builder.create()
        bottomSheet.show()
    }

    private fun checkWritePermissionAndSave(messageItem: MessageItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            lifecycleScope.launch {
                                messageItem.saveToLocal(requireContext())
                            }
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {
                    },
                )
        } else {
            lifecycleScope.launch {
                messageItem.saveToLocal(requireContext())
            }
        }
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

    private val chatControlCallback =
        object : ChatControlView.Callback {
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
                popupWindow.contentView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.view_silence, null, false).apply {
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
                PopupWindowCompat.showAsDropDown(popupWindow, binding.chatControl.anchorView, (-200).dp, (-110).dp, Gravity.END)
            }

            override fun onRecordStart(audio: Boolean) {
                AudioPlayer.pause()
                MusicPlayer.pause()
                OpusAudioRecorder.get(conversationId).startRecording(this@ConversationFragment)
                if (!isNearToSensor) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun isReady(): Boolean {
                return OpusAudioRecorder.state == STATE_RECORDING
            }

            override fun onRecordSend() {
                OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.SEND)
                if (!isNearToSensor) {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun onRecordPreview() {
                OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.PREVIEW)
                if (!isNearToSensor) {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun onRecordCancel() {
                OpusAudioRecorder.get(conversationId).stopRecording(AudioEndStatus.CANCEL)
                if (!isNearToSensor) {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
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
            val users =
                if (isBot) {
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

    private fun checkPeerIfNeeded() =
        lifecycleScope.launch {
            if (!isGroup) return@launch

            delay(1000)
            if (callState.getGroupCallStateOrNull(conversationId) != null) return@launch

            checkPeers(requireContext(), conversationId)
        }

    private var snackBar: Snackbar? = null

    private fun callbackForward(data: Intent?) {
        val selectItems = data?.getParcelableArrayListCompat(ARGS_RESULT, SelectItem::class.java)
        if (selectItems.isNullOrEmpty()) return

        val selectItem = selectItems[0]
        this.selectItem = selectItem
        snackBar =
            Snackbar.make(binding.barLayout, getString(R.string.Forward_success), Snackbar.LENGTH_LONG)
                .setAction(R.string.View) {
                    ConversationActivity.show(requireContext(), selectItem.conversationId, selectItem.userId)
                }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                    (view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }.apply {
                    snackBar?.config(binding.barLayout.context)
                }
        snackBar?.show()
    }

    private fun callbackChatHistory(data: Intent?) {
        data?.getStringExtra(JUMP_ID)?.let { messageId ->
            binding.messageRv.postDelayed({
                scrollToMessage(messageId) {
                    positionBeforeClickQuote = null
                }
            }, 100)
        }
    }

    private fun callbackEditor(data: Intent?) {
        val uri = data?.getParcelableExtraCompat(ImageEditorActivity.ARGS_EDITOR_RESULT, Uri::class.java)
        if (uri != null) {
            val notCompress = data.getBooleanExtra(ImageEditorActivity.ARGS_NOT_COMPRESS, false)
            sendImageMessage(uri, notCompress)
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
        transcriptData?.let { _ ->
            fun check() {
                transcriptDialog?.dismiss()
                val transcriptDialogLayoutBinding = generateTranscriptDialogLayout()
                transcriptDialog =
                    alertDialogBuilder()
                        .setMessage(getString(R.string.chat_import_content, groupName ?: recipient?.fullName ?: ""))
                        .setView(transcriptDialogLayoutBinding.root)
                        .create()
                transcriptDialogLayoutBinding.importChat.setOnClickListener {
                    transcriptDialog?.dismiss()
                }
                transcriptDialogLayoutBinding.sendChat.setOnClickListener {
                    transcriptDialog?.dismiss()
                }
                transcriptDialog?.show()
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            check()
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
            } else {
                check()
            }
        }
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
        val unShareable = messageAdapter.selectSet.find { it.isShareable() == false }
        if (unShareable != null) {
            toast(
                when {
                    unShareable.isLive() -> R.string.live_shareable_false
                    unShareable.isAppCard() -> R.string.app_card_shareable_false
                    else -> R.string.message_shareable_false
                },
            )
            return
        }
        if (messageAdapter.selectSet.size == 1) {
            forward()
        } else {
            val forwardDialogLayoutBinding = generateForwardDialogLayout()
            forwardDialog =
                alertDialogBuilder()
                    .setMessage(getString(R.string.Forward_message))
                    .setView(forwardDialogLayoutBinding.root)
                    .create()
            forwardDialogLayoutBinding.forward.setOnClickListener {
                forward()
                forwardDialog?.dismiss()
            }
            // disable combine transcript
            forwardDialogLayoutBinding.combineForward.isVisible = !messageAdapter.selectSet.any { it.isTranscript() }
            forwardDialogLayoutBinding.combineForward.setOnClickListener {
                combineForward()
                forwardDialog?.dismiss()
            }
            forwardDialog?.show()
        }
    }

    private fun forward() {
        lifecycleScope.launch {
            val list = chatViewModel.getSortMessagesByIds(messageAdapter.selectSet)
            getForwardResult.launch(Pair(list, null))
            closeTool()
        }
    }

    private fun combineForward() {
        lifecycleScope.launch {
            val transcriptId = UUID.randomUUID().toString()
            val messages =
                messageAdapter.selectSet.filter { m -> !m.canNotForward() }
                    .sortedWith(compareBy { it.createdAt })
                    .map { it.toTranscript(transcriptId) }
            val nonExistent =
                withContext(Dispatchers.IO) {
                    messages.filter { m -> m.isAttachment() }
                        .mapNotNull { m -> Uri.parse(m.absolutePath()).path }.any { path ->
                            !File(path).exists()
                        }
                }
            if (nonExistent) {
                toast(R.string.File_does_not_exist)
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
            .request(
                * (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        arrayOf(Manifest.permission.RECORD_AUDIO)
                    }
                ),
            )
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
        if (!requireContext().isAuto() && !initAudioSwitch && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            initAudioSwitch = true
            audioSwitch.start()
        }
    }
}
