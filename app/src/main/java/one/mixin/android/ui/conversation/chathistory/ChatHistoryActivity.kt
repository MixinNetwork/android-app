package one.mixin.android.ui.conversation.chathistory

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityChatHistoryBinding
import one.mixin.android.databinding.ViewTranscriptBinding
import one.mixin.android.databinding.ViewUrlBottomBinding
import one.mixin.android.event.BlinkEvent
import one.mixin.android.extension.alert
import one.mixin.android.extension.callPhone
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.openEmail
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.showIcon
import one.mixin.android.extension.toast
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.job.SendTranscriptAttachmentMessageJob
import one.mixin.android.job.TranscriptAttachmentDownloadJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.message.SendMessageHelper
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.chathistory.holder.BaseViewHolder
import one.mixin.android.ui.conversation.location.LocationActivity
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.ui.media.pager.transcript.TranscriptMediaPagerActivity
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.setting.WallpaperManager
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.copy
import one.mixin.android.vo.generateForwardMessage
import one.mixin.android.vo.isAppButtonGroup
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isText
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.saveToLocal
import one.mixin.android.vo.toMessageItem
import one.mixin.android.vo.toUser
import one.mixin.android.vo.toVideoClip
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.PinAction
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetItem
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.buildBottomSheetView
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@UnstableApi @AndroidEntryPoint
class ChatHistoryActivity : BaseActivity() {
    private lateinit var binding: ActivityChatHistoryBinding

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_NoActionBar

    override fun getDefaultThemeId(): Int = R.style.AppTheme_NoActionBar

    private val decoration by lazy {
        MixinHeadersDecoration(chatHistoryAdapter)
    }

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var messenger: SendMessageHelper

    @Inject
    lateinit var jobManager: MixinJobManager
    private val conversationId by lazy {
        requireNotNull(intent.getStringExtra(CONVERSATION_ID))
    }
    private val transcriptId by lazy {
        requireNotNull(intent.getStringExtra(MESSAGE_ID))
    }
    private val encryptCategory by lazy {
        EncryptCategory.values()[intent.getIntExtra(ENCRYPT_CATEGORY, EncryptCategory.PLAIN.ordinal)]
    }
    private val isGroup by lazy {
        intent.getBooleanExtra(IS_GROUP, false)
    }
    private val count by lazy {
        intent.getIntExtra(COUNT, 0)
    }
    private val isTranscript by lazy {
        intent.getIntExtra(CATEGORY, TRANSCRIPT) == TRANSCRIPT
    }

    private var firstLoad = true

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.container.backgroundImage = WallpaperManager.getWallpaper(this@ChatHistoryActivity)
        binding.titleView.leftIb.setOnClickListener { finish() }
        binding.titleView.setSubTitle(
            getString(
                if (isTranscript) {
                    R.string.Transcript
                } else {
                    R.string.Pinned_Messages
                },
            ),
            "",
        )
        binding.recyclerView.addItemDecoration(decoration)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager =
            object : LinearLayoutManager(this) {
                override fun onLayoutChildren(
                    recycler: RecyclerView.Recycler,
                    state: RecyclerView.State,
                ) {
                    if (!isTranscript && firstLoad && state.itemCount > 0) {
                        firstLoad = false
                        scrollToPositionWithOffset(
                            state.itemCount - 1,
                            0,
                        )
                    }
                    super.onLayoutChildren(recycler, state)
                }
            }
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = chatHistoryAdapter
        if (isTranscript) {
            binding.unpinTv.isVisible = false
            buildLivePagedList(conversationRepository.findTranscriptMessageItemById(transcriptId))
                .observe(this) { transcripts ->
                    binding.titleView.rightIb.setOnClickListener {
                        showBottomSheet()
                    }
                    chatHistoryAdapter.submitList(transcripts)
                }
            binding.titleView.rightAnimator.isVisible = true
            binding.titleView.rightIb.setImageResource(R.drawable.ic_more)
        } else {
            lifecycleScope.launch {
                if (isGroup) {
                    val role =
                        withContext(Dispatchers.IO) {
                            conversationRepository.findParticipantById(
                                conversationId,
                                Session.getAccountId()!!,
                            )?.role
                        }
                    binding.unpinTv.isVisible = role == ParticipantRole.OWNER.name || role == ParticipantRole.ADMIN.name
                } else {
                    binding.unpinTv.isVisible = true
                }
            }
            binding.unpinTv.setOnClickListener {
                alert(getString(R.string.unpin_all_messages_confirmation))
                    .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(R.string.OK) { dialog, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                conversationRepository.getPinMessageMinimals(conversationId)
                                    .chunked(128) { list ->
                                        if (list.isEmpty()) return@chunked
                                        conversationRepository.deletePinMessageByIds(list.map { it.messageId })
                                        Timber.e((list.map { it.messageId }.toString()))
                                        messenger.sendPinMessage(
                                            conversationId,
                                            requireNotNull(Session.getAccount()).toUser(),
                                            PinAction.UNPIN,
                                            list,
                                        )
                                    }
                            }
                            finish()
                        }
                        dialog.dismiss()
                    }.show()
            }
            binding.titleView.rightAnimator.isVisible = false
            binding.titleView.setSubTitle(
                this@ChatHistoryActivity.resources.getQuantityString(R.plurals.pinned_message_title, count, count),
                "",
            )
            buildLivePagedList(conversationRepository.getPinMessages(conversationId, count))
                .observe(this) { list ->
                    chatHistoryAdapter.submitList(list)
                }
        }
    }

    override fun onStop() {
        binding.recyclerView.let { rv ->
            rv.children.forEach {
                val vh = rv.getChildViewHolder(it)
                if (vh != null && vh is BaseViewHolder) {
                    vh.stopListen()
                }
            }
        }
        super.onStop()
        AudioPlayer.pause()
    }

    private fun buildLivePagedList(dataSource: DataSource.Factory<Int, ChatHistoryMessageItem>): LiveData<PagedList<ChatHistoryMessageItem>> {
        val pagedListConfig =
            PagedList.Config.Builder()
                .setPrefetchDistance(10 * 2)
                .setPageSize(10)
                .setEnablePlaceholders(true)
                .build()
        return LivePagedListBuilder(
            dataSource,
            pagedListConfig,
        ).build()
    }

    private val chatHistoryAdapter by lazy {
        ChatHistoryAdapter(onItemListener, this)
    }
    private val onItemListener by lazy {
        object : ChatHistoryAdapter.OnItemListener() {
            override fun onUrlClick(url: String) {
                url.openAsUrlOrWeb(
                    this@ChatHistoryActivity,
                    conversationId,
                    supportFragmentManager,
                    lifecycleScope,
                )
            }

            override fun onPostClick(
                view: View,
                messageItem: ChatHistoryMessageItem,
            ) {
                MarkdownActivity.show(
                    this@ChatHistoryActivity,
                    messageItem.content!!,
                    conversationId,
                )
            }

            override fun onUrlLongClick(url: String) {
                val builder = BottomSheet.Builder(this@ChatHistoryActivity)
                val view =
                    View.inflate(
                        ContextThemeWrapper(this@ChatHistoryActivity, R.style.Custom),
                        R.layout.view_url_bottom,
                        null,
                    )
                val viewBinding = ViewUrlBottomBinding.bind(view)
                builder.setCustomView(view)
                val bottomSheet = builder.create()
                viewBinding.urlTv.text = url
                viewBinding.openTv.setOnClickListener {
                    url.openAsUrlOrWeb(
                        this@ChatHistoryActivity,
                        conversationId,
                        supportFragmentManager,
                        lifecycleScope,
                    )
                    bottomSheet.dismiss()
                }
                viewBinding.copyTv.setOnClickListener {
                    this@ChatHistoryActivity.getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    toast(R.string.copied_to_clipboard)
                    bottomSheet.dismiss()
                }
                bottomSheet.show()
            }

            override fun onMentionClick(identityNumber: String) {
                lifecycleScope.launch {
                    userRepository.findUserByIdentityNumberSuspend(identityNumber.replace("@", ""))
                        ?.let { user ->
                            showUserBottom(supportFragmentManager, user, conversationId)
                        }
                }
            }

            override fun onEmailClick(email: String) {
                this@ChatHistoryActivity.openEmail(email)
            }

            override fun onPhoneClick(phoneNumber: String) {
                this@ChatHistoryActivity.callPhone(phoneNumber)
            }

            override fun onQuoteMessageClick(
                messageId: String,
                quoteMessageId: String?,
            ) {
                quoteMessageId?.let { msgId ->
                    lifecycleScope.launch {
                        val index =
                            if (isTranscript) {
                                conversationRepository.findTranscriptMessageIndex(
                                    transcriptId,
                                    msgId,
                                )
                            } else {
                                conversationRepository.findPinMessageIndex(
                                    conversationId,
                                    msgId,
                                )
                            }
                        scrollTo(index, this@ChatHistoryActivity.screenHeight() * 3 / 4) {
                            RxBus.publish(BlinkEvent(quoteMessageId))
                        }
                    }
                }
            }

            override fun onImageClick(
                messageItem: ChatHistoryMessageItem,
                view: View,
            ) {
                if (isTranscript) {
                    TranscriptMediaPagerActivity.show(
                        this@ChatHistoryActivity,
                        view,
                        transcriptId,
                        messageItem.messageId,
                    )
                } else {
                    MediaPagerActivity.show(
                        this@ChatHistoryActivity,
                        view,
                        messageItem.conversationId!!,
                        messageItem.messageId,
                        messageItem.toMessageItem(),
                        MediaPagerActivity.MediaSource.ChatHistory,
                    )
                }
            }

            override fun onAudioClick(messageItem: ChatHistoryMessageItem) {
                if (
                    AudioPlayer.isPlay(messageItem.messageId)
                ) {
                    AudioPlayer.pause()
                } else {
                    AudioPlayer.play(messageItem.toMessageItem())
                }
            }

            override fun onAudioFileClick(messageItem: ChatHistoryMessageItem) {
            }

            override fun onActionClick(
                action: String,
                userId: String?,
            ) {
                if (openInputAction(action) || userId == null) return

                lifecycleScope.launch {
                    val app = userRepository.findAppById(userId)
                    action.openAsUrlOrWeb(this@ChatHistoryActivity, conversationId, supportFragmentManager, lifecycleScope, app)
                }
            }

            override fun onAppCardClick(
                appCard: AppCardData,
                userId: String?,
            ) {
                if (openInputAction(appCard.action)) return

                appCard.action.openAsUrlOrWeb(this@ChatHistoryActivity, conversationId, supportFragmentManager, lifecycleScope, null, appCard)
            }

            override fun onTranscriptClick(messageItem: ChatHistoryMessageItem) {
                show(this@ChatHistoryActivity, messageItem.messageId, conversationId, encryptCategory)
            }

            override fun onTextDoubleClick(messageItem: ChatHistoryMessageItem) {
                TextPreviewActivity.show(
                    this@ChatHistoryActivity,
                    messageItem.toMessageItem(conversationId),
                )
            }

            override fun onRetryDownload(
                transcriptId: String?,
                messageId: String,
            ) {
                lifecycleScope.launch {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.addJobInBackground(
                                    TranscriptAttachmentDownloadJob(
                                        conversationId,
                                        transcript,
                                    ),
                                )
                            }
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            RxPermissions(this@ChatHistoryActivity)
                                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .autoDispose(stopScope)
                                .subscribe(
                                    { granted ->
                                        if (granted) {
                                            retryDownload(messageId)
                                        } else {
                                            this@ChatHistoryActivity.openPermissionSetting()
                                        }
                                    },
                                    {
                                    },
                                )
                        } else {
                            retryDownload(messageId)
                        }
                    }
                }
            }

            override fun onRetryUpload(
                transcriptId: String?,
                messageId: String,
            ) {
                lifecycleScope.launch {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.addJobInBackground(
                                    SendTranscriptAttachmentMessageJob(
                                        transcript,
                                        encryptCategory,
                                    ),
                                )
                            }
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            RxPermissions(this@ChatHistoryActivity)
                                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .autoDispose(stopScope)
                                .subscribe(
                                    { granted ->
                                        if (granted) {
                                            retryUpload(messageId) {
                                                toast(R.string.Retry_upload_failed)
                                            }
                                        } else {
                                            this@ChatHistoryActivity.openPermissionSetting()
                                        }
                                    },
                                    {
                                    },
                                )
                        } else {
                            retryUpload(messageId) {
                                toast(R.string.Retry_upload_failed)
                            }
                        }
                    }
                }
            }

            override fun onCancel(
                transcriptId: String?,
                messageId: String,
            ) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.cancelJobByMixinJobId("${transcript.transcriptId}${transcript.messageId}")
                                conversationRepository.updateTranscriptMediaStatus(
                                    transcript.transcriptId,
                                    transcript.messageId,
                                    MediaStatus.CANCELED.name,
                                )
                            }
                    } else {
                        jobManager.cancelJobByMixinJobId(messageId) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                conversationRepository.updateMediaStatusSuspend(
                                    MediaStatus.CANCELED.name,
                                    messageId,
                                    conversationId,
                                )
                            }
                        }
                    }
                }
            }

            override fun onLocationClick(messageItem: ChatHistoryMessageItem) {
                val location =
                    GsonHelper.customGson.fromJson(messageItem.content, LocationPayload::class.java)
                LocationActivity.show(this@ChatHistoryActivity, location)
            }

            override fun onContactCardClick(userId: String) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        userRepository.getUserById(userId)?.let { user ->
                            withContext(Dispatchers.Main) {
                                showUserBottom(supportFragmentManager, user, conversationId)
                            }
                        }
                    }
                }
            }

            override fun onFileClick(messageItem: ChatHistoryMessageItem) {
                showBottomSheet(messageItem)
            }

            override fun onUserClick(userId: String?) {
                userId?.let { uid ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            userRepository.getUserById(uid)?.let { user ->
                                withContext(Dispatchers.Main) {
                                    showUserBottom(supportFragmentManager, user, conversationId)
                                }
                            }
                        }
                    }
                }
            }

            override fun onMessageJump(messageId: String) {
                setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        putExtra(JUMP_ID, messageId)
                    },
                )
                finish()
            }

            override fun onMenu(
                view: View,
                messageItem: ChatHistoryMessageItem,
            ) {
                lifecycleScope.launch {
                    val role = withContext(Dispatchers.IO) { conversationRepository.findParticipantById(conversationId, Session.getAccountId()!!) }?.role
                    val isAdmin = role == ParticipantRole.OWNER.name || role == ParticipantRole.ADMIN.name
                    val popMenu = PopupMenu(this@ChatHistoryActivity, view)
                    popMenu.menuInflater.inflate(
                        R.menu.chathistory,
                        popMenu.menu,
                    )
                    popMenu.menu.findItem(R.id.unpin).isVisible = !isTranscript && isAdmin
                    popMenu.menu.findItem(R.id.copy).isVisible = messageItem.isText()
                    popMenu.menu.findItem(R.id.forward).isVisible = !isTranscript && !messageItem.isAppButtonGroup()

                    popMenu.showIcon()
                    popMenu.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.copy -> {
                                try {
                                    this@ChatHistoryActivity.getClipboardManager().setPrimaryClip(
                                        ClipData.newPlainText(null, messageItem.content),
                                    )
                                    toast(R.string.copied_to_clipboard)
                                } catch (_: ArrayIndexOutOfBoundsException) {
                                }
                            }
                            R.id.forward -> {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    conversationRepository.findMessageById(messageItem.messageId)
                                        ?.let { message ->
                                            val forwardMessage =
                                                generateForwardMessage(message) ?: return@launch
                                            withContext(Dispatchers.Main) {
                                                ForwardActivity.show(
                                                    this@ChatHistoryActivity,
                                                    arrayListOf(forwardMessage),
                                                    ForwardAction.App.Resultless(),
                                                )
                                            }
                                        }
                                }
                            }
                            R.id.unpin -> {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val list = listOf(messageItem.messageId)
                                    messenger.sendUnPinMessage(conversationId, Session.getAccount()!!.toUser(), list)
                                    conversationRepository.deletePinMessageByIds(list)
                                    withContext(Dispatchers.Main) {
                                        toast(R.string.Message_unpinned)
                                    }
                                }
                            }
                            else -> {
                            }
                        }
                        true
                    }
                    popMenu.show()
                }
            }
        }
    }

    private fun openInputAction(action: String): Boolean {
        if (action.startsWith("input:") && action.length > 6) {
            val msg = action.substring(6).trim()
            if (msg.isNotEmpty()) {
                messenger.sendTextMessage(lifecycleScope, conversationId, requireNotNull(Session.getAccount()).toUser(), msg, encryptCategory)
            }
            return true
        }
        return false
    }

    private fun scrollTo(
        position: Int,
        offset: Int = -1,
        delay: Long = 30,
        action: (() -> Unit)? = null,
    ) {
        binding.recyclerView.postDelayed(
            {
                if (position == 0 && offset == 0) {
                    binding.recyclerView.layoutManager?.scrollToPosition(0)
                } else if (offset == -1) {
                    (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        position,
                        0,
                    )
                } else {
                    (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        position,
                        offset,
                    )
                }
                binding.recyclerView.postDelayed(
                    {
                        (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            offset,
                        )
                        action?.let { it() }
                    },
                    160,
                )
            },
            delay,
        )
    }

    private fun showBottomSheet(messageItem: ChatHistoryMessageItem) {
        var bottomSheet: BottomSheet? = null
        val builder = BottomSheet.Builder(this)
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
                        this.openMedia(messageItem)
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
        val view = buildBottomSheetView(this, items)
        builder.setCustomView(view)
        bottomSheet = builder.create()
        bottomSheet.show()
    }

    private fun checkWritePermissionAndSave(messageItem: ChatHistoryMessageItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            messageItem.saveToLocal(this)
                        } else {
                            openPermissionSetting()
                        }
                    },
                    {
                    },
                )
        } else {
            messageItem.saveToLocal(this)
        }
    }

    @SuppressLint("AutoDispose")
    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(this)
        val view =
            View.inflate(
                androidx.appcompat.view.ContextThemeWrapper(this, R.style.Custom),
                R.layout.view_transcript,
                null,
            )
        val viewBinding = ViewTranscriptBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.forward.setOnClickListener {
            lifecycleScope.launch {
                if (conversationRepository.hasUploadedAttachmentSuspend(transcriptId) > 0) {
                    alert(getString(R.string.error_transcript_forward))
                        .setPositiveButton(R.string.OK) { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                    bottomSheet.dismiss()
                    return@launch
                }
                val transcriptId = UUID.randomUUID().toString()
                ForwardActivity.combineForward(
                    this@ChatHistoryActivity,
                    arrayListOf<TranscriptMessage>().apply {
                        addAll(
                            conversationRepository.getTranscriptsById(this@ChatHistoryActivity.transcriptId)
                                .map {
                                    it.copy(transcriptId)
                                },
                        )
                    },
                )
                bottomSheet.dismiss()
            }
        }
        bottomSheet.show()
    }

    private fun retryUpload(
        id: String,
        onError: () -> Unit,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(id)?.let { message ->
                if (message.isVideo() && message.mediaSize != null && message.mediaSize == 0L) {
                    try {
                        conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, message.messageId, message.conversationId)
                        val videoClip = toVideoClip(message.content, message.mediaUrl)
                        jobManager.addJobInBackground(
                            ConvertVideoJob(
                                message.conversationId,
                                message.userId,
                                Uri.parse(videoClip.uri),
                                videoClip.startProgress,
                                videoClip.endProgress,
                                when {
                                    message.isSignal() -> EncryptCategory.SIGNAL
                                    message.isEncrypted() -> EncryptCategory.ENCRYPTED
                                    else -> EncryptCategory.PLAIN
                                },
                                message.messageId,
                                message.createdAt,
                            ),
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else if (message.isImage() && message.mediaSize != null && message.mediaSize == 0L) { // un-downloaded GIPHY
                    val category =
                        when {
                            message.isSignal() -> MessageCategory.SIGNAL_IMAGE
                            message.isEncrypted() -> MessageCategory.ENCRYPTED_IMAGE
                            else -> MessageCategory.PLAIN_IMAGE
                        }.name
                    try {
                        jobManager.addJobInBackground(
                            SendGiphyJob(
                                message.conversationId,
                                message.userId,
                                message.mediaUrl!!,
                                message.mediaWidth!!,
                                message.mediaHeight!!,
                                message.mediaSize,
                                category,
                                message.messageId,
                                message.thumbImage ?: "",
                                message.createdAt,
                            ),
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else {
                    conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, message.messageId, message.conversationId)
                    jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                }
            }
        }
    }

    private fun retryDownload(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(id)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
    }

    companion object {
        const val JUMP_ID = "jump_id"
        private const val MESSAGE_ID = "transcript_id"
        private const val CONVERSATION_ID = "conversation_id"
        private const val ENCRYPT_CATEGORY = "encryptCategory"
        private const val IS_GROUP = "is_group"
        private const val CATEGORY = "category"
        private const val COUNT = "count"
        private const val TRANSCRIPT = 0
        private const val CHAT_HISTORY = 1

        fun show(
            context: Context,
            messageId: String,
            conversationId: String,
            encryptCategory: EncryptCategory,
        ) {
            context.startActivity(
                Intent(context, ChatHistoryActivity::class.java).apply {
                    putExtra(MESSAGE_ID, messageId)
                    putExtra(CONVERSATION_ID, conversationId)
                    putExtra(ENCRYPT_CATEGORY, encryptCategory.ordinal)
                    putExtra(CATEGORY, TRANSCRIPT)
                },
            )
        }

        fun getPinIntent(
            context: Context,
            conversationId: String,
            isGroup: Boolean,
            count: Int,
        ): Intent {
            return Intent(context, ChatHistoryActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(CATEGORY, CHAT_HISTORY)
                putExtra(IS_GROUP, isGroup)
                putExtra(COUNT, count)
            }
        }
    }
}
