package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import blockchain.Blockchain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.generateRandomBytes
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.isNightMode
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupBeforePage
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupPage
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupPinPage
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupShownPage
import one.mixin.android.ui.setting.ui.page.MnemonicPhraseBackupVerifyPage
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class MnemonicPhraseBackupFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): MnemonicPhraseBackupFragment =
            MnemonicPhraseBackupFragment().apply {

            }
    }

    @Inject
    lateinit var tip: Tip

    private val binding by viewBinding(FragmentComposeBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()

    enum class MnemonicPhraseBackupStep {
        Initial, Before, Pin, MnemonicPhrase, MnemonicPhraseVerify
    }

    private var pin = ""
    private var mnemonic = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.isVisible = false
        binding.compose.setContent {
            MixinAppTheme(
                darkTheme = requireContext().isNightMode(),
            ) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MnemonicPhraseBackupStep.Initial.name,
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
                    composable(MnemonicPhraseBackupStep.Initial.name) {
                        MnemonicPhraseBackupPage({
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, {
                            navController.navigate(MnemonicPhraseBackupStep.Before.name)
                        })
                    }

                    composable(MnemonicPhraseBackupStep.Before.name) {
                        MnemonicPhraseBackupBeforePage({
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, {
                            navController.navigate(MnemonicPhraseBackupStep.Pin.name)
                        })
                    }

                    composable(MnemonicPhraseBackupStep.Pin.name) {
                        MnemonicPhraseBackupPinPage({
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, { pin ->
                            this@MnemonicPhraseBackupFragment.pin = pin
                            lifecycleScope.launch {
                                this@MnemonicPhraseBackupFragment.mnemonic = Blockchain.newMnemonic(generateRandomBytes(16))
                                navController.navigate(MnemonicPhraseBackupStep.MnemonicPhrase.name)
                            }
                        })
                    }

                    composable(MnemonicPhraseBackupStep.MnemonicPhrase.name) {
                        MnemonicPhraseBackupShownPage(mnemonic.split(" "), {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, {
                            navController.navigate(MnemonicPhraseBackupStep.MnemonicPhraseVerify.name)
                        })
                    }

                    composable(MnemonicPhraseBackupStep.MnemonicPhraseVerify.name) {
                        MnemonicPhraseBackupVerifyPage({
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, {
                            lifecycleScope.launch {
                                // val seed = tip.getOrRecoverTipPriv(requireContext(), pin).getOrThrow()
                                // val saltBase64 = tip.generateMnemonicSaltAndEncryptedSaltBase64(mnemonic, pin, seed)
                                // Todo salt export
                                // ExportRequest()
                                // walletViewModel.saltExport()
                            }
                        })
                    }
                }
            }
        }
    }
}