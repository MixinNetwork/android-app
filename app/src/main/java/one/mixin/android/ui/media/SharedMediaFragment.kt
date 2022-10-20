package one.mixin.android.ui.media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSharedMediaBinding
import one.mixin.android.databinding.ViewSharedMediaBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity
import one.mixin.android.ui.media.SharedMediaActivity.Companion.FROM_CHAT
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class SharedMediaFragment : BaseFragment(R.layout.fragment_shared_media) {
    companion object {
        const val TAG = "SharedMediaFragment"

        fun newInstance(conversationId: String, fromChat: Boolean) = SharedMediaFragment().withArgs {
            putString(ARGS_CONVERSATION_ID, conversationId)
            putBoolean(FROM_CHAT, fromChat)
        }
    }

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private val fromChat: Boolean by lazy {
        requireArguments().getBoolean(FROM_CHAT, false)
    }

    private val adapter: SharedMediaAdapter by lazy {
        SharedMediaAdapter(this, conversationId) { messageId ->
            val builder = BottomSheet.Builder(requireContext())
            val view = View.inflate(
                ContextThemeWrapper(requireContext(), R.style.Custom),
                R.layout.view_shared_media,
                null
            )
            val binding = ViewSharedMediaBinding.bind(view)
            builder.setCustomView(view)
            val bottomSheet = builder.create()
            binding.cancel.setOnClickListener {
                bottomSheet.dismiss()
            }
            binding.showInChat.setOnClickListener {
                if (fromChat) {
                    requireActivity().setResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            putExtra(ChatHistoryActivity.JUMP_ID, messageId)
                        }
                    )
                } else {
                    ConversationActivity.showAndClear(requireActivity(), conversationId, messageId = messageId)
                }
                requireActivity().finish()
                bottomSheet.dismiss()
            }
            bottomSheet.show()
        }
    }

    private val binding by viewBinding(FragmentSharedMediaBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { if (isAdded) { requireActivity().onBackPressedDispatcher.onBackPressed() } }
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
