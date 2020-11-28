package one.mixin.android.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSharedMediaBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class SharedMediaFragment : BaseFragment() {
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

    private var _binding: FragmentSharedMediaBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

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
                    0 -> R.string.media
                    1 -> R.string.audio
                    2 -> R.string.post
                    3 -> R.string.links
                    else -> R.string.files
                }
            )
            binding.viewPager.setCurrentItem(tab.position, true)
        }.attach()
        binding.sharedTl.tabMode = TabLayout.MODE_FIXED
        binding.viewPager.currentItem = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
