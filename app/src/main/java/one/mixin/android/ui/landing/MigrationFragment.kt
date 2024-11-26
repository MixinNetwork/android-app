package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUpgradeBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.database.migrationDbFile
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class MigrationFragment : BaseFragment(R.layout.fragment_upgrade) {
    companion object {
        const val TAG: String = "MigrationFragment"


        fun newInstance() = MigrationFragment()
    }

    private val binding by viewBinding(FragmentUpgradeBinding::bind)


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycleScope.launch {
            binding.pb.isIndeterminate = true
            withContext(Dispatchers.IO) {
                migrationDbFile(requireContext())
            }
            MainActivity.show(requireContext())
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
