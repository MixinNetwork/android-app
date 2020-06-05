package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ViewAnimator
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.layout_recycler_view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.conversation.markdown.MarkdownActivity
import one.mixin.android.vo.MessageItem

class PostFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "PostFragment"

        fun newInstance(conversationId: String) = PostFragment().withArgs {
            putString(Constants.ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter by lazy {
        PostAdapter(
            requireContext(),
            fun(messageItem: MessageItem) {
                MarkdownActivity.show(requireActivity(), messageItem.content!!, conversationId)
            }
        )
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
        empty_tv.setText(R.string.no_post)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        recycler_view.adapter = adapter
        viewModel.getPostMessages(conversationId).observe(
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
