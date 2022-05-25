package one.mixin.android.ui.media

import android.os.Bundle
import android.view.View
import android.widget.ViewAnimator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.LayoutRecyclerViewBinding
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.mediaDownloaded

@AndroidEntryPoint
class AudioFragment : BaseFragment(R.layout.layout_recycler_view) {
    companion object {
        const val TAG = "AudioFragment"

        fun newInstance(conversationId: String) = AudioFragment().withArgs {
            putString(Constants.ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val viewModel by viewModels<SharedMediaViewModel>()
    private val binding by viewBinding(LayoutRecyclerViewBinding::bind)

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter = AudioAdapter(
        fun(messageItem: MessageItem) {
            when {
                messageItem.mediaStatus == MediaStatus.CANCELED.name -> {
                    if (Session.getAccountId() == messageItem.userId) {
                        viewModel.retryUpload(messageItem.messageId) {
                            toast(R.string.Retry_upload_failed)
                        }
                    } else {
                        viewModel.retryDownload(messageItem.messageId)
                    }
                }
                messageItem.mediaStatus == MediaStatus.PENDING.name -> {
                    viewModel.cancel(messageItem.messageId, messageItem.conversationId)
                }
                mediaDownloaded(messageItem.mediaStatus) ->
                    if (AudioPlayer.isPlay(messageItem.messageId)) {
                        AudioPlayer.pause()
                    } else {
                        AudioPlayer.play(messageItem, continuePlayOnlyToday = true)
                    }
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.recyclerView.adapter = adapter
        binding.emptyIv.setImageResource(R.drawable.ic_empty_audio)
        binding.emptyTv.setText(R.string.NO_AUDIO)
        viewModel.getAudioMessages(conversationId).observe(
            viewLifecycleOwner
        ) {
            if (it.size <= 0) {
                (view as ViewAnimator).displayedChild = 1
            } else {
                (view as ViewAnimator).displayedChild = 0
            }
            adapter.submitList(it)
        }
    }
}
