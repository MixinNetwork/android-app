package one.mixin.android.ui.setting.star

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.page.MixinStarInvoicesPage

@AndroidEntryPoint
class MixinStarInvoicesFragment : BaseFragment() {
    companion object {
        const val TAG = "MixinStarInvoicesFragment"
        fun newInstance() = MixinStarInvoicesFragment()
    }

    private val viewModel: SettingViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinStarInvoicesPage {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}
