package one.mixin.android.ui.home.web3.trade

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.perps.AllPositionsFragment
import one.mixin.android.ui.home.web3.trade.perps.PerpetualViewModel
import one.mixin.android.ui.home.web3.trade.perps.navigateToPerpsRoute
import one.mixin.android.ui.wallet.AllOrdersFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker

@AndroidEntryPoint
class TradeMoreBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    companion object {
        const val TAG = "TradeMoreBottomSheetDialogFragment"

        private const val ARGS_WALLET_ID = "wallet_id"
        private const val ARGS_WALLET_NAME = "wallet_name"

        fun newInstance(walletId: String?, walletName: String?) = TradeMoreBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_WALLET_ID, walletId)
                putString(ARGS_WALLET_NAME, walletName)
            }
        }
    }

    private val swapViewModel by viewModels<SwapViewModel>()
    private val perpsViewModel by viewModels<PerpetualViewModel>()

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @Composable
    override fun ComposeContent() {
        val walletId = arguments?.getString(ARGS_WALLET_ID)
        val currentWalletId = walletId ?: Session.getAccountId().orEmpty()
        val swapCount by remember(currentWalletId) {
            swapViewModel.observeOrderCount(currentWalletId, "swap")
        }.collectAsStateWithLifecycle(0)
        val limitCount by remember(currentWalletId) {
            swapViewModel.observeOrderCount(currentWalletId, "limit")
        }.collectAsStateWithLifecycle(0)
        val activityCount by remember { perpsViewModel.observeActivityCount() }.collectAsStateWithLifecycle(0)
        MixinAppTheme {
            TradeMoreBottomSheet(
                walletId = walletId,
                walletName = arguments?.getString(ARGS_WALLET_NAME),
                swapCount = swapCount,
                limitCount = limitCount,
                activityCount = activityCount,
                onOrders = { orderType ->
                    val target = AllOrdersFragment.newInstanceWithWalletIds(
                        walletIds = arrayListOf(currentWalletId),
                        spotType = if (orderType == "limit") {
                            AnalyticsTracker.SpotTradeType.ADVANCED
                        } else {
                            AnalyticsTracker.SpotTradeType.SIMPLE
                        },
                        orderType = orderType,
                    )
                    navTo(target, AllOrdersFragment.TAG)
                    dismiss()
                },
                onActivity = {
                    val manager = requireActivity().supportFragmentManager
                    dismiss()
                    manager.navigateToPerpsRoute(
                        AllPositionsFragment.newClosedInstance(AnalyticsTracker.PerpsSource.PERPS_HOME_MENU),
                        AllPositionsFragment.TAG,
                        R.id.container,
                    )
                },
                onTradePro = {
                    AnalyticsTracker.trackTradeProClick()
                    val activity = requireActivity()
                    dismiss()
                    Constants.HelpLink.TRADE_PRO.openAsUrlOrWeb(
                        activity, null, activity.supportFragmentManager, activity.lifecycleScope,
                    )
                },
                onDismiss = { dismiss() },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int = view.height

    override fun showError(error: String) {
    }
}
