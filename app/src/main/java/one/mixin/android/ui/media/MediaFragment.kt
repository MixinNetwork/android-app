package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ViewAnimator
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.layout_recycler_view.*
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.recyclerview.StickyRecyclerHeadersDecorationForGrid
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.media.pager.MediaPagerActivity
import org.jetbrains.anko.dip

@AndroidEntryPoint
class MediaFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "MediaFragment"
        const val PADDING = 1
        const val COLUMN = 4

        fun newInstance(conversationId: String) = MediaFragment().withArgs {
            putString(ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    private val adapter = MediaAdapter(
        fun(imageView: View, messageId: String) {
            MediaPagerActivity.show(requireActivity(), imageView, conversationId, messageId, true)
        }
    )

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_recycler_view, container, false)

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
        recycler_view.layoutManager = lm
        recycler_view.itemAnimator = null
        recycler_view.isVerticalScrollBarEnabled = false
        recycler_view.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, false))
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecorationForGrid(adapter, COLUMN))
        recycler_view.adapter = adapter
        empty_iv.setImageResource(R.drawable.ic_empty_media)
        empty_tv.setText(R.string.no_media)
        viewModel.getMediaMessagesExcludeLive(conversationId).observe(
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
