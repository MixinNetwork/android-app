package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.realSize
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.recyclerview.StickyRecyclerHeadersDecorationForGrid
import org.jetbrains.anko.dip

class MediaFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "MediaFragment"
        const val PADDING = 10
        const val COLUMN = 3

        fun newInstance(conversationId: String) = MediaFragment().withArgs {
            putString(ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        arguments!!.getString(ARGS_CONVERSATION_ID)!!
    }

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    private val adapter = MediaAdapter()

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_recycler_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as RecyclerView
        adapter.size = (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN
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
        view.layoutManager = lm
        view.addItemDecoration(StickyRecyclerHeadersDecorationForGrid(adapter, COLUMN))
        view.adapter = adapter
        viewModel.getMediaMessagesExcludeLive(conversationId).observe(this, Observer {
            adapter.submitList(it)
        })
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