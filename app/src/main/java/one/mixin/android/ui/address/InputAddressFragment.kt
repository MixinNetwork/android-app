package one.mixin.android.ui.address

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.supportDepositFromMixin
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.address.component.AddressBottom
import one.mixin.android.ui.address.component.AddressPage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.getChainNetwork
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.InputFragment
import one.mixin.android.widget.CoilRoundedHexagonTransformation
import org.web3j.crypto.WalletUtils

@AndroidEntryPoint
class InputAddressFragment() : BaseFragment(R.layout.fragment_address_input) {
    companion object {
        const val TAG = "InputAddressFragment"
        const val ARGS_WEB3_TOKEN = "args_web3_token"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"
        const val ARGS_ADDRESS = "args_address"

        fun newInstance(
            address: String,
            web3Token: Web3Token,
            chainToken: Web3Token?,
        ) =
            InputAddressFragment().apply {
                withArgs {
                    putParcelable(ARGS_WEB3_TOKEN, web3Token)
                    putParcelable(ARGS_CHAIN_TOKEN, chainToken)
                    putString(ARGS_ADDRESS, address)
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
        binding.titleView.rightAnimator.isEnabled = false
        binding.titleView.leftIb.setOnClickListener {
            if (viewDestroyed()) return@setOnClickListener

            if (binding.addrEt.isFocused) binding.addrEt.hideKeyboard()
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.apply {
            if (token?.collectionHash != null) {
                tokenIcon.loadImage(
                    token?.iconUrl ?: web3Token?.iconUrl,
                    R.drawable.ic_avatar_place_holder,
                    transformation = CoilRoundedHexagonTransformation()
                )
            } else {
                tokenIcon.loadImage(
                    token?.iconUrl ?: web3Token?.iconUrl,
                    R.drawable.ic_avatar_place_holder,
                )
            }
            tokenName.text = token?.name ?: web3Token?.name
            token?.let { token ->
                val networkName = getChainNetwork(token.assetId, token.chainId, token.chainId)
                tokenNetworkName.isVisible = networkName.isNullOrEmpty().not()
                tokenNetworkName.text = networkName
            }
            balance.text =
                "Bal: ${token?.balance ?: web3Token?.balance} ${token?.symbol ?: web3Token?.symbol}"
        }
        binding.continueTv.setOnClickListener {
            val destination = binding.addrEt.text.toString()
            binding.addrEt.hideKeyboard()
            // todo
            address ?: return@setOnClickListener
            web3Token ?: return@setOnClickListener
            navTo(
                InputFragment.Companion.newInstance(
                    address!!,
                    destination,
                    web3Token!!,
                    chainToken
                ), InputFragment.Companion.TAG
            )
        }
        binding.addrVa.setOnClickListener {
            if (binding.addrVa.displayedChild == 0) {
                handleClick()
            } else {
                binding.addrEt.setText("")
            }
        }
        binding.mixinIdTv.text =
            getString(R.string.contact_mixin_id, Session.getAccount()?.identityNumber)
        binding.toRl.setOnClickListener {
            lifecycleScope.launch {
                // Todos
                address ?: return@launch
                web3Token ?: return@launch
                var toAddress = web3ViewModel.findAddres(web3Token!!)
                binding.addrEt.hideKeyboard()
                if (toAddress != null) {
                    navTo(
                        InputFragment.Companion.newInstance(
                            address!!,
                            toAddress,
                            web3Token!!,
                            chainToken
                        ), InputFragment.Companion.TAG
                    )
                } else {
                    val alertDialog =
                        indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                            show()
                        }
                    toAddress = web3ViewModel.findAndSyncDepositEntry(web3Token!!)
                    alertDialog.dismiss()
                    if (toAddress != null) {
                        navTo(
                            InputFragment.Companion.newInstance(
                                address!!,
                                toAddress,
                                web3Token!!,
                                chainToken
                            ), InputFragment.Companion.TAG
                        )
                    } else {
                        toast(R.string.Not_found)
                    }
                }
            }
        }
        binding.walletLl.isVisible = token == null && supportDeposit
        token?.let { t ->
            binding.address.isVisible = true
            binding.compose.isVisible = true
            binding.compose.setContent {
                AddressPage(t)
            }
        }
        binding.bottom.setContent {
            if (token != null)
                AddressBottom(token!!)// Todo web3
        }
        binding.addrEt.addTextChangedListener(mWatcher)
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

    private fun callbackScan(
        data: Intent?,
        isAddr: Boolean = true,
    ) {
        val text = data?.getStringExtra(CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT)
        if (text != null) {
            if (isIcapAddress(text)) {
                binding.addrEt.setText(decodeICAP(text))
            } else {
                binding.addrEt.setText(text)
            }
        }
    }

    private val mWatcher: TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                if (viewDestroyed()) return
                if (s.isEmpty()) {
                    binding.addrVa.displayedChild = 0
                    binding.walletLl.isVisible = supportDeposit
                    binding.compose.isVisible = true
                } else {
                    binding.addrVa.displayedChild = 1
                    binding.walletLl.isVisible = false
                    binding.compose.isVisible = false
                }
                updateSaveButton()
            }
        }

    private val supportDeposit by lazy {
        web3Token?.supportDepositFromMixin() == true
    }

    private fun updateSaveButton() {
        if (binding.addrEt.text.isNotEmpty() && isValidAddress(binding.addrEt.text.toString())) {
            binding.continueTv.isEnabled = true
            binding.continueTv.textColor = requireContext().getColor(R.color.white)
        } else {
            binding.continueTv.isEnabled = false
            binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
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