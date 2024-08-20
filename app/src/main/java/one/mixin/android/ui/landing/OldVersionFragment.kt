package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentOldVersionBinding
import one.mixin.android.extension.openMarket
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class OldVersionFragment : BaseFragment(R.layout.fragment_old_version) {
    companion object {
        const val TAG: String = "OldVersionFragment"

        fun newInstance() = OldVersionFragment()
    }

    private val binding by viewBinding(FragmentOldVersionBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            desTv.text =
                getString(R.string.update_mixin_description, requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName)
            updateTv.setOnClickListener {
                requireContext().openMarket()
            }
        }
    }
}
