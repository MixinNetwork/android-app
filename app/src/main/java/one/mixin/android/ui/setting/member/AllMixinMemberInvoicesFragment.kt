package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ui.page.AllInvoicesPage
import one.mixin.android.ui.viewmodel.MemberViewModel

@AndroidEntryPoint
class AllMixinMemberInvoicesFragment : BaseFragment() {

    companion object {
        const val TAG = "AllMixinMemberInvoicesFragment"

        fun newInstance(): AllMixinMemberInvoicesFragment {
            return AllMixinMemberInvoicesFragment()
        }
    }

    private val memberViewModel: MemberViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lifecycleScope.launch {
            memberViewModel.loadOrders()
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AllInvoicesPage(
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onOrderClick = { order ->
                        navTo(MixinMemberOrderDetailFragment.newInstance(order), MixinMemberOrderDetailFragment.TAG)
                    }
                )
            }
        }
    }
}
