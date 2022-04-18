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
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.EOS_CHAIN_ID
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAddressAddBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.textColor
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ADD
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem

@AndroidEntryPoint
class AddressAddFragment() : BaseFragment(R.layout.fragment_address_add) {
    companion object {
        const val ARGS_ADDRESS = "args_address"
    }

    lateinit var asset: AssetItem
    private var memoEnabled = true

    // for testing
    private lateinit var resultRegistry: ActivityResultRegistry

    // testing constructor
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        testRegistry: ActivityResultRegistry,
    ) : this() {
        resultRegistry = testRegistry
    }

    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>
    private lateinit var getScanMemoResult: ActivityResultLauncher<Pair<String, Boolean>>
    private val binding by viewBinding(FragmentAddressAddBinding::bind)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry =
            requireActivity().activityResultRegistry

        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract(),
            resultRegistry,
            ::callbackScan
        )
        getScanMemoResult = registerForActivityResult(
            CaptureActivity.CaptureContract(),
            resultRegistry,
            ::callbackScanMemo
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelable(ARGS_ASSET)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.rightAnimator.isEnabled = false
        binding.titleView.leftIb.setOnClickListener {
            if (viewDestroyed()) return@setOnClickListener

            if (binding.labelEt.isFocused) binding.labelEt.hideKeyboard()
            if (binding.addrEt.isFocused) binding.addrEt.hideKeyboard()
            if (binding.tagEt.isFocused) binding.tagEt.hideKeyboard()
            activity?.onBackPressed()
        }
        binding.titleView.titleTv.text = getString(R.string.withdrawal_addr_new, asset.symbol)
        binding.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        binding.saveTv.setOnClickListener {
            var destination = binding.addrEt.text.toString()
            if (asset.chainId == BITCOIN_CHAIN_ID) {
                val dest = BitcoinPaymentURI.parse(destination)
                if (dest != null) {
                    destination = dest.address
                }
            }
            val bottomSheet =
                PinAddrBottomSheetDialogFragment.newInstance(
                    asset.assetId,
                    asset.name,
                    assetUrl = asset.iconUrl,
                    assetSymbol = asset.symbol,
                    chainId = asset.chainId,
                    chainName = asset.chainName,
                    chainIconUrl = asset.chainIconUrl,
                    label = binding.labelEt.text.toString(),
                    destination = destination,
                    tag = if (memoEnabled) {
                        binding.tagEt.text.toString()
                    } else {
                        ""
                    },
                    type = ADD
                )

            bottomSheet.showNow(parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
            bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback() {
                override fun onSuccess() {
                    activity?.onBackPressed()
                }
            }
        }

        if (asset.assetId == RIPPLE_CHAIN_ID) {
            binding.tagEt.setHint(R.string.Tag)
        } else {
            binding.tagEt.setHint(R.string.withdrawal_memo)
        }
        if (asset.chainId == EOS_CHAIN_ID) {
            binding.tipTv.isVisible = true
            binding.tipTv.text = buildSpannedString {
                append(getString(R.string.wallet_address_add_tip))
                bold {
                    append(" ")
                    color(requireContext().colorFromAttribute(R.attr.text_primary)) {
                        append(getString(R.string.eos_contract_address))
                    }
                }
            }
        } else {
            binding.tipTv.isVisible = false
        }
        binding.labelEt.addTextChangedListener(mWatcher)
        binding.addrEt.addTextChangedListener(mWatcher)
        binding.tagEt.addTextChangedListener(mWatcher)
        binding.addrIv.setOnClickListener { handleClick(true) }
        handleMemo()
        binding.labelEt.showKeyboard()
    }

    private fun handleMemo(address: Address? = null) {
        if (memoEnabled) {
            binding.tagEt.isEnabled = memoEnabled
            binding.tagRl.isVisible = memoEnabled
            binding.tagEt.setText(address?.tag ?: "")
            binding.tagIv.isVisible = memoEnabled
            binding.tagIv.setOnClickListener { handleClick(false) }
            binding.info.setOnClickListener {
                memoEnabled = false
                updateSaveButton()
                handleMemo()
            }
            binding.info.setText(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    R.string.withdrawal_addr_tag
                } else {
                    R.string.withdrawal_addr_memo
                }
            )
            binding.info.highLight(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    getString(R.string.No_tag)
                } else {
                    getString(R.string.withdrawal_no_memo)
                }
            )
        } else {
            binding.tagEt.isEnabled = memoEnabled
            binding.tagRl.isVisible = memoEnabled
            binding.tagEt.setText(R.string.No_Memo)
            binding.tagIv.isVisible = memoEnabled
            binding.info.setOnClickListener {
                memoEnabled = true
                updateSaveButton()
                handleMemo()
                binding.tagEt.showKeyboard()
            }
            binding.info.setText(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    R.string.withdrawal_addr_no_tag
                } else {
                    R.string.withdrawal_addr_no_memo
                }
            )
            binding.info.highLight(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    getString(R.string.Add_tag)
                } else {
                    getString(R.string.Add_memo)
                }
            )
        }
    }

    private fun handleClick(isAddr: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    if (isAddr) {
                        getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                    } else {
                        getScanMemoResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                    }
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun callbackScanMemo(data: Intent?) {
        callbackScan(data, false)
    }

    private fun callbackScan(data: Intent?, isAddr: Boolean = true) {
        val text = data?.getStringExtra(ARGS_FOR_SCAN_RESULT)
        if (text != null) {
            if (isAddr) {
                if (isIcapAddress(text)) {
                    binding.addrEt.setText(decodeICAP(text))
                } else {
                    binding.addrEt.setText(text)
                }
            } else {
                binding.tagEt.setText(text)
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (viewDestroyed()) return
            updateSaveButton()
        }
    }

    private fun updateSaveButton() {
        if (binding.addrEt.text.isNotEmpty() && binding.labelEt.text.isNotEmpty() && (!memoEnabled || binding.tagEt.text.isNotEmpty())) {
            binding.saveTv.isEnabled = true
            binding.saveTv.textColor = requireContext().getColor(R.color.white)
        } else {
            binding.saveTv.isEnabled = false
            binding.saveTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
        }
    }
}
