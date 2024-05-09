package one.mixin.android.ui.tip.wc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.connections.ConnectionDetailsPage
import one.mixin.android.ui.tip.wc.connections.ConnectionsPage

enum class WCDestination {
    Connections,
    ConnectionDetails,
}

@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class WalletConnectFragment : BaseFragment() {
    companion object {
        const val TAG = "WalletConnectFragment"

        fun newInstance(): WalletConnectFragment = WalletConnectFragment()
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
                        startDestination = WCDestination.Connections.name,
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
                        composable(WCDestination.Connections.name) {
                            ConnectionsPage({
                                navController.navigate("${WCDestination.ConnectionDetails.name}/$it") {
                                    popUpTo(WCDestination.Connections.name)
                                }
                            }) {
                                navigateUp(navController)
                            }
                        }
                        composable("${WCDestination.ConnectionDetails.name}/{connectionId}") { navBackStackEntry ->
                            navBackStackEntry.arguments?.getString("connectionId")?.toIntOrNull().let { connectionId ->
                                ConnectionDetailsPage(connectionId) {
                                    navigateUp(navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}