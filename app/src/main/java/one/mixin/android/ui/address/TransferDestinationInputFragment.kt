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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.ui.address.page.AddressInputPage
import one.mixin.android.ui.address.page.LabelInputPage
import one.mixin.android.ui.address.page.MemoInputPage
import one.mixin.android.ui.address.page.TransferDestinationInputPage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.buildWithdrawalBiometricItem
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.InputFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransferContactBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
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
            web3Token: Web3TokenItem,
            chainToken: Web3TokenItem,
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
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
    }

    private val chainToken by lazy {
        requireArguments().getParcelableCompat(ARGS_CHAIN_TOKEN, Web3TokenItem::class.java)
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
        (token?.chainId ?: web3Token?.chainId)?.let {
            jobManager.addJobInBackground(RefreshAddressJob(it))
        }
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
                                    token?.let { t ->
                                        validateAndNavigateToInput(
                                            assetId = t.assetId,
                                            destination = address,
                                            asset = t,
                                            toAccount = true
                                        )
                                    }
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
                                        web3Token?.let { token ->
                                            val deposit = web3ViewModel.findAndSyncDepositEntry(token)
                                            if (deposit == null) {
                                                dialog.dismiss()
                                                return@launch
                                            }
                                            val fromAddress = if (token.isSolana()) {
                                                PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                                            } else {
                                                PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                                            }

                                            if (fromAddress.isBlank()) {
                                                toast(R.string.Alert_Not_Support)
                                            } else {
                                                (chainToken ?: web3ViewModel.web3TokenItemById(token.chainId))?.let { chain ->
                                                    navTo(InputFragment.newInstance(fromAddress = fromAddress, toAddress = deposit.destination, web3Token = token, chainToken = chain, toWallet = true), InputFragment.TAG)
                                                }
                                            }
                                        }
                                        dialog.dismiss()
                                    }
                                },
                                toAddAddress = {
                                    navController.navigate(TransferDestination.Address.name)
                                },
                                onSend = { address ->
                                    if (address.isExternalTransferUrl() || address.isLightningUrl()) {
                                        LinkBottomSheetDialogFragment.newInstance(address).show(
                                            parentFragmentManager,
                                            LinkBottomSheetDialogFragment.TAG
                                        )
                                    } else {
                                        val memoEnabled =
                                            token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE
                                        if (memoEnabled) {
                                            navController.navigate("${TransferDestination.SendMemo.name}?address=${address}")
                                        } else if (web3Token != null) {
                                            lifecycleScope.launch {
                                                web3Token?.let { token ->
                                                    val fromAddress = if (token.isSolana()) {
                                                        PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                                                    } else {
                                                        PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                                                    }
                                                    if (fromAddress.isBlank()) {
                                                        toast(R.string.Alert_Not_Support)
                                                    } else {
                                                        val chain = chainToken ?: web3ViewModel.web3TokenItemById(token.chainId) ?:return@launch
                                                        validateAndNavigateToInput(
                                                            assetId = token.assetId,
                                                            destination = address,
                                                            fromAddress = fromAddress,
                                                            web3Token = token,
                                                            chainToken = chain
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            token?.let { t ->
                                                validateAndNavigateToInput(
                                                    assetId = t.assetId,
                                                    destination = address,
                                                    asset = t
                                                )
                                            }
                                        }
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
                                            if (fromAddress.isBlank()) {
                                                toast(R.string.Alert_Not_Support)
                                            } else {
                                                (chainToken ?: web3ViewModel.web3TokenItemById(
                                                    web3Token!!.chainId
                                                ))?.let { chain ->
                                                    navTo(
                                                        InputFragment.newInstance(
                                                            fromAddress = fromAddress,
                                                            toAddress = address.destination,
                                                            web3Token = web3Token!!,
                                                            chainToken = chain,
                                                            label = address.label
                                                        ), InputFragment.TAG
                                                    )
                                                }
                                            }
                                            dialog.dismiss()
                                        }
                                    } else if (token != null){
                                        navTo(
                                            InputFragment.newInstance(buildWithdrawalBiometricItem(address, token!!)),
                                            InputFragment.TAG
                                        )
                                    }
                                },
                                onDeleteAddress = { address ->
                                    if (token == null && web3Token != null) {
                                        lifecycleScope.launch {
                                            val t = web3ViewModel.syncAsset(web3Token!!.assetId) ?: return@launch
                                            showDeleteBottomSheet(address, t)
                                        }
                                    } else if (token != null) {
                                        showDeleteBottomSheet(address, token!!)
                                    } else {
                                        toast(R.string.Data_error)
                                    }
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
                                    token?.let { t ->
                                        validateAndNavigateToInput(
                                            assetId = t.assetId,
                                            destination = address,
                                            tag = memo,
                                            asset = t
                                        )
                                    }
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
                                    if (token == null && web3Token != null) {
                                        lifecycleScope.launch {
                                            val t = web3ViewModel.syncAsset(web3Token!!.assetId) ?: return@launch
                                            handleLabelComplete(t, address, memo, label, navController)
                                        }
                                    } else if (token != null) {
                                        handleLabelComplete(token!!, address, memo, label, navController)
                                    } else {
                                        toast(R.string.Data_error)
                                    }
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
            if (result.isLightningUrl() || result.isExternalTransferUrl()) {
                LinkBottomSheetDialogFragment.newInstance(result).show(
                    parentFragmentManager,
                    LinkBottomSheetDialogFragment.TAG
                )
                return@let
            }
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

    private fun validateAndNavigateToInput(
        assetId: String,
        destination: String,
        tag: String? = null,
        fromAddress: String? = null,
        web3Token: Web3TokenItem? = null,
        chainToken: Web3TokenItem? = null,
        asset: TokenItem? = null,
        toAccount: Boolean? = null,
    ) {
        requireView().hideKeyboard()
        val dialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
        
        lifecycleScope.launch {
            dialog.show()
            try {
                if (assetId.isNotEmpty() && destination.isNotEmpty()) {
                    val response = viewModel.validateExternalAddress(assetId, destination, tag)
                    if (response.isSuccess) {
                        val addressLabel = withContext(Dispatchers.IO) {
                            viewModel.findAddressByReceiver(destination, tag ?: "")
                        }
                        when {
                            asset != null && destination.isNotEmpty() && tag != null -> {
                                navTo(InputFragment.newInstance(asset, destination, tag, label = addressLabel), InputFragment.TAG)
                            }
                            asset != null && destination.isNotEmpty() -> {
                                navTo(InputFragment.newInstance(asset, destination, toAccount = toAccount, label = addressLabel), InputFragment.TAG)
                            }
                            fromAddress != null && destination.isNotEmpty() && web3Token != null && chainToken != null -> {
                                navTo(InputFragment.newInstance(fromAddress = fromAddress, toAddress = destination, web3Token = web3Token, chainToken = chainToken, label = addressLabel), InputFragment.TAG)
                            }
                        }
                    } else {
                        toast(response.errorDescription)
                    }
                }
            } catch (e: Exception) {
                toast(e.message?:getString(R.string.Data_error))
            } finally {
                dialog.dismiss()
            }
        }
    }

    private fun handleLabelComplete(
        asset: TokenItem,
        address: String,
        memo: String?,
        label: String,
        navController: NavHostController,
    ) {
        val bottomSheet = TransferBottomSheetDialogFragment.newInstance(
            AddressManageBiometricItem(
                asset = asset,
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
                        navController.popBackStack(
                            TransferDestination.Initial.name,
                            inclusive = false
                        )
                    }
                }
            },
        )
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
        // todo
        // return if (web3Token?.chainName.equals("solana", true) == true) {
        //     https://github.com/solana-labs/solana-web3.js/blob/afe5602674b2eb8f5e780097d98e1d60ec63606b/packages/addresses/src/address.ts#L36
        //     if (address.length < 32 || address.length > 44) {
        //         return false
        //     }
        //     return try {
        //         address.decodeBase58().size == 32
        //     } catch (e: Exception) {
        //         false
        //     }
        // } else {
        //     WalletUtils.isValidAddress(address)
        // }
        return true
    }
}