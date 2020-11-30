package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_FTS4_UPGRADE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUpgradeBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.MessageFts4Helper
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class UpgradeFragment : BaseFragment(R.layout.fragment_upgrade) {

    companion object {
        const val TAG: String = "UpgradeFragment"

        fun newInstance() = UpgradeFragment()
    }

    private val viewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentUpgradeBinding::bind)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().onlining.set(true)
        lifecycleScope.launch {
            val done = MessageFts4Helper.syncMessageFts4(preProcess = true) { progress ->
                binding.pb.progress = progress
                binding.progressTv.text = "$progress%"
            }
            if (!done) {
                viewModel.startSyncFts4Job()
            }
            defaultSharedPreferences.putBoolean(PREF_FTS4_UPGRADE, true)
            MainActivity.show(requireContext())
            activity?.finish()
        }
    }
}
