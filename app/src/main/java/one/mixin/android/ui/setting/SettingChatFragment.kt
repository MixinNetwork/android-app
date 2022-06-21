package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSettingChatBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SettingChatFragment : BaseFragment(R.layout.fragment_setting_chat) {
    companion object {
        const val TAG = "SettingChatFragment"
        fun newInstance() = SettingChatFragment()
    }

    private val binding by viewBinding(FragmentSettingChatBinding::bind)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireContext().booleanFromAttribute(R.attr.flag_night)) {
            binding.container.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_night)
        } else {
            binding.container.backgroundImage =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat)
        }
    }
}
