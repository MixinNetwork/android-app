package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_shared_media.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseViewModelFragment

class SharedMediaFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "SharedMediaFragment"

        fun newInstance(conversationId: String) = SharedMediaFragment().withArgs {
            putString(ARGS_CONVERSATION_ID, conversationId)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private val adapter: SharedMediaAdapter by lazy {
        SharedMediaAdapter(requireActivity(), conversationId)
    }

    override fun getModelClass() = SharedMediaViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_shared_media, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        view_pager.adapter = adapter
        TabLayoutMediator(
            shared_tl,
            view_pager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                tab.text = getString(
                    when (position) {
                        0 -> R.string.media
                        1 -> R.string.audio
                        2 -> R.string.post
                        3 -> R.string.links
                        else -> R.string.files
                    }
                )
                view_pager.setCurrentItem(tab.position, true)
            }
        ).attach()
        shared_tl.tabMode = TabLayout.MODE_FIXED
        view_pager.currentItem = 0
    }
}
