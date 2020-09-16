package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_upgrade.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_FTS4_UPGRADE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.MessageFts4Helper

@AndroidEntryPoint
class UpgradeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "UpgradeFragment"

        fun newInstance() = UpgradeFragment()
    }

    private val viewModel by viewModels<MobileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_upgrade, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().onlining.set(true)
        lifecycleScope.launch {
            val done = MessageFts4Helper.syncMessageFts4(preProcess = true) { progress ->
                pb.progress = progress
                progress_tv.text = "$progress%"
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
