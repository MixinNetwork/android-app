package one.mixin.android.ui.setting.star

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.page.MixinStarInvoicesPage
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan

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
        val member = Membership(
            plan = Plan.ADVANCE,
            expiredAt = "2025-12-31T23:59:59Z"
        )
        val invoices = listOf(
            MemberInvoice(
                plan = Plan.ADVANCE,
                transactionId = "123456",
                via = "Credit Card",
                amount = "$99.99",
                description = "Advance Plan",
                time = "2023-10-01 12:00",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.UPGRADE
            ),
            MemberInvoice(
                plan = Plan.PROSPERITY,
                transactionId = "789012",
                via = "PayPal",
                amount = "$49.99",
                description = "Prosperity Plan",
                time = "2023-09-15 15:30",
                status = InvoiceStatus.EXPIRED,
                type = InvoiceType.PURCHASE
            ),
            MemberInvoice(
                plan = Plan.ELITE,
                transactionId = "345678",
                via = "Debit Card",
                amount = "$199.99",
                description = "Elite Plan",
                time = "2023-08-20 10:45",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.UPGRADE
            ),
            MemberInvoice(
                plan = Plan.ADVANCE,
                transactionId = "901234",
                via = "Bank Transfer",
                amount = "$29.99",
                description = "Advance Plan",
                time = "2023-07-05 14:20",
                status = InvoiceStatus.EXPIRED,
                type = InvoiceType.PURCHASE
            ),
            MemberInvoice(
                plan = Plan.PROSPERITY,
                transactionId = "567890",
                via = "Crypto",
                amount = "$149.99",
                description = "Prosperity Plan",
                time = "2023-06-15 09:30",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.UPGRADE
            ),
            MemberInvoice(
                plan = Plan.ELITE,
                transactionId = "112233",
                via = "Credit Card",
                amount = "$79.99",
                description = "Elite Plan",
                time = "2023-05-10 11:00",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.PURCHASE
            ),
            MemberInvoice(
                plan = Plan.ADVANCE,
                transactionId = "445566",
                via = "PayPal",
                amount = "$59.99",
                description = "Advance Plan",
                time = "2023-04-25 16:45",
                status = InvoiceStatus.EXPIRED,
                type = InvoiceType.UPGRADE
            ),
            MemberInvoice(
                plan = Plan.PROSPERITY,
                transactionId = "778899",
                via = "Debit Card",
                amount = "$89.99",
                description = "Prosperity Plan",
                time = "2023-03-30 13:15",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.PURCHASE,
            ),
            MemberInvoice(
                plan = Plan.ELITE,
                transactionId = "998877",
                via = "Bank Transfer",
                amount = "$109.99",
                description = "Elite Plan",
                time = "2023-02-20 08:50",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.UPGRADE
            ),
            MemberInvoice(
                plan = Plan.ADVANCE,
                transactionId = "665544",
                via = "Crypto",
                amount = "$39.99",
                description = "Advance Plan",
                time = "2023-01-15 17:30",
                status = InvoiceStatus.EXPIRED,
                type = InvoiceType.PURCHASE
            ),
            MemberInvoice(
                plan = Plan.PROSPERITY,
                transactionId = "223344",
                via = "Credit Card",
                amount = "$129.99",
                description = "Prosperity Plan",
                time = "2022-12-10 19:00",
                status = InvoiceStatus.COMPLETED,
                type = InvoiceType.UPGRADE
            )
        )

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinStarInvoicesPage(
                    membership = member,
                    invoices = invoices,
                    onPop = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onViewPlanClick = {
                        MixinStarUpgradeBottomSheetDialogFragment.newInstance()
                            .showNow(parentFragmentManager, MixinStarUpgradeBottomSheetDialogFragment.TAG)
                    },
                    onInvoiceClick = { invoice ->
                        navTo(MixinInvoiceDetailFragment.newInstance(invoice), MixinInvoiceDetailFragment.TAG)
                    },
                    onAll = {
                        navTo(AllMixinStarInvoicesFragment.newInstance(invoices), AllMixinStarInvoicesFragment.TAG)
                    }
                )
            }
        }
    }
}
