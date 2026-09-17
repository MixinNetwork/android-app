package one.mixin.android.ui.media

import android.os.Bundle
import android.view.View
import android.widget.ViewAnimator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.LayoutRecyclerViewBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageItem

@AndroidEntryPoint
class PostFragment : BaseFragment(R.layout.layout_recycler_view) {
    companion object {
        const val TAG = "PostFragment"

        fun newInstance(conversationId: String) =
            PostFragment().withArgs {
                putString(Constants.ARGS_CONVERSATION_ID, conversationId)
            }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter by lazy {
        PostAdapter(
            requireActivity(),
            fun(messageItem: MessageItem) {
                MarkdownActivity.show(requireActivity(), messageItem.content!!, conversationId)
            },
            fun(messageId: String) {
                onLongClickListener?.invoke(messageId)
            },
        )
    }

    private val viewModel by viewModels<SharedMediaViewModel>()
    private val binding by viewBinding(LayoutRecyclerViewBinding::bind)
    var onLongClickListener: ((String) -> Unit)? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.emptyIv.setImageResource(R.drawable.ic_empty_file)
        binding.emptyTv.setText(R.string.NO_POSTS)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.recyclerView.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.refresh is LoadState.NotLoading) {
                    (view as ViewAnimator).displayedChild = if (adapter.itemCount <= 0) 1 else 0
                }
            }
        }
        viewModel.getPostMessages(conversationId).observe(
            viewLifecycleOwner,
        ) {
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }
    }
}
