package one.mixin.android.ui.wallet.alert

import PageScaffold
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.MultiSelectTokenListBottomSheetDialogFragment
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class AlertFragment : BaseFragment(), MultiSelectTokenListBottomSheetDialogFragment.DataProvider {
    companion object {
        const val TAG = "AlertFragment"
        fun newInstance(): AlertFragment {
            return AlertFragment()
        }
    }

    enum class AlertDestination {
        Content,
        Edit,
    }

    private var tokens by mutableStateOf<List<TokenItem>?>(emptyList())

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
                            AlertPage(
                                assets = tokens,
                                openFilter = { openFilter() },
                                pop = { navigateUp(navController) })
                        }

                        composable(AlertDestination.Edit.name) {
                            PageScaffold(
                                title = stringResource(id = R.string.Edit_Alert),
                                verticalScrollable = true,
                                pop = { navigateUp(navController) },
                            ) {
                                // Todo
                            }
                        }
                    }
                }
            }
        }
    }

    private val multiSelectTokenListBottomSheetDialogFragment by lazy {
        MultiSelectTokenListBottomSheetDialogFragment.newInstance()
            .setDateProvider(this@AlertFragment)
            .setOnMultiSelectTokenListener(object : MultiSelectTokenListBottomSheetDialogFragment.OnMultiSelectTokenListener {
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
        multiSelectTokenListBottomSheetDialogFragment
            .showNow(parentFragmentManager, MultiSelectTokenListBottomSheetDialogFragment.TAG)
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
