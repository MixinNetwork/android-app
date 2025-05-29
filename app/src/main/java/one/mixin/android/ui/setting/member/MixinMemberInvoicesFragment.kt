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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import one.mixin.android.billing.BillingManager
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
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingManager = BillingManager.getInstance(requireContext())
        billingManager.initialize()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            memberViewModel.loadOrders()
        }

        lifecycleScope.launch {
            billingManager.subscriptionStatus.collectLatest { status ->
                Timber.d("Subscription status: $status")
                when (status) {
                    BillingManager.SubscriptionStatus.Basic -> {
                        Timber.d("User has Basic subscription")
                    }

                    BillingManager.SubscriptionStatus.Standard -> {
                        Timber.d("User has Standard subscription")
                    }

                    BillingManager.SubscriptionStatus.Premium -> {
                        Timber.d("User has Premium subscription")
                    }

                    BillingManager.SubscriptionStatus.None -> {
                        Timber.d("User has no subscription")
                    }
                }
            }
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
                    // onUpgradeClick = { planType ->
                    //     launchSubscription(planType)
                    // }
                )
            }
        }
    }
}
