package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.mediaDownloaded

@AndroidEntryPoint
class AudioFragment : BaseFragment() {
    companion object {
        const val TAG = "AudioFragment"

        fun newInstance(conversationId: String) = AudioFragment().withArgs {
            putString(Constants.ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val viewModel by viewModels<SharedMediaViewModel>()

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter = AudioAdapter(
        fun(messageItem: MessageItem) {
            when {
                messageItem.mediaStatus == MediaStatus.CANCELED.name -> {
                    if (Session.getAccountId() == messageItem.userId) {
                        viewModel.retryUpload(messageItem.messageId) {
                            toast(R.string.error_retry_upload)
                        }
                    } else {
                        viewModel.retryDownload(messageItem.messageId)
                    }
                }
                messageItem.mediaStatus == MediaStatus.PENDING.name -> {
                    viewModel.cancel(messageItem.messageId)
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

    private var _binding: LayoutRecyclerViewBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.recyclerView.adapter = adapter
        binding.emptyIv.setImageResource(R.drawable.ic_empty_audio)
        binding.emptyTv.setText(R.string.no_audio)
        viewModel.getAudioMessages(conversationId).observe(
            viewLifecycleOwner,
            {
                if (it.size <= 0) {
                    (view as ViewAnimator).displayedChild = 1
                } else {
                    (view as ViewAnimator).displayedChild = 0
                }
                adapter.submitList(it)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
