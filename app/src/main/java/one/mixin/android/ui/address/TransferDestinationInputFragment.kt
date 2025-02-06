package one.mixin.android.ui.address

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.page.AddressInputPage
import one.mixin.android.ui.address.page.LabelInputPage
import one.mixin.android.ui.address.page.MemoInputPage
import one.mixin.android.ui.address.page.TransferDestinationInputPage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import org.web3j.crypto.WalletUtils
import timber.log.Timber

@AndroidEntryPoint
class TransferDestinationInputFragment() : BaseFragment(R.layout.fragment_address_input) {
    companion object {
        const val TAG = "TransferDestinationInputFragment"
        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"
        const val ARGS_ADDRESS = "args_address"

        fun newInstance(
            address: String,
            web3Token: Web3Token,
            chainToken: Web3Token?,
        ) =
            TransferDestinationInputFragment().apply {
                withArgs {
                    putParcelable(ARGS_WEB3_TOKEN, web3Token)
                    putParcelable(ARGS_CHAIN_TOKEN, chainToken)
                    putString(ARGS_ADDRESS, address)
                }
            }

        fun newInstance(
            token: TokenItem,
        ) =
            TransferDestinationInputFragment().apply {
                withArgs {
                    putParcelable(TransactionsFragment.Companion.ARGS_ASSET, token)
                }
            }
    }

    private val token: TokenItem? by lazy {
        requireArguments().getParcelableCompat(
            TransactionsFragment.Companion.ARGS_ASSET,
            TokenItem::class.java
        )
    }

    private val address by lazy {
        requireArguments().getString(ARGS_ADDRESS)
    }

    private val web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3Token::class.java)
    }

    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_CHAIN_TOKEN, Web3Token::class.java)
    }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    // for testing
    private lateinit var resultRegistry: ActivityResultRegistry

    // testing constructor
    @VisibleForTesting(otherwise = VisibleForTesting.Companion.NONE)
    constructor(
        testRegistry: ActivityResultRegistry,
    ) : this() {
        resultRegistry = testRegistry
    }

    private val viewModel: AddressViewModel by viewModels()
    private var contentText by mutableStateOf("")

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>
    private val binding by viewBinding(FragmentAddressInputBinding::bind)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) {
            resultRegistry =
                requireActivity().activityResultRegistry
        }

        getScanResult =
            registerForActivityResult(
                CaptureActivity.CaptureContract(),
                resultRegistry,
                ::callbackScan,
            )
    }

    enum class TransferDestination {
        Initial,
        Address,
        Memo,
        Label
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            compose.setContent {
                MixinAppTheme {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = TransferDestination.Initial.name,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable(TransferDestination.Initial.name) {
                            TransferDestinationInputPage(
                                token = token,
                                web3Token = web3Token,
                                web3Chain = chainToken,
                                pop = {
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                },
                                onScan = {
                                    handleClick()
                                },
                                contentText = contentText,
                                onContentTextChange = { text ->
                                    contentText = text
                                },
                                onAddClick = {
                                    navController.navigate(TransferDestination.Address.name)
                                }
                            )
                        }
                        composable(TransferDestination.Address.name) {
                            AddressInputPage(
                                token = token,
                                web3Token = web3Token,
                                onNext = { address ->
                                    if (token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE)
                                        navController.navigate("${TransferDestination.Memo.name}?address=$address")
                                    else
                                        navController.navigate("${TransferDestination.Label.name}?address=$address")
                                },
                                pop = { findNavController().popBackStack() }
                            )
                        }

                        composable(
                            route = "${TransferDestination.Memo.name}?address={address}",
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val address = backStackEntry.arguments?.getString("address")
                            MemoInputPage(
                                token = token,
                                web3Token = web3Token,
                                onNext = { memo ->
                                    navController.navigate("${TransferDestination.Label.name}?address=${address}&memo=${memo}")
                                },
                                pop = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "${TransferDestination.Label.name}?address={address}&memo={memo}",
                            arguments = listOf(
                                navArgument("address") { type = NavType.StringType },
                                navArgument("memo") {
                                    type = NavType.StringType
                                    nullable = true 
                                }
                            )
                        ) { backStackEntry ->
                            val address = backStackEntry.arguments?.getString("address") ?: ""
                            val memo = backStackEntry.arguments?.getString("memo")
                            LabelInputPage(
                                token = token,
                                web3Token = web3Token,
                                address = address,
                                memo = memo,
                                onComplete = { address, memo, label ->
                                    Timber.e("$address $memo $label")
                                    val bottomSheet =
                                        TransferBottomSheetDialogFragment.newInstance(
                                            AddressManageBiometricItem(
                                                asset = token,
                                                label = label,
                                                addressId = null,
                                                destination = address,
                                                tag = memo ?: "",
                                                type = TransferBottomSheetDialogFragment.ADD,
                                            ),
                                        )

                                    bottomSheet.showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                                    bottomSheet.setCallback(
                                        object : TransferBottomSheetDialogFragment.Callback() {
                                            override fun onDismiss(success: Boolean) {
                                                navController.navigate(TransferDestination.Address.name)
                                            }
                                        },
                                    )
                                },
                                pop = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun callbackScan(
        data: Intent?,
        isAddr: Boolean = true,
    ) {
        val text = data?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
        if (text != null) {
            contentText = if (isIcapAddress(text)) {
                decodeICAP(text)
            } else {
                text
            }
        }
    }

    private fun handleClick() {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    getScanResult.launch(
                        Pair(
                            CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT,
                            true
                        )
                    )
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun isValidAddress(address: String): Boolean {
        return if (web3Token?.chainName.equals("solana", true) == true) {
            // https://github.com/solana-labs/solana-web3.js/blob/afe5602674b2eb8f5e780097d98e1d60ec63606b/packages/addresses/src/address.ts#L36
            if (address.length < 32 || address.length > 44) {
                return false
            }
            return try {
                address.decodeBase58().size == 32
            } catch (e: Exception) {
                false
            }
        } else {
            WalletUtils.isValidAddress(address)
        }
    }
}