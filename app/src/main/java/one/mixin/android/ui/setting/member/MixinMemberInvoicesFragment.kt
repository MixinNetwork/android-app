package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.page.MixinMemberInvoicesPage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan

@AndroidEntryPoint
class MixinMemberInvoicesFragment : BaseFragment() {
    companion object {
        const val TAG = "MixinMemberInvoicesFragment"
        fun newInstance() = MixinMemberInvoicesFragment()
    }

    private val settingViewModel: SettingViewModel by viewModels({ requireActivity() })
    private val memberViewModel: MemberViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val member = Membership(
            plan = Plan.ADVANCE,
            expiredAt = "2025-12-31T23:59:59Z"
        )

        lifecycleScope.launch {
            memberViewModel.loadOrders()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val orders by memberViewModel.orders.collectAsState(initial = emptyList())

                MixinMemberInvoicesPage(
                    membership = member,
                    orders = orders,
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onViewPlanClick = {
                        MixinMemberUpgradeBottomSheetDialogFragment.newInstance()
                            .showNow(parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG)
                    },
                    onOrderClick = { order ->
                        navTo(MixinMemberOrderDetailFragment.newInstance(order), MixinMemberOrderDetailFragment.TAG)
                    },
                    onAll = {
                        navTo(AllMixinMemberInvoicesFragment.newInstance(), AllMixinMemberInvoicesFragment.TAG)
                    }
                )
            }
        }
    }
}
