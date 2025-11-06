package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import one.mixin.android.Constants
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.isNightMode
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOrdersJob
import one.mixin.android.job.RefreshPendingOrdersJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.toast
import java.math.BigDecimal
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

    private val swapViewModel by viewModels<SwapViewModel>()

    private val dialog by lazy { indeterminateProgressDialog(message = one.mixin.android.R.string.Please_wait_a_bit).apply { setCancelable(false) } }

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
                        onShare = { payAssetId: String, receiveAssetId: String, type: String ->
                            viewLifecycleOwner.lifecycleScope.launch { shareOrder(payAssetId, receiveAssetId, type) }
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

    private suspend fun shareOrder(payAssetId: String, receiveAssetId: String, type: String) {
        dialog.show()
        runCatching {
            var payId: String = payAssetId
            val receiveId: String = if (receiveAssetId in Constants.AssetId.usdcAssets || receiveAssetId in Constants.AssetId.usdtAssets) {
                payId = receiveAssetId
                payAssetId
            } else {
                receiveAssetId
            }
            val msg: ForwardMessage = swapViewModel.syncAsset(receiveId)?.let { token: TokenItem ->
                buildForwardMessage(token, payId, receiveId, type)
            } ?: run {
                // Fallback: share plain URL when token not found
                val isLimit = type.equals("limit", true)
                val url = if (isLimit) {
                    "${Constants.Scheme.HTTPS_TRADE}?type=limit&input=$payId&output=$receiveId"
                } else {
                    "${Constants.Scheme.HTTPS_SWAP}?input=$payId&output=$receiveId"
                }
                ForwardMessage(ShareCategory.Text, url)
            }
            ShareMessageBottomSheetDialogFragment.newInstance(msg, null)
                .showNow(parentFragmentManager, ShareMessageBottomSheetDialogFragment.TAG)
        }.onFailure { e: Throwable ->
            ErrorHandler.handleError(e)
        }
        dialog.dismiss()
    }

    private fun buildForwardMessage(token: TokenItem, payId: String, receiveId: String, type: String): ForwardMessage {
        val description: String = buildString {
            append("🔥 ${token.name} (${token.symbol})\n\n")
            append("🏷️ ${getString(one.mixin.android.R.string.Price)}: ${Fiats.getSymbol()}${BigDecimal(token.priceUsd).priceFormat()}\n")
            append("💰 ${getString(one.mixin.android.R.string.price_change_24h)}: ${runCatching { "${(BigDecimal(token.changeUsd) * BigDecimal(100)).numberFormat2()}%" }.getOrDefault("N/A")}"
            )
        }
        val isLimit = type.equals("limit", true)
        val buildTradeUrl = { inId: String, outId: String ->
            if (isLimit) "${Constants.Scheme.HTTPS_TRADE}?type=limit&input=$inId&output=$outId&referral=${one.mixin.android.session.Session.getAccount()?.identityNumber}"
            else "${Constants.Scheme.HTTPS_SWAP}?input=$inId&output=$outId&referral=${one.mixin.android.session.Session.getAccount()?.identityNumber}"
        }
        val actions: List<ActionButtonData> = listOf(
            ActionButtonData(
                label = getString(one.mixin.android.R.string.buy_token, token.symbol),
                color = "#50BD5C",
                action = buildTradeUrl(payId, receiveId)
            ),
            ActionButtonData(
                label = getString(one.mixin.android.R.string.sell_token, token.symbol),
                color = "#DB454F",
                action = buildTradeUrl(receiveId, payId)
            ),
            ActionButtonData(
                label = "${token.symbol} ${getString(one.mixin.android.R.string.Market)}",
                color = "#3D75E3",
                action = "${Constants.Scheme.HTTPS_MARKET}/${token.assetId}"
            )
        )
        val appCard: AppCardData = AppCardData(
            appId = one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID,
            iconUrl = null,
            coverUrl = null,
            cover = null,
            title = "${getString(one.mixin.android.R.string.Swap)} ${token.symbol}",
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = actions,
        )
        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }


}
