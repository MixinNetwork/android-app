package one.mixin.android.ui.media

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSharedMediaBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SharedMediaFragment : BaseFragment(R.layout.fragment_shared_media) {
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
        SharedMediaAdapter(this, conversationId)
    }

    private val binding by viewBinding(FragmentSharedMediaBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(
            binding.sharedTl,
            binding.viewPager
        ) { tab, position ->
            tab.text = getString(
                when (position) {
                    0 -> R.string.Media
                    1 -> R.string.Audio
                    2 -> R.string.Post
                    3 -> R.string.Links
                    else -> R.string.Files
                }
            )
            binding.viewPager.setCurrentItem(tab.position, true)
        }.attach()
        binding.sharedTl.tabMode = TabLayout.MODE_FIXED
        binding.viewPager.currentItem = 0
    }
}
