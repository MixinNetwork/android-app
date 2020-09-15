package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ViewAnimator
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.layout_recycler_view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.MediaStatus

@AndroidEntryPoint
class FileFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "FileFragment"

        fun newInstance(conversationId: String) = FileFragment().withArgs {
            putString(Constants.ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter = FileAdapter { messageItem ->
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
            messageItem.mediaStatus == MediaStatus.EXPIRED.name -> {}
            else -> requireContext().openMedia(messageItem)
        }
    }

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_recycler_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empty_iv.setImageResource(R.drawable.ic_empty_file)
        empty_tv.setText(R.string.no_file)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        recycler_view.adapter = adapter
        viewModel.getFileMessages(conversationId).observe(
            viewLifecycleOwner,
            Observer {
                if (it.size <= 0) {
                    (view as ViewAnimator).displayedChild = 1
                } else {
                    (view as ViewAnimator).displayedChild = 0
                }
                adapter.submitList(it)
            }
        )
    }
}
