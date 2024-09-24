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
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.MultiSelectTokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber

@AndroidEntryPoint
class AlertFragment : BaseFragment(), MultiSelectTokenListBottomSheetDialogFragment.DataProvider {
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

    private var tokens by mutableStateOf<List<TokenItem>?>(emptyList())
    private var selectToken by mutableStateOf<TokenItem?>(null)
    private var currentAlert by mutableStateOf<Alert?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            val r =alertViewModel.requestAlerts()
            Timber.e("size: ${r?.data?.size}")
        }
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
                            AlertPage(assets = tokens, openFilter = { openFilter() }, pop = { navigateUp(navController) }, to = { onAddAlert(navController) }, onEdit = { alert ->
                                lifecycleScope.launch {
                                    val token = alertViewModel.simpleAssetItem(alert.assetId)
                                    if (token != null) {
                                        selectToken = token
                                        currentAlert = alert
                                        navController.navigate(AlertDestination.Edit.name)
                                    }
                                }
                            })
                        }

                        composable(AlertDestination.Edit.name) {
                            AlertEditPage(selectToken, currentAlert, pop = { navigateUp(navController) })
                        }
                    }
                }
            }
        }
    }

    private fun onAddAlert(navController: NavHostController) {
        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_RECEIVE).setOnAssetClick { asset ->
            lifecycleScope.launch {
                val count = withContext(Dispatchers.IO){
                    alertViewModel.checkCount(asset.assetId)
                }
                when {
                    count < maxAlertsPerAsset -> {
                        selectToken = asset
                        currentAlert = null
                        navController.navigate(AlertDestination.Edit.name)
                    }

                    count >= maxTotalAlerts -> {
                        toast(getString(R.string.alert_limit_exceeded, maxTotalAlerts))
                    }

                    else -> {
                        toast(getString(R.string.alert_per_asset_limit_exceeded, maxAlertsPerAsset))
                    }
                }
            }

        }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
    }

    private val multiSelectTokenListBottomSheetDialogFragment by lazy {
        MultiSelectTokenListBottomSheetDialogFragment.newInstance().setDateProvider(this@AlertFragment).setOnMultiSelectTokenListener(object : MultiSelectTokenListBottomSheetDialogFragment.OnMultiSelectTokenListener {
            override fun onTokenSelect(tokenItems: List<TokenItem>?) {
                tokens = tokenItems
            }

            override fun onDismiss() {
            }
        })
    }

    override fun getCurrentTokens(): List<TokenItem> {
        return tokens ?: emptyList()
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
