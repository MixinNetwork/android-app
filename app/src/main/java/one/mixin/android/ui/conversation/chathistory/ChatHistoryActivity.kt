package one.mixin.android.ui.conversation.chathistory

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.util.MimeTypes
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.ActivityTranscriptBinding
import one.mixin.android.databinding.ViewTranscriptBinding
import one.mixin.android.databinding.ViewUrlBottomBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.job.*
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.location.LocationActivity
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.media.pager.transcript.TranscriptMediaPagerActivity
import one.mixin.android.ui.preview.TextPreviewActivity
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.*
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetItem
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.WebControlView
import one.mixin.android.widget.buildBottomSheetView
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ChatHistoryActivity : BaseActivity() {
    private lateinit var binding: ActivityTranscriptBinding

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private val decoration by lazy {
        MixinHeadersDecoration(adapter)
    }

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var jobManager: MixinJobManager

    private val conversationId by lazy {
        requireNotNull(intent.getStringExtra(CONVERSATION_ID))
    }

    private val transcriptId by lazy {
        requireNotNull(intent.getStringExtra(MESSAGE_ID))
    }

    private val isPlain by lazy {
        intent.getBooleanExtra(IS_PLAIN, true)
    }

    private val isTranscript by lazy {
        intent.getIntExtra(CATEGORY, TRANSCRIPT) == TRANSCRIPT
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscriptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUIManager.transparentDraws(window)
        getScreenshot()?.let {
            binding.container.background = BitmapDrawable(resources, it.blurBitmap(25))
        }
        binding.control.mode = this.isNightMode()
        binding.recyclerView.addItemDecoration(decoration)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        if (isTranscript) {
            conversationRepository.findTranscriptMessageItemById(transcriptId)
                .observe(this) { transcripts ->
                    binding.control.callback = object : WebControlView.Callback {
                        override fun onMoreClick() {
                            showBottomSheet()
                        }

                        override fun onCloseClick() {
                            finish()
                        }
                    }
                    adapter.transcripts = transcripts
                }
        } else {
            conversationRepository.getPinMessages(conversationId)
                .observe(this) { list ->
                    binding.control.callback = object : WebControlView.Callback {
                        override fun onMoreClick() {
                            showBottomSheet()
                        }

                        override fun onCloseClick() {
                            finish()
                        }
                    }
                    adapter.transcripts = list
                }
        }
    }

    override fun onStop() {
        super.onStop()
        AudioPlayer.pause()
    }

    private val adapter by lazy {
        TranscriptAdapter(onItemListener, this)
    }

    private val onItemListener by lazy {
        object : TranscriptAdapter.OnItemListener() {
            override fun onUrlClick(url: String) {
                url.openAsUrlOrWeb(
                    this@ChatHistoryActivity,
                    conversationId,
                    supportFragmentManager,
                    lifecycleScope
                )
            }

            override fun onPostClick(view: View, messageItem: ChatHistoryMessageItem) {
                MarkdownActivity.show(
                    this@ChatHistoryActivity,
                    messageItem.content!!,
                    conversationId
                )
            }

            override fun onUrlLongClick(url: String) {
                val builder = BottomSheet.Builder(this@ChatHistoryActivity)
                val view = View.inflate(
                    ContextThemeWrapper(this@ChatHistoryActivity, R.style.Custom),
                    R.layout.view_url_bottom,
                    null
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
                        lifecycleScope
                    )
                    bottomSheet.dismiss()
                }
                viewBinding.copyTv.setOnClickListener {
                    this@ChatHistoryActivity.getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    this@ChatHistoryActivity.toast(R.string.copy_success)
                    bottomSheet.dismiss()
                }
                bottomSheet.show()
            }

            override fun onMentionClick(identityNumber: String) {
                lifecycleScope.launch {
                    userRepository.findUserByIdentityNumberSuspend(identityNumber.replace("@", ""))
                        ?.let { user ->
                            UserBottomSheetDialogFragment.newInstance(user, conversationId)
                                .showNow(supportFragmentManager, UserBottomSheetDialogFragment.TAG)
                        }
                }
            }

            override fun onQuoteMessageClick(messageId: String, quoteMessageId: String?) {
                quoteMessageId?.let { msgId ->
                    lifecycleScope.launch {
                        val index =
                            conversationRepository.findTranscriptMessageIndex(transcriptId, msgId)
                        scrollTo(index, this@ChatHistoryActivity.screenHeight() * 3 / 4)
                    }
                }
            }

            override fun onImageClick(messageItem: ChatHistoryMessageItem, view: View) {
                TranscriptMediaPagerActivity.show(
                    this@ChatHistoryActivity,
                    view,
                    transcriptId,
                    messageItem.messageId
                )
            }

            override fun onAudioClick(messageItem: ChatHistoryMessageItem) {
                if (
                    AudioPlayer.isPlay(messageItem.messageId)
                ) {
                    AudioPlayer.pause()
                } else {
                    AudioPlayer.play(messageItem.toMessageItem(this@ChatHistoryActivity.conversationId))
                }
            }

            override fun onAudioFileClick(messageItem: ChatHistoryMessageItem) {
            }

            override fun onTranscriptClick(messageItem: ChatHistoryMessageItem) {
                show(this@ChatHistoryActivity, messageItem.messageId, conversationId, isPlain)
            }

            override fun onTextDoubleClick(messageItem: ChatHistoryMessageItem) {
                TextPreviewActivity.show(
                    this@ChatHistoryActivity,
                    messageItem.toMessageItem(conversationId)
                )
            }

            override fun onRetryDownload(transcriptId: String?, messageId: String) {
                lifecycleScope.launch {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.addJobInBackground(
                                    TranscriptAttachmentDownloadJob(
                                        conversationId,
                                        transcript
                                    )
                                )
                            }
                    } else {
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
                                }
                            )
                    }
                }
            }

            override fun onRetryUpload(transcriptId: String?, messageId: String) {
                lifecycleScope.launch {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.addJobInBackground(
                                    SendTranscriptAttachmentMessageJob(
                                        transcript,
                                        isPlain
                                    )
                                )
                            }
                    } else {
                        RxPermissions(this@ChatHistoryActivity)
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .autoDispose(stopScope)
                            .subscribe(
                                { granted ->
                                    if (granted) {
                                        retryUpload(messageId) {
                                            toast(R.string.error_retry_upload)
                                        }
                                    } else {
                                        this@ChatHistoryActivity.openPermissionSetting()
                                    }
                                },
                                {
                                }
                            )
                    }
                }
            }

            override fun onCancel(transcriptId: String?, messageId: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (transcriptId != null) {
                        conversationRepository.getTranscriptById(transcriptId, messageId)
                            ?.let { transcript ->
                                jobManager.cancelJobByMixinJobId("${transcript.transcriptId}${transcript.messageId}")
                                conversationRepository.updateTranscriptMediaStatus(
                                    transcript.transcriptId,
                                    transcript.messageId,
                                    MediaStatus.CANCELED.name
                                )
                            }
                    } else {
                        jobManager.cancelJobByMixinJobId(messageId) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                conversationRepository.updateMediaStatusSuspend(
                                    MediaStatus.CANCELED.name,
                                    messageId
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
                                UserBottomSheetDialogFragment.newInstance(user, conversationId)
                                    .showNow(
                                        supportFragmentManager,
                                        UserBottomSheetDialogFragment.TAG
                                    )
                            }
                        }
                    }
                }
            }

            override fun onFileClick(messageItem: ChatHistoryMessageItem) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O &&
                    messageItem.mediaMimeType.equals(
                            "application/vnd.android.package-archive",
                            true
                        )
                ) {
                    if (this@ChatHistoryActivity.packageManager.canRequestPackageInstalls()) {
                        openMedia(messageItem)
                    } else {
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                    }
                } else if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
                    showBottomSheet(messageItem)
                } else {
                    openMedia(messageItem)
                }
            }

            override fun onUserClick(userId: String?) {
                userId?.let { uid ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            userRepository.getUserById(uid)?.let { user ->
                                withContext(Dispatchers.Main) {
                                    UserBottomSheetDialogFragment.newInstance(user, conversationId)
                                        .showNow(
                                            supportFragmentManager,
                                            UserBottomSheetDialogFragment.TAG
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scrollTo(
        position: Int,
        offset: Int = -1,
        delay: Long = 30,
        action: (() -> Unit)? = null
    ) {
        binding.recyclerView.postDelayed(
            {

                if (position == 0 && offset == 0) {
                    binding.recyclerView.layoutManager?.scrollToPosition(0)
                } else if (offset == -1) {
                    (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        position,
                        0
                    )
                } else {
                    (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        position,
                        offset
                    )
                }
                binding.recyclerView.postDelayed(
                    {
                        (binding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            position,
                            offset
                        )
                        action?.let { it() }
                    },
                    160
                )
            },
            delay
        )
    }

    private fun showBottomSheet(messageItem: ChatHistoryMessageItem) {
        var bottomSheet: BottomSheet? = null
        val builder = BottomSheet.Builder(this)
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
                    openMedia(messageItem)
                    bottomSheet?.dismiss()
                }
            )
        )
        val view = buildBottomSheetView(this, items)
        builder.setCustomView(view)
        bottomSheet = builder.create()
        bottomSheet.show()
    }

    private fun checkWritePermissionAndSave(messageItem: ChatHistoryMessageItem) {
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
                }
            )
    }

    lateinit var getCombineForwardContract: ActivityResultLauncher<ArrayList<TranscriptMessage>>

    @SuppressLint("AutoDispose")
    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            androidx.appcompat.view.ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_transcript,
            null
        )
        val viewBinding = ViewTranscriptBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.forward.setOnClickListener {
            lifecycleScope.launch {
                if (conversationRepository.hasUploadedAttachmentSuspend(transcriptId) > 0) {
                    alert(getString(R.string.error_transcript_forward))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
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
                                }
                        )
                    }
                )
                bottomSheet.dismiss()
            }
        }
        bottomSheet.show()
    }

    private fun retryUpload(id: String, onError: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            conversationRepository.findMessageById(id)?.let {
                if (it.isVideo() && it.mediaSize != null && it.mediaSize == 0L) {
                    try {
                        conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, it.id)
                        jobManager.addJobInBackground(
                            ConvertVideoJob(
                                it.conversationId,
                                it.userId,
                                Uri.parse(it.mediaUrl),
                                it.category.startsWith("PLAIN"),
                                it.id,
                                it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else if (it.isImage() && it.mediaSize != null && it.mediaSize == 0L) { // un-downloaded GIPHY
                    val category =
                        if (it.category.startsWith("PLAIN")) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
                    try {
                        jobManager.addJobInBackground(
                            SendGiphyJob(
                                it.conversationId, it.userId, it.mediaUrl!!, it.mediaWidth!!, it.mediaHeight!!,
                                it.mediaSize, category, it.id, it.thumbImage ?: "", it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else {
                    conversationRepository.updateMediaStatus(MediaStatus.PENDING.name, it.id)
                    jobManager.addJobInBackground(SendAttachmentMessageJob(it))
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
        private const val MESSAGE_ID = "transcript_id"
        private const val CONVERSATION_ID = "conversation_id"
        private const val IS_PLAIN = "is_plain"
        private const val CATEGORY = "category"
        private const val TRANSCRIPT = 0
        private const val CHAT_HISTORY = 1
        fun show(context: Context, messageId: String, conversationId: String, isPlain: Boolean) {
            refreshScreenshot(context)
            context.startActivity(
                Intent(context, ChatHistoryActivity::class.java).apply {
                    putExtra(MESSAGE_ID, messageId)
                    putExtra(CONVERSATION_ID, conversationId)
                    putExtra(IS_PLAIN, isPlain)
                    putExtra(CATEGORY, TRANSCRIPT)
                }
            )
        }

        fun show(context: Context, conversationId: String) {
            refreshScreenshot(context)
            context.startActivity(
                Intent(context, ChatHistoryActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, conversationId)
                    putExtra(CATEGORY, CHAT_HISTORY)
                }
            )
        }
    }
}
