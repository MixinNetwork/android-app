package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_old_version.*
import one.mixin.android.R
import one.mixin.android.extension.openMarket
import one.mixin.android.ui.common.BaseFragment
import javax.inject.Inject

class OldVersionFragment : BaseFragment() {

    companion object {
        const val TAG: String = "OldVersionFragment"
        fun newInstance() = OldVersionFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_old_version, container, false)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        des_tv.text = getString(R.string.update_mixin_description, requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName)
        update_tv.setOnClickListener {
            requireContext().openMarket()
        }
    }
}
