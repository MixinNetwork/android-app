package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
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
import one.mixin.android.ui.landing.components.SetPinPage
import one.mixin.android.ui.landing.components.SetupPinPage
import one.mixin.android.ui.landing.viewmodel.LandingViewModel
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SetupPinFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): SetupPinFragment =
            SetupPinFragment().apply {

            }
    }

    private val landingViewModel by viewModels<LandingViewModel>()
    private val binding by viewBinding(FragmentComposeBinding::bind)

    enum class SetupPinDestination {
        Initial, Setup, Loading, Quiz
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.isVisible = false
        binding.compose.setContent {
            MixinAppTheme(
                darkTheme = requireContext().isNightMode(),
            ) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = SetupPinDestination.Initial.name,
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
                    composable(SetupPinDestination.Initial.name) {
                        SetPinPage {
                            navController.navigate(SetupPinDestination.Setup.name)
                        }
                    }
                    composable(SetupPinDestination.Setup.name) {
                        SetupPinPage({
                            navController.navigate(SetupPinDestination.Initial.name)
                        }, {
                            navController.navigate(SetupPinDestination.Loading.name)
                        })
                    }
                    composable(SetupPinDestination.Loading.name) {
                        SetPinLoadingPage {
                            navController.navigate(SetupPinDestination.Quiz.name)
                        }
                    }
                    composable(SetupPinDestination.Quiz.name) {
                        QuizPage {
                            // Todo
                            navController.navigate(SetupPinDestination.Initial.name)
                        }
                    }
                }
            }
        }
    }
}