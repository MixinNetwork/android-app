package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.page.MixinMemberInvoicesPage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import timber.log.Timber

@AndroidEntryPoint
class MixinMemberInvoicesFragment : BaseFragment() {
    companion object {
        const val TAG = "MixinMemberInvoicesFragment"
        fun newInstance() = MixinMemberInvoicesFragment()
    }

    private val settingViewModel: SettingViewModel by viewModels({ requireActivity() })
    private val memberViewModel: MemberViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            memberViewModel.loadOrders()
            memberViewModel.refreshSubscriptionStatus()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val orders by memberViewModel.orders.collectAsState(initial = emptyList())

                MixinMemberInvoicesPage(
                    membership = Session.getAccount()?.membership ?: Membership(
                        plan = Plan.None, expiredAt = "0-0-0 00:00:00"
                    ),
                    orders = orders,
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onViewPlanClick = {
                        MixinMemberUpgradeBottomSheetDialogFragment.newInstance()
                            .showNow(
                                parentFragmentManager,
                                MixinMemberUpgradeBottomSheetDialogFragment.TAG
                            )
                    },
                    onOrderClick = { order ->
                        navTo(
                            MixinMemberOrderDetailFragment.newInstance(order),
                            MixinMemberOrderDetailFragment.TAG
                        )
                    },
                    onAll = {
                        navTo(
                            AllMixinMemberInvoicesFragment.newInstance(),
                            AllMixinMemberInvoicesFragment.TAG
                        )
                    },
                )
            }
        }
    }

    // Todo remove test code
    private fun launchPurchase100Subscription() {
        lifecycleScope.launch {
            try {
                val orderRequest = MemberOrderRequest(plan = "basic")
                val orderResponse = memberViewModel.createMemberOrder(orderRequest)

                if (orderResponse.isSuccess && orderResponse.data != null) {
                    val order = orderResponse.data
                    memberViewModel.subscribe100(requireActivity(), order!!.orderId)
                } else {
                    Toast.makeText(requireContext(), "创建订单失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "创建订单时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
