package one.mixin.android.ui.tip.wc

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.toUri
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.connections.ConnectionDetailsPage
import one.mixin.android.ui.tip.wc.connections.ConnectionsPage
import timber.log.Timber

enum class WCDestination {
    Connections,
    ConnectionDetails,
}

open class WCNavController {
    open fun navTo(destination: WCDestination, args: Bundle? = null) {
        Timber.d("WCNavController navTo: ${destination.name}")
    }

    open fun navTo(route: String, args: Bundle? = null) {
        Timber.d("WCNavController navTo: $route")
    }

    open fun pop() {
        Timber.d("WCNavController pop")
    }
}

private class WCNavControllerImpl(
    private val navController: NavHostController,
    private val activityNavUp: () -> Unit,
) : WCNavController() {
    override fun navTo(destination: WCDestination, args: Bundle?) {
        navTo(NavDestination.createRoute(destination.name), args)
    }

    override fun navTo(route: String, args: Bundle?) {
        val routeLink = NavDeepLinkRequest.Builder
            .fromUri(route.toUri())
            .build()
        val deepLinkMatch = navController.graph.matchDeepLink(routeLink)
        if (deepLinkMatch == null) {
            Timber.w("navigateTo: no match for $route")
            return
        }
        val id = deepLinkMatch.destination.id
        navController.navigate(id, args)
    }

    override fun pop() {
        if (!navController.popBackStack()) {
            activityNavUp()
        }
    }
}

val LocalWCNav = compositionLocalOf { WCNavController() }

@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class WalletConnectFragment : BaseFragment() {
    companion object {
        const val TAG = "WalletConnectFragment"

        fun newInstance(): WalletConnectFragment = WalletConnectFragment()
    }

    private val parentBackStackEntryCount = MutableLiveData(1)

    private val onParentBackStackChanged = FragmentManager.OnBackStackChangedListener {
        parentBackStackEntryCount.value = parentFragmentManager.backStackEntryCount
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentFragmentManager.addOnBackStackChangedListener(onParentBackStackChanged)
    }

    override fun onDetach() {
        super.onDetach()
        parentFragmentManager.removeOnBackStackChangedListener(onParentBackStackChanged)
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
                    val navController = rememberAnimatedNavController()
                    val navigationController = remember {
                        WCNavControllerImpl(navController) {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    }

                    DisposableEffect(navController) {
                        val observer = Observer<Int> { value ->
                            navController.enableOnBackPressed(value == 0)
                        }
                        parentBackStackEntryCount.observeForever(observer)
                        onDispose {
                            parentBackStackEntryCount.removeObserver(observer)
                        }
                    }

                    CompositionLocalProvider(
                        LocalWCNav provides navigationController,
                    ) {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = WCDestination.Connections.name,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Left,
                                    animationSpec = tween(300),
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Right,
                                    animationSpec = tween(300),
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Left,
                                    animationSpec = tween(300),
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Right,
                                    animationSpec = tween(300),
                                )
                            },
                        ) {
                            composable(WCDestination.Connections.name) {
                                ConnectionsPage()
                            }
                            composable(WCDestination.ConnectionDetails.name) {
                                val connectionId = it.arguments?.getInt("connectionId")
                                ConnectionDetailsPage(connectionId)
                            }
                        }
                    }
                }
            }
        }
    }
}
