package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOrdersJob
import one.mixin.android.job.RefreshPendingOrdersJob
import one.mixin.android.ui.common.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class OrderDetailFragment : BaseFragment() {
    companion object {
        const val TAG: String = "OrderDetailFragment"
        private const val ARGS_ORDER_ID: String = "args_order_id"
        private const val ARGS_WALLET_ID: String = "args_wallet_id"

        fun newInstance(orderId: String, walletId: String? = null): OrderDetailFragment {
            return OrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_ORDER_ID, orderId)
                    walletId?.let { putString(ARGS_WALLET_ID, it) }
                }
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshOrdersJob())
        jobManager.addJobInBackground(RefreshPendingOrdersJob())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val orderId: String = arguments?.getString(ARGS_ORDER_ID) ?: ""
        val walletId: String? = arguments?.getString(ARGS_WALLET_ID)
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    OrderDetailPage(
                        walletId = walletId,
                        orderId = orderId,
                        onShare = { payAssetId, receiveAssetId ->
                            // Delegate to TradeFragment's share logic if needed in future
                        },
                        onTryAgain = { _, _ ->
                            // no-op in standalone detail; user can go back to Trade
                        },
                        pop = {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    )
                }
            }
        }
    }
}
