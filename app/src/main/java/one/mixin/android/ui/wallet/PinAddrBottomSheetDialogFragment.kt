package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.TRON_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.databinding.FragmentPinBottomSheetAddressBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.ErrorHandler.Companion.INVALID_ADDRESS
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class PinAddrBottomSheetDialogFragment : BiometricBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PinAddrBottomSheetDialogFragment"

        const val ADD = 0
        const val DELETE = 1
        const val MODIFY = 2

        const val ARGS_ASSET_ID = "args_asset_id"
        const val ARGS_ASSET_NAME = "args_asset_name"
        const val ARGS_ASSET_URL = "args_asset_url"
        const val ARGS_ASSET_SYMBOL = "args_asset_symbol"
        const val ARGS_CHAIN_ID = "args_chain_id"
        const val ARGS_CHAIN_NAME = "args_chain_name"
        const val ARGS_CHAIN_URL = "args_chain_url"
        const val ARGS_LABEL = "args_label"
        const val ARGS_DESTINATION = "args_destination"
        const val ARGS_TAG = "args_tag"
        const val ARGS_ADDRESS_ID = "args_address_id"
        const val ARGS_TYPE = "args_type"

        fun newInstance(
            assetId: String? = null,
            assetName: String? = null,
            assetUrl: String? = null,
            assetSymbol: String? = null,
            chainId: String? = null,
            chainName: String? = null,
            chainIconUrl: String? = null,
            label: String,
            destination: String,
            tag: String? = null,
            addressId: String? = null,
            type: Int = ADD
        ) = PinAddrBottomSheetDialogFragment().apply {
            val b = bundleOf(
                ARGS_ASSET_ID to assetId,
                ARGS_ASSET_NAME to assetName,
                ARGS_ASSET_URL to assetUrl,
                ARGS_ASSET_SYMBOL to assetSymbol,
                ARGS_CHAIN_ID to chainId,
                ARGS_CHAIN_NAME to chainName,
                ARGS_CHAIN_URL to chainIconUrl,
                ARGS_LABEL to label,
                ARGS_DESTINATION to destination,
                ARGS_ADDRESS_ID to addressId,
                ARGS_TYPE to type,
                ARGS_TAG to tag
            )
            arguments = b
        }
    }

    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val assetName: String? by lazy { requireArguments().getString(ARGS_ASSET_NAME) }
    private val assetUrl: String? by lazy { requireArguments().getString(ARGS_ASSET_URL) }
    private val assetSymbol: String? by lazy { requireArguments().getString(ARGS_ASSET_SYMBOL) }
    private val chainId: String? by lazy { requireArguments().getString(ARGS_CHAIN_ID) }
    private val chainName: String? by lazy { requireArguments().getString(ARGS_CHAIN_NAME) }
    private val chainIconUrl: String? by lazy { requireArguments().getString(ARGS_CHAIN_URL) }
    private val label: String? by lazy { requireArguments().getString(ARGS_LABEL) }
    private val destination: String? by lazy { requireArguments().getString(ARGS_DESTINATION) }
    private val addressId: String? by lazy { requireArguments().getString(ARGS_ADDRESS_ID) }
    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }
    private val addressTag: String? by lazy { requireArguments().getString(ARGS_TAG) }

    private val binding by viewBinding(FragmentPinBottomSheetAddressBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        binding.apply {
            titleView.rightIv.setOnClickListener { dismiss() }
            title.text = getTitle()
            assetIcon.bg.loadImage(assetUrl, R.drawable.ic_avatar_place_holder)
            assetIcon.badge.loadImage(chainIconUrl, R.drawable.ic_avatar_place_holder)
            assetName.text = label
            assetAddress.text = if (addressTag.isNullOrBlank()) destination else "$destination:$addressTag"
            biometricLayout.payTv.text = getTipText()
            biometricLayout.biometricTv.text = getBiometricText()
        }
    }

    override fun getBiometricInfo(): BiometricInfo {
        return BiometricInfo(
            getTitle(),
            label ?: "",
            destination ?: "",
            getTipText()
        )
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return if (type == ADD || type == MODIFY) {
            bottomViewModel.syncAddr(assetId!!, destination, label, addressTag, pin)
        } else {
            bottomViewModel.deleteAddr(addressId!!, pin)
        }
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        lifecycleScope.launch {
            if (type == ADD || type == MODIFY) {
                bottomViewModel.saveAddr(response.data as Address)
            } else {
                bottomViewModel.deleteLocalAddr(addressId!!)
            }
            binding.biometricLayout.showPin(false)
        }
        return true
    }

    override suspend fun doWithMixinErrorCode(errorCode: Int, pin: String): String? {
        return if (errorCode == INVALID_ADDRESS) {
            getString(
                R.string.error_invalid_address, INVALID_ADDRESS,
                when (chainId) {
                    ETHEREUM_CHAIN_ID -> "Ethereum(ERC20)"
                    TRON_CHAIN_ID -> "TRON(TRC20)"
                    else -> chainName
                },
                assetSymbol
            )
        } else null
    }

    private fun getTitle() = getString(
        when (type) {
            ADD -> R.string.withdrawal_addr_add
            MODIFY -> R.string.withdrawal_addr_modify
            else -> R.string.Delete_withdraw_Address
        },
        assetName
    )

    private fun getTipText() = getString(
        when (type) {
            ADD -> R.string.withdrawal_addr_pin_add
            DELETE -> R.string.withdrawal_addr_pin_delete
            MODIFY -> R.string.withdrawal_addr_pin_modify
            else -> R.string.withdrawal_addr_pin_add
        }
    )

    private fun getBiometricText() = getString(
        when (type) {
            ADD -> R.string.withdrawal_addr_biometric_add
            DELETE -> R.string.withdrawal_addr_biometric_delete
            MODIFY -> R.string.withdrawal_addr_biometric_modify
            else -> R.string.withdrawal_addr_biometric_add
        }
    )
}
