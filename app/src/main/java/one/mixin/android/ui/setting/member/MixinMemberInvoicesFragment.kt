package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.RxBus
import one.mixin.android.event.MembershipEvent
import one.mixin.android.extension.navTo
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.profile.InputReferralBottomSheetDialogFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.page.MixinMemberInvoicesPage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import javax.inject.Inject

@AndroidEntryPoint
class MixinMemberInvoicesFragment : BaseFragment() {
    companion object {
        const val TAG = "MixinMemberInvoicesFragment"
        fun newInstance() = MixinMemberInvoicesFragment()
    }

    private val settingViewModel: SettingViewModel by viewModels({ requireActivity() })
    private val memberViewModel: MemberViewModel by viewModels()

    @Inject
    lateinit var jobManager: MixinJobManager

    private var currentMembership: Membership by mutableStateOf(
        Session.getAccount()?.membership ?: Membership(
            plan = Plan.None, expiredAt = "0000-00-00T00:00:00.0000Z"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshAccountJob())
        RxBus.listen(MembershipEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope(Lifecycle.Event.ON_DESTROY))
            .subscribe { _ ->
                currentMembership = Session.getAccount()?.membership ?: Membership(
                    plan = Plan.None, expiredAt = "0000-00-00T00:00:00.0000Z"
                )
            }
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
                MixinMemberInvoicesPage(
                    membership = currentMembership,
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
                    onReferral = {
                        // todo replace
                        InputReferralBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, InputReferralBottomSheetDialogFragment.TAG)
                    }
                )
            }
        }
    }
}
