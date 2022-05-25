package one.mixin.android.ui.media

import android.os.Bundle
import android.view.View
import android.widget.ViewAnimator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.LayoutRecyclerViewBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.StickyRecyclerHeadersDecorationForGrid
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageItem

@AndroidEntryPoint
class MediaFragment : BaseFragment(R.layout.layout_recycler_view) {
    companion object {
        const val TAG = "MediaFragment"
        const val PADDING = 1
        const val COLUMN = 4

        const val PAGE_SIZE = 25

        fun newInstance(conversationId: String) = MediaFragment().withArgs {
            putString(ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private val padding: Int by lazy {
        PADDING.dp
    }

    private val adapter = MediaAdapter(
        fun(imageView: View, messageItem: MessageItem) {
            MediaPagerActivity.show(requireActivity(), imageView, conversationId, messageItem.messageId, messageItem, MediaPagerActivity.MediaSource.SharedMedia)
        }
    )

    private val viewModel by viewModels<SharedMediaViewModel>()
    private val binding by viewBinding(LayoutRecyclerViewBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.size = (requireContext().realSize().x - (COLUMN - 1) * padding) / COLUMN
        val lm = GridLayoutManager(requireContext(), COLUMN)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (different2Next(position)) {
                    val offset = getSameIdItemCount(position)
                    val columnIndex = offset.rem(COLUMN)
                    val extraColumn = COLUMN - (columnIndex + 1)
                    1 + extraColumn
                } else {
                    1
                }
            }
        }
        binding.recyclerView.layoutManager = lm
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.isVerticalScrollBarEnabled = false
        binding.recyclerView.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, false))
        binding.recyclerView.addItemDecoration(StickyRecyclerHeadersDecorationForGrid(adapter, COLUMN))
        binding.recyclerView.adapter = adapter
        binding.emptyIv.setImageResource(R.drawable.ic_empty_media)
        binding.emptyTv.setText(R.string.NO_MEDIA)
        viewModel.getMediaMessagesExcludeLive(conversationId).observe(
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

    private fun different2Next(pos: Int): Boolean {
        val headerId = adapter.getHeaderId(pos)
        var nextHeaderId = -1L
        val nextPos = pos + 1
        if (nextPos >= 0 && nextPos < adapter.itemCount) {
            nextHeaderId = adapter.getHeaderId(nextPos)
        }
        return headerId != nextHeaderId
    }

    private fun getSameIdItemCount(pos: Int): Int {
        val currentId = adapter.getHeaderId(pos)
        for (i in 1 until pos) {
            if (adapter.getHeaderId(pos - i) != currentId) {
                return i - 1
            }
        }
        return pos
    }
}
