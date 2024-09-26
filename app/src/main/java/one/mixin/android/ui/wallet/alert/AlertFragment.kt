package one.mixin.android.ui.wallet.alert

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAlertsJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.MultiSelectCoinListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.MultiSelectTokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class AlertFragment : BaseFragment(),MultiSelectCoinListBottomSheetDialogFragment.DataProvider {
    companion object {
        const val TAG = "AlertFragment"

        const val maxTotalAlerts = 100
        const val maxAlertsPerAsset = 10

        fun newInstance(): AlertFragment {
            return AlertFragment()
        }
    }

    enum class AlertDestination {
        Content, Edit,
    }

    private val alertViewModel by viewModels<AlertViewModel>()

    private var coins by mutableStateOf<List<CoinItem>?>(emptyList())
    private var selectCoin by mutableStateOf<CoinItem?>(null)
    private var currentAlert by mutableStateOf<Alert?>(null)

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(RefreshAlertsJob())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = AlertDestination.Content.name,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300),
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300),
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300),
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300),
                            )
                        },
                    ) {
                        composable(AlertDestination.Content.name) {
                            AlertPage(coins = coins, openFilter = { openFilter() }, pop = { requireActivity().onBackPressedDispatcher.onBackPressed() }, to = { onAddAlert(navController) }, onEdit = { alert ->
                                lifecycleScope.launch {
                                    val coin = alertViewModel.simpleCoinItem(alert.coinId)
                                    if (coin != null) {
                                        selectCoin = coin
                                        currentAlert = alert
                                        navController.navigate(AlertDestination.Edit.name)
                                    }
                                }
                            })
                        }

                        composable(AlertDestination.Edit.name) {
                            AlertEditPage(selectCoin, currentAlert, pop = { navigateUp(navController) })
                        }
                    }
                }
            }
        }
    }

    private fun onAddAlert(navController: NavHostController) {
        lifecycleScope.launch {
            val isTotalAlertCountExceeded = withContext(Dispatchers.IO) {
                alertViewModel.isTotalAlertCountExceeded()
            }
            if (isTotalAlertCountExceeded) {
                toast(getString(R.string.alert_limit_exceeded, maxTotalAlerts))
            } else {
                selectTokenListBottomSheetDialogFragment.setOnMultiSelectCoinListener(object : MultiSelectCoinListBottomSheetDialogFragment.OnMultiSelectCoinListener {
                    override fun onCoinClick(coinItem: CoinItem) {
                        lifecycleScope.launch {
                            val isAssetAlertCountExceeded = withContext(Dispatchers.IO) {
                                alertViewModel.isAssetAlertCountExceeded(coinItem.coinId)
                            }
                            if (isAssetAlertCountExceeded) {
                                toast(getString(R.string.alert_per_asset_limit_exceeded, maxAlertsPerAsset))
                            } else {
                                selectCoin = coinItem
                                currentAlert = null
                                navController.navigate(AlertDestination.Edit.name)
                            }
                        }
                    }

                    override fun onCoinSelect(coinItems: List<CoinItem>?) {
                    }

                    override fun onDismiss() {
                    }
                }).showNow(parentFragmentManager, MultiSelectTokenListBottomSheetDialogFragment.TAG)
            }
        }
    }

    private val selectTokenListBottomSheetDialogFragment by lazy {
        MultiSelectCoinListBottomSheetDialogFragment.newInstance(true).setDateProvider(object :MultiSelectCoinListBottomSheetDialogFragment.DataProvider{
            override fun getCurrentCoins(): List<CoinItem> {
                return emptyList()
            }
        })
    }

    private val multiSelectTokenListBottomSheetDialogFragment by lazy {
        MultiSelectCoinListBottomSheetDialogFragment.newInstance().setDateProvider(this@AlertFragment).setOnMultiSelectCoinListener(object : MultiSelectCoinListBottomSheetDialogFragment.OnMultiSelectCoinListener {
            override fun onCoinClick(coinItem: CoinItem) {
            }

            override fun onCoinSelect(coinItems: List<CoinItem>?) {
                coins = coinItems
            }

            override fun onDismiss() {
            }
        })
    }

    override fun getCurrentCoins(): List<CoinItem> {
        return coins ?: emptyList()
    }

    private fun openFilter() {
        multiSelectTokenListBottomSheetDialogFragment.showNow(parentFragmentManager, MultiSelectTokenListBottomSheetDialogFragment.TAG)
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
