package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_FTS_UPGRADE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity

class UpgradeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "UpgradeFragment"

        fun newInstance() = UpgradeFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loadingViewModel: LoadingViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(LoadingViewModel::class.java)
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
            loadingViewModel.upgradeFtsMessage()
            defaultSharedPreferences.putBoolean(PREF_FTS_UPGRADE, true)
            MainActivity.show(requireContext())
            activity?.finish()
        }
    }
}
