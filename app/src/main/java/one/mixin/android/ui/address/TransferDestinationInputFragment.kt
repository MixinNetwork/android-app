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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.isSolana
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.ui.address.page.AddressInputPage
import one.mixin.android.ui.address.page.LabelInputPage
import one.mixin.android.ui.address.page.MemoInputPage
import one.mixin.android.ui.address.page.TransferDestinationInputPage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.buildWithdrawalBiometricItem
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.InputFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransferContactBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import org.web3j.crypto.WalletUtils
import javax.inject.Inject

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
                    putParcelable(ARGS_ASSET, token)
                }
            }
    }

    private val token: TokenItem? by lazy {
        requireArguments().getParcelableCompat(
            ARGS_ASSET,
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
    private var currentScanType: ScanType? by mutableStateOf(null)
    private var scannedAddress by mutableStateOf("")
    private var scannedMemo by mutableStateOf("")
    private var scannedLabel by mutableStateOf("")
    private var scannedTransferDest by mutableStateOf("")

    enum class ScanType { ADDRESS, MEMO, LABEL, TRANSFER_DEST }

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
                ::handleScanResult,
            )
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    enum class TransferDestination {
        Initial,
        Address,
        SendMemo,
        Memo,
        Label
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(SyncOutputJob())
        binding.apply {
            compose.setContent {
                MixinAppTheme {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "${TransferDestination.Initial.name}?address=false",
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
                        composable(
                            route = TransferDestination.Initial.name
                        ) { backStackEntry ->
                            val addressShown = backStackEntry.arguments?.getBoolean("address") ?: false
                            TransferDestinationInputPage(
                                token = token,
                                web3Token = web3Token,
                                web3Chain = chainToken,
                                addressShown = addressShown,
                                pop = {
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                },
                                onScan = {
                                    startQrScan(ScanType.TRANSFER_DEST)
                                },
                                contentText = scannedTransferDest,
                                toAccount = { address ->
                                    requireView().hideKeyboard()
                                    navTo(
                                        InputFragment.newInstance(token!!, address),
                                        InputFragment.TAG
                                    )
                                },
                                toContact = {
                                    requireView().hideKeyboard()
                                    token?.let { t ->
                                        TransferContactBottomSheetDialogFragment.newInstance(t)
                                            .apply {
                                                onUserClick = { user->
                                                    this@TransferDestinationInputFragment.navTo(InputFragment.newInstance(t, user), InputFragment.TAG)
                                                }
                                            }
                                            .show(
                                                parentFragmentManager,
                                                TransferContactBottomSheetDialogFragment.TAG
                                            )
                                    }
                                },
                                toWallet = {
                                    requireView().hideKeyboard()
                                    val dialog =
                                        indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                            setCancelable(false)
                                        }
                                    lifecycleScope.launch {
                                        dialog.show()
                                        web3Token?.let {
                                            val deposit = web3ViewModel.findAndSyncDepositEntry(it) ?: return@launch
                                            val fromAddress = if (it.isSolana()) {
                                                PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                                            } else {
                                                PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                                            }

                                            if (fromAddress.isBlank()) {
                                                toast(R.string.Alert_Not_Support)
                                            } else {
                                                navTo(InputFragment.newInstance(fromAddress = fromAddress, toAddress = deposit.destination, web3Token = it, chainToken = chainToken, toWallet = true), InputFragment.TAG)
                                            }
                                        }
                                        dialog.dismiss()
                                    }
                                },
                                toAddAddress = {
                                    navController.navigate(TransferDestination.Address.name)
                                },
                                onSend = { address ->
                                    val memoEnabled =
                                        token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE
                                    if (memoEnabled) {
                                        navController.navigate("${TransferDestination.SendMemo.name}?address=${address}")
                                    } else if (web3Token != null) {
                                        lifecycleScope.launch {
                                            web3Token?.let {
                                                val deposit = web3ViewModel.findAndSyncDepositEntry(it) ?: return@launch
                                                val fromAddress = if (it.isSolana()) {
                                                    PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                                                } else {
                                                    PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                                                }
                                                if (fromAddress.isBlank()) {
                                                    toast(R.string.Alert_Not_Support)
                                                } else {
                                                    navTo(InputFragment.newInstance(fromAddress = fromAddress, toAddress = address, web3Token = it, chainToken= chainToken), InputFragment.TAG)
                                                }
                                            }
                                        }
                                    } else {
                                        requireView().hideKeyboard()
                                        navTo(InputFragment.newInstance(token!!, address), InputFragment.TAG)
                                    }
                                },
                                onAddressClick = { address ->
                                    requireView().hideKeyboard()
                                    if (web3Token != null) {
                                        val dialog =
                                            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                                setCancelable(false)
                                            }
                                        lifecycleScope.launch {
                                            dialog.show()
                                            val fromAddress = if (web3Token!!.isSolana()) {
                                                PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                                            } else {
                                                PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                                            }
                                            val token = web3ViewModel.syncAsset(web3Token!!.assetId ?: "")
                                            if (token == null || fromAddress.isBlank()) {
                                                toast(R.string.Alert_Not_Support)
                                            } else {
                                                navTo(
                                                    InputFragment.newInstance(
                                                        fromAddress = fromAddress,
                                                        toAddress = address.destination,
                                                        web3Token = web3Token!!,
                                                        chainToken = chainToken,
                                                        label = address.label
                                                    ), InputFragment.TAG
                                                )
                                            }
                                            dialog.dismiss()
                                        }
                                    } else {
                                        navTo(
                                            InputFragment.newInstance(buildWithdrawalBiometricItem(address, token!!)),
                                            InputFragment.TAG
                                        )
                                    }
                                },
                                onDeleteAddress = { address ->
                                    showDeleteBottomSheet(address, token!!)
                                }
                            )
                        }
                        composable(TransferDestination.Address.name) {
                            AddressInputPage(
                                token = token,
                                web3Token = web3Token,
                                contentText = scannedAddress,
                                onNext = { address ->
                                    if (token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE)
                                        navController.navigate("${TransferDestination.Memo.name}?address=$address")
                                    else
                                        navController.navigate("${TransferDestination.Label.name}?address=$address")
                                },
                                onScan = { startQrScan(ScanType.ADDRESS) },
                                pop = { findNavController().popBackStack() }
                            )
                        }

                        composable(
                            route = "${TransferDestination.SendMemo.name}?address={address}",
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val address = backStackEntry.arguments?.getString("address") ?: ""
                            MemoInputPage(
                                token = token,
                                web3Token = web3Token,
                                address = address,
                                contentText = scannedMemo,
                                onNext = { memo ->
                                    requireView().hideKeyboard()
                                    navTo(InputFragment.newInstance(token!!, address, memo), InputFragment.TAG)
                                },
                                onScan = { startQrScan(ScanType.MEMO) },
                                pop = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "${TransferDestination.Memo.name}?address={address}",
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val address = backStackEntry.arguments?.getString("address") ?: ""
                            MemoInputPage(
                                token = token,
                                web3Token = web3Token,
                                address = address,
                                contentText = scannedMemo,
                                onNext = { memo ->
                                    navController.navigate("${TransferDestination.Label.name}?address=${address}&memo=${memo}")
                                },
                                onScan = { startQrScan(ScanType.MEMO) },
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
                                contentText = scannedLabel,
                                onScan = { startQrScan(ScanType.LABEL) },
                                onComplete = { label ->
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

                                    bottomSheet.showNow(
                                        parentFragmentManager,
                                        TransferBottomSheetDialogFragment.TAG
                                    )
                                    scannedMemo = ""
                                    scannedLabel = ""
                                    scannedAddress = ""
                                    bottomSheet.setCallback(
                                        object : TransferBottomSheetDialogFragment.Callback() {
                                            override fun onDismiss(success: Boolean) {
                                                if (success) {
                                                    navController.popBackStack(TransferDestination.Initial.name, inclusive = false)
                                                }
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

    private fun startQrScan(scanType: ScanType) {
        currentScanType = scanType
        when (currentScanType) {
            ScanType.ADDRESS -> scannedTransferDest = ""
            ScanType.MEMO -> scannedMemo = ""
            ScanType.LABEL -> scannedLabel = ""
            ScanType.TRANSFER_DEST -> scannedTransferDest = ""
            null -> Unit
        }
        handleClick()
    }

    private fun handleScanResult(data: Intent?, isAddr: Boolean = true) {
        if (data == null) return

        data.getStringExtra(CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT)?.let { result ->
            when (currentScanType) {
                ScanType.ADDRESS -> {
                    scannedAddress = if (isIcapAddress(result)) {
                        decodeICAP(result)
                    } else {
                        result
                    }
                }

                ScanType.MEMO -> scannedMemo = result
                ScanType.LABEL -> scannedLabel = result
                ScanType.TRANSFER_DEST -> scannedTransferDest = result
                null -> Unit
            }
        }
        currentScanType = null
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

    private fun showDeleteBottomSheet(
        address: Address,
        asset: TokenItem,
    ): TransferBottomSheetDialogFragment {
        val bottomSheet =
            TransferBottomSheetDialogFragment.newInstance(
                AddressManageBiometricItem(
                    asset = asset,
                    addressId = address.addressId,
                    label = address.label,
                    tag = address.tag,
                    destination = address.destination,
                    type = TransferBottomSheetDialogFragment.DELETE,
                ),
            ).apply {
                setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                    override fun onDismiss(success: Boolean) {

                    }
                })
            }
        bottomSheet.showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        return bottomSheet
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