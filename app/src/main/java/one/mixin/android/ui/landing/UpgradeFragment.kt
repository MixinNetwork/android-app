package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUpgradeBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.viewmodel.LandingViewModel
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class UpgradeFragment : BaseFragment(R.layout.fragment_upgrade) {
    companion object {
        const val TAG: String = "UpgradeFragment"

        const val ARGS_TYPE = "args_type"
        const val TYPE_DB = 0

        fun newInstance(type: Int) =
            UpgradeFragment().withArgs {
                putInt(ARGS_TYPE, type)
            }
    }

    private val viewModel by viewModels<LandingViewModel>()
    private val binding by viewBinding(FragmentUpgradeBinding::bind)

    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        MixinApplication.get().isOnline.set(true)

        lifecycleScope.launch {
            binding.pb.isIndeterminate = true
            withContext(Dispatchers.IO) {
                PropertyHelper.checkMigrated()
                viewModel.lockAndUpgradeDatabase()
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
