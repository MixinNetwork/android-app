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
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexString
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.components.QuizPage
import one.mixin.android.ui.landing.components.SetPinLoadingPage
import one.mixin.android.ui.landing.components.SetPinPage
import one.mixin.android.ui.landing.components.SetupPinPage
import one.mixin.android.ui.tip.LegacyPIN
import one.mixin.android.ui.tip.Processing
import one.mixin.android.ui.tip.RetryRegister
import one.mixin.android.ui.tip.TipViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.js.JsSigner
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SetupPinFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"
        const val KEY_START_DESTINATION = "key_start_destination"

        fun newInstance(
            startDestination: String = SetupPinDestination.Initial.name,
        ): SetupPinFragment =
            SetupPinFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_START_DESTINATION, startDestination)
                }
            }
    }

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
                val startDestination =
                    arguments?.getString(KEY_START_DESTINATION)
                        ?: SetupPinDestination.Initial.name
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
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
                            requireContext().defaultSharedPreferences.putBoolean(Constants.Account.PREF_QUIZ_PENDING, true)
                            navController.navigate(SetupPinDestination.Loading.name)
                        })
                    }
                    composable(SetupPinDestination.Quiz.name) {
                        QuizPage {
                            requireContext().defaultSharedPreferences.putBoolean(Constants.Account.PREF_QUIZ_PENDING, false)
                            MainActivity.show(requireContext())
                            requireActivity().finish()
                        }
                    }
                    composable(SetupPinDestination.Loading.name) {
                        SetPinLoadingPage {
                            navController.navigate(SetupPinDestination.Quiz.name)
                        }
                    }
                }
            }
        }
    }
}