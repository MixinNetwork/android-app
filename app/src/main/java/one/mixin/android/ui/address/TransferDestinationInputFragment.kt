package one.mixin.android.ui.address

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.supportDepositFromMixin
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.component.TransferDestinationInputPage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import org.web3j.crypto.WalletUtils

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            compose.setContent {
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
                    }
                )
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
                    getScanResult.launch(Pair(CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT, true))
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private val supportDeposit by lazy {
        web3Token?.supportDepositFromMixin() == true
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