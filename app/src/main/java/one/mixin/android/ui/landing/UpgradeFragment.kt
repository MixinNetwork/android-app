package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_FTS4_UPGRADE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.MessageFts4Helper

class UpgradeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "UpgradeFragment"

        fun newInstance() = UpgradeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_upgrade, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        MixinApplication.get().onlining.set(true)
        lifecycleScope.launch {
            MessageFts4Helper.syncMessageFts4(requireContext())
            defaultSharedPreferences.putBoolean(PREF_FTS4_UPGRADE, true)
            MainActivity.show(requireContext())
            activity?.finish()
        }
    }
}
