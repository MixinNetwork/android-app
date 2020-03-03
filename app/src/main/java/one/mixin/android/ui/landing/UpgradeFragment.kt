package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
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

class UpgradeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "UpgradeFragment"

        fun newInstance() = UpgradeFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: MobileViewModel by viewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_upgrade, container, false)

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
