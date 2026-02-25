package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.QuizPage
import one.mixin.android.ui.landing.components.SetPinLoadingPage
import one.mixin.android.ui.landing.components.SetupPinPage
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import timber.log.Timber

@AndroidEntryPoint
class SetupPinFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): SetupPinFragment =
            SetupPinFragment().apply {

            }
    }
    private val binding by viewBinding(FragmentComposeBinding::bind)

    enum class SetupPinDestination {
        Setup, Loading, Quiz
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("$TAG onViewCreated")
        AnalyticsTracker.trackSignUpPinSet()
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.titleView.isVisible = false
        binding.compose.setContent {
            MixinAppTheme(
                darkTheme = requireContext().isNightMode(),
            ) {
                val navController = rememberNavController()
                var pin: String by remember { mutableStateOf("") }
                var errorMessage: String by remember { mutableStateOf("") }
                NavHost(
                    navController = navController,
                    startDestination = SetupPinDestination.Setup.name,
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
                    composable(SetupPinDestination.Setup.name) {
                        SetupPinPage(
                            next = { pinValue ->
                                Timber.e("$TAG Setup PIN completed")
                                pin = pinValue
                                navController.navigate(SetupPinDestination.Quiz.name)
                            },
                            errorMessage = errorMessage,
                            onRetry = {
                                Timber.e("$TAG Retry setup PIN")
                                errorMessage = ""
                            }
                        )
                    }
                    composable(SetupPinDestination.Quiz.name) {
                        QuizPage(
                            next = {
                                Timber.e("$TAG Quiz completed")
                                navController.navigate(SetupPinDestination.Loading.name)
                            },
                            pop = {
                                Timber.e("$TAG Quiz back pressed")
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(SetupPinDestination.Loading.name) {
                        SetPinLoadingPage(
                            pin = pin,
                            next = {
                                Timber.e("$TAG PIN set successfully")
                                AnalyticsTracker.trackSignUpEnd()
                                requireActivity().finish()
                            }
                        )
                    }
                }
            }
        }
    }
}