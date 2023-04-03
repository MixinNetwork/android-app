package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMigrateRestoreBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.transfer.TransferActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class MigrateRestoreFragment : BaseFragment(R.layout.fragment_migrate_restore) {
    companion object {
        const val TAG = "MigrateRestoreFragment"

        fun newInstance() = MigrateRestoreFragment()
    }

    private val binding by viewBinding(FragmentMigrateRestoreBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            backRl.setOnClickListener {
                navTo(BackUpFragment.newInstance(), BackUpFragment.TAG)
            }
            anotherRl.setOnClickListener { TransferActivity.show(requireContext(), false) }
            computerRl.setOnClickListener { TransferActivity.show(requireContext(), true) }
        }
    }
}
