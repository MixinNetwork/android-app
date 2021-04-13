package one.mixin.android.ui.conversation.transcript

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityTranscriptBinding
import one.mixin.android.databinding.ViewUrlBottomBinding
import one.mixin.android.event.BlinkEvent
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.toast
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.media.pager.TranscriptMediaPagerActivity
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Transcript
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.toMessageItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.MixinHeadersDecoration
import one.mixin.android.widget.WebControlView
import javax.inject.Inject

@AndroidEntryPoint
class TranscriptActivity : BaseActivity() {
    private lateinit var binding: ActivityTranscriptBinding

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_BLUR

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private val decoration by lazy {
        MixinHeadersDecoration(adapter)
    }

    @Inject
    lateinit var userRepository: UserRepository

    private val conversationId by lazy {
        intent.getStringExtra(CONVERSATION_ID)
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
        binding.control.callback = object : WebControlView.Callback {
            override fun onMoreClick() {
            }

            override fun onCloseClick() {
                finish()
            }
        }
        binding.recyclerView.addItemDecoration(decoration)
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
    }

    private val adapter by lazy {
        TranscriptAdapter(transcripts, onItemListener) { view, messageItem ->
            if (messageItem.isVideo() || messageItem.isLive() || messageItem.isImage()) {
                val index = attachmentTranscripts.indexOf(messageItem)
                TranscriptMediaPagerActivity.show(this, view, attachmentTranscripts, index)
            } else if (messageItem.isAudio()) {
                if (AudioPlayer.isPlay(messageItem.messageId)) {
                    AudioPlayer.pause()
                } else {
                    AudioPlayer.play(messageItem)
                }
            }
        }
    }

    private val onItemListener by lazy {
        object : ConversationAdapter.OnItemListener() {
            override fun onUrlClick(url: String) {
                url.openAsUrlOrWeb(this@TranscriptActivity, conversationId, supportFragmentManager, lifecycleScope)
            }

            override fun onUrlLongClick(url: String) {
                val builder = BottomSheet.Builder(this@TranscriptActivity)
                val view = View.inflate(
                    ContextThemeWrapper(this@TranscriptActivity, R.style.Custom),
                    R.layout.view_url_bottom,
                    null
                )
                val viewBinding = ViewUrlBottomBinding.bind(view)
                builder.setCustomView(view)
                val bottomSheet = builder.create()
                viewBinding.urlTv.text = url
                viewBinding.openTv.setOnClickListener {
                    url.openAsUrlOrWeb(
                        this@TranscriptActivity,
                        conversationId,
                        supportFragmentManager,
                        lifecycleScope
                    )
                    bottomSheet.dismiss()
                }
                viewBinding.copyTv.setOnClickListener {
                    this@TranscriptActivity.getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, url))
                    this@TranscriptActivity.toast(R.string.copy_success)
                    bottomSheet.dismiss()
                }
                bottomSheet.show()
            }

            override fun onMentionClick(identityNumber: String) {
                lifecycleScope.launch {
                    userRepository.findUserByIdentityNumberSuspend(identityNumber)?.let { user ->
                        UserBottomSheetDialogFragment.newInstance(user, conversationId)
                            .showNow(supportFragmentManager, UserBottomSheetDialogFragment.TAG)
                    }
                }
            }

            override fun onQuoteMessageClick(messageId: String, quoteMessageId: String?) {
                quoteMessageId?.let { msgId ->
                    val index = transcripts.indexOfFirst { it.messageId == msgId }
                    if (index >= 0) {
                        scrollTo(index) {
                            RxBus.publish(BlinkEvent(messageId))
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

    private val attachmentTranscripts by lazy {
        ArrayList(transcripts.filter { it.isLive() || it.isImage() || it.isVideo() })
    }

    private val transcripts by lazy {
        val content = intent.getStringExtra(CONTENT)
        return@lazy GsonHelper.customGson.fromJson(content, Array<Transcript>::class.java).map {
            it.toMessageItem()
        }
    }

    companion object {
        private const val CONTENT = "content"
        private const val CONVERSATION_ID = "conversation_id"
        fun show(context: Context, content: String, conversationId: String? = null) {
            refreshScreenshot(context)
            context.startActivity(
                Intent(context, TranscriptActivity::class.java).apply {
                    putExtra(CONTENT, content)
                    putExtra(CONVERSATION_ID, conversationId)
                }
            )
        }
    }
}
