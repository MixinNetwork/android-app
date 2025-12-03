package one.mixin.android.ui.home.web3.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.service.RouteService
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.WalletDatabase
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshOrdersJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.route.OrderState
import one.mixin.android.vo.safe.TokenItem
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

    @Inject
    lateinit var routeService: RouteService

    @Inject
    lateinit var walletDatabase: WalletDatabase

    private val swapViewModel by viewModels<SwapViewModel>()

    private val dialog by lazy { indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply { setCancelable(false) } }

    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobManager.addJobInBackground(RefreshOrdersJob())
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
                        onTryAgain = {walletId, type, payAssetId, receiveAssetId ->
                            val inMixin = walletId == null || walletId == Session.getAccountId()
                            val isLimit = type.equals("limit", true)
                            activity?.finish()
                            if (inMixin) {
                                SwapActivity.show(requireContext(), input = payAssetId, output = receiveAssetId, inMixin = true, walletId = null, openLimit = isLimit)
                            } else {
                                SwapActivity.show(requireContext(), input = payAssetId, output = receiveAssetId, inMixin = false, walletId = walletId, openLimit = isLimit)
                            }
                        },
                        pop = {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val orderId: String = arguments?.getString(ARGS_ORDER_ID) ?: return
        startOrderPolling(orderId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopOrderPolling()
    }

    private fun startOrderPolling(orderId: String) {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val local = withContext(Dispatchers.IO) { walletDatabase.orderDao().observeOrder(orderId).first() }
                val isPending = local?.state == OrderState.PENDING.value
                if (!isPending && local != null) break
                withContext(Dispatchers.IO) {
                    val type = local?.orderType
                    val resp = if (type == "swap") {
                        routeService.getSwapOrder(orderId)
                    } else {
                        routeService.getLimitOrder(orderId)
                    }
                    if (resp.isSuccess && resp.data != null) {
                        walletDatabase.orderDao().insertListSuspend(listOf(resp.data!!))
                    }
                }
                delay(3_000)
            }
        }
    }

    private fun stopOrderPolling() {
        refreshJob?.cancel()
        refreshJob = null
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
                    "${Constants.Scheme.MIXIN_TRADE}?type=limit&input=$payId&output=$receiveId"
                } else {
                    "${Constants.Scheme.MIXIN_SWAP}?input=$payId&output=$receiveId"
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
            append("🏷️ ${getString(R.string.Price)}: ${Fiats.getSymbol()}${BigDecimal(token.priceUsd).priceFormat()}\n")
            append("💰 ${getString(R.string.price_change_24h)}: ${runCatching { "${(BigDecimal(token.changeUsd) * BigDecimal(100)).numberFormat2()}%" }.getOrDefault("N/A")}"
            )
        }
        val isLimit = type.equals("limit", true)
        val buildTradeUrl = { inId: String, outId: String ->
            if (isLimit) "${Constants.Scheme.HTTPS_TRADE}?type=limit&input=$inId&output=$outId&referral=${Session.getAccount()?.identityNumber}"
            else "${Constants.Scheme.HTTPS_SWAP}?input=$inId&output=$outId&referral=${Session.getAccount()?.identityNumber}"
        }
        val actions: List<ActionButtonData> = listOf(
            ActionButtonData(
                label = getString(R.string.buy_token, token.symbol),
                color = "#50BD5C",
                action = buildTradeUrl(payId, receiveId)
            ),
            ActionButtonData(
                label = getString(R.string.sell_token, token.symbol),
                color = "#DB454F",
                action = buildTradeUrl(receiveId, payId)
            ),
            ActionButtonData(
                label = "${token.symbol} ${getString(R.string.Market)}",
                color = "#3D75E3",
                action = "${Constants.Scheme.HTTPS_MARKET}/${token.assetId}"
            )
        )
        val appCard = AppCardData(
            appId = Constants.RouteConfig.ROUTE_BOT_USER_ID,
            iconUrl = null,
            coverUrl = null,
            cover = null,
            title = "${if(isLimit) getString(R.string.Trade) else getString(R.string.Swap)} ${token.symbol}",
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = actions,
        )
        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }


}
