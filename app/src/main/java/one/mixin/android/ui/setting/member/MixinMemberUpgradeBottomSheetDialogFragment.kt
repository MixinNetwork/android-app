package one.mixin.android.ui.setting.member

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.MembershipEvent
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.screenHeight
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.parser.NewSchemeParser
import one.mixin.android.ui.setting.ui.page.MixinMemberUpgradePage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.Plan
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MixinMemberUpgradeBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MixinMemberUpgradeBottomSheetDialogFragment"
        private const val ARG_DEFAULT_PLAN = "arg_default_plan"

        fun newInstance(defaultPlan: Plan? = null): MixinMemberUpgradeBottomSheetDialogFragment {
            return MixinMemberUpgradeBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    if (defaultPlan != null) {
                        putString(ARG_DEFAULT_PLAN, defaultPlan.name)
                    }
                }
            }
        }
    }

    private var defaultPlan: Plan? = null

    private val newSchemeParser: NewSchemeParser by lazy { NewSchemeParser(this, linkViewModel) }

    @Inject
    lateinit var jobManager: MixinJobManager

    val linkViewModel by viewModels<BottomSheetViewModel>()
    private val memberViewModel by viewModels<MemberViewModel>()

    private var currentUserPlan: Plan by mutableStateOf(Session.getAccount()?.membership?.plan ?: Plan.None)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshAccountJob())
        arguments?.getString(ARG_DEFAULT_PLAN)?.let {
            try {
                defaultPlan = Plan.valueOf(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse default plan")
            }
        }
        RxBus.listen(MembershipEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope(Lifecycle.Event.ON_DESTROY))
            .subscribe { _ ->
                currentUserPlan = Session.getAccount()!!.membership?.plan ?: Plan.None
            }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        LaunchedEffect(Unit) {
            memberViewModel.refreshSubscriptionStatus()
        }
        MixinMemberUpgradePage(
            currentUserPlan = currentUserPlan,
            selectedPlanOverride = defaultPlan ?: currentUserPlan,
            onClose = { dismiss() },
            onUrlGenerated = { url ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Timber.e("MixinMemberUpgradeBottomSheetDialogFragment url: $url")
                    WebActivity.show(requireContext(), url, null)
                }
            },
            onGooglePlay = { orderId, playStoreSubscriptionId ->
                launchPurchaseSubscription(orderId, playStoreSubscriptionId)
            },
            onContactTeamMixin = {
                requireContext().openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            },
            onViewInvoice = { order ->
                dismiss()
                navTo(
                    MixinMemberOrderDetailFragment.newInstance(order),
                    MixinMemberOrderDetailFragment.TAG
                )
            }
        )
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop() 
    }

    override fun showError(errorRes: Int) {
        super.showError(errorRes)
    }

    override fun showError(error: String) {
    }

    override fun syncUtxo() {
        jobManager.addJobInBackground(SyncOutputJob())
    }

    private fun launchPurchaseSubscription(orderId: String, playStoreSubscriptionId: String) {
        lifecycleScope.launch {
            memberViewModel.subscribeWithPlanId(requireActivity(), orderId, playStoreSubscriptionId)
        }
    }
}
