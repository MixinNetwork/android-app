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
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment

class LinkFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "LinkFragment"

        fun newInstance(conversationId: String) = LinkFragment().withArgs {
            putString(Constants.ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(Constants.ARGS_CONVERSATION_ID)!!
    }

    private val adapter = LinkAdapter {
        WebBottomSheetDialogFragment.newInstance(it, conversationId)
            .showNow(parentFragmentManager, WebBottomSheetDialogFragment.TAG)
    }

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_recycler_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empty_iv.setImageResource(R.drawable.ic_empty_link)
        empty_tv.setText(R.string.no_link)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        recycler_view.adapter = adapter
        viewModel.getLinkMessages(conversationId).observe(
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
