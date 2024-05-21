package one.mixin.android.ui.home.web3.swap

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
import kotlinx.coroutines.launch
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.safeNavigateUp
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.web3.details.Web3TransactionDetailsFragment.Companion.ARGS_TOKEN
import one.mixin.android.web3.receive.Web3TokenListBottomSheetDialogFragment
import one.mixin.android.web3.swap.SwapTokenListBottomSheetDialogFragment

@AndroidEntryPoint
class SwapFragment : BaseFragment() {
    companion object {
        const val TAG = "SwapFragment"

        fun newInstance(token: Web3Token): SwapFragment = SwapFragment().withArgs {
            putParcelable(ARGS_TOKEN, token)
        }
    }

    enum class SwapDestination {
        Swap,
    }

    private val token: Web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java)!!
    }

    private var list: List<SwapToken> by mutableStateOf(emptyList())
    private var fromToken: SwapToken? by mutableStateOf(null)
    private var toToken: SwapToken? by mutableStateOf(null)

    private val swapViewModel by viewModels<SwapViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    swapViewModel.web3Tokens()
                },
                successBlock = { resp ->
                    resp.data
                },
            )?.let {
                list = it
                fromToken = list[0]
                toToken = list[1]
            }
        }
        return ComposeView(inflater.context).apply {
            setContent {
                MixinAppTheme(
                    darkTheme = context.isNightMode(),
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = SwapDestination.Swap.name,
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
                        composable(SwapDestination.Swap.name) {
                            SwapPage(fromToken, toToken, list, {
                                val token = fromToken
                                fromToken = toToken
                                toToken = token
                            }, { index ->
                                if (list.isNotEmpty()) {
                                    selectCallback(list, index)
                                }
                            }) {
                                navigateUp(navController)
                            }
                        }
                    }
                }
            }
        }
    }

    private val selectCallback = fun(list: List<SwapToken>, index: Int) {
        SwapTokenListBottomSheetDialogFragment.newInstance(ArrayList(list)).apply {
            setOnClickListener { token ->
                if (index == 0) {
                    this@SwapFragment.fromToken = token
                } else {
                    this@SwapFragment.toToken = token
                }
                dismissNow()
            }
        }.show(parentFragmentManager, Web3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun navigateUp(navController: NavHostController) {
        if (!navController.safeNavigateUp()) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
