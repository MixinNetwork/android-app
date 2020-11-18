package one.mixin.android.ui.address

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_address_add.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.ChainId.BITCOIN_CHAIN_ID
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ADD
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.textColor

@AndroidEntryPoint
class AddressAddFragment() : BaseFragment() {
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

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getScanResult = registerForActivityResult(CaptureActivity.CaptureContract(), resultRegistry, ::callbackScan)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelable(ARGS_ASSET)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_address_add, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.right_animator.isEnabled = false
        title_view.left_ib.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            if (label_et.isFocused) label_et.hideKeyboard()
            if (addr_et.isFocused) addr_et.hideKeyboard()
            if (tag_et.isFocused) tag_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.title_tv.text = getString(R.string.withdrawal_addr_new, asset.symbol)
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        save_tv.setOnClickListener {
            var destination = addr_et.text.toString()
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
                    chainId = asset.chainId,
                    chainIconUrl = asset.chainIconUrl,
                    label = label_et.text.toString(),
                    destination = destination,
                    tag = if (memoEnabled) {
                        tag_et.text.toString()
                    } else {
                        ""
                    },
                    type = ADD
                )

            bottomSheet.showNow(parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
            bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    activity?.onBackPressed()
                }
            }
        }

        if (asset.assetId == RIPPLE_CHAIN_ID) {
            tag_et.setHint(R.string.withdrawal_addr_tag_hint)
        } else {
            tag_et.setHint(R.string.withdrawal_addr_memo_hint)
        }
        label_et.addTextChangedListener(mWatcher)
        addr_et.addTextChangedListener(mWatcher)
        tag_et.addTextChangedListener(mWatcher)
        addr_iv.setOnClickListener { handleClick(true) }
        handleMemo()
        label_et.showKeyboard()
    }

    private fun handleMemo(address: Address? = null) {
        if (memoEnabled) {
            tag_et.isEnabled = memoEnabled
            tag_rl.isVisible = memoEnabled
            tag_et.setText(address?.tag ?: "")
            tag_iv.isVisible = memoEnabled
            tag_iv.setOnClickListener { handleClick(false) }
            info.setOnClickListener {
                memoEnabled = false
                updateSaveButton()
                handleMemo()
            }
            info.setText(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    R.string.withdrawal_addr_tag
                } else {
                    R.string.withdrawal_addr_memo
                }
            )
            info.highLight(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    getString(R.string.withdrawal_addr_tag_link)
                } else {
                    getString(R.string.withdrawal_addr_memo_link)
                }
            )
        } else {
            tag_et.isEnabled = memoEnabled
            tag_rl.isVisible = memoEnabled
            tag_et.setText(R.string.withdrawal_no_tag)
            tag_iv.isVisible = memoEnabled
            info.setOnClickListener {
                memoEnabled = true
                updateSaveButton()
                handleMemo()
                tag_et.showKeyboard()
            }
            info.setText(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    R.string.withdrawal_addr_no_tag
                } else {
                    R.string.withdrawal_addr_no_memo
                }
            )
            info.highLight(
                if (asset.assetId == RIPPLE_CHAIN_ID) {
                    getString(R.string.withdrawal_addr_no_tag_link)
                } else {
                    getString(R.string.withdrawal_addr_no_memo_link)
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
                    this.isAddr = isAddr
                    getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    var isAddr = false
        private set

    private fun callbackScan(data: Intent?) {
        val text = data?.getStringExtra(ARGS_FOR_SCAN_RESULT)
        if (text != null) {
            if (isAddr) {
                if (isIcapAddress(text)) {
                    addr_et.setText(decodeICAP(text))
                } else {
                    addr_et.setText(text)
                }
            } else {
                tag_et.setText(text)
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (!isAdded) return
            updateSaveButton()
        }
    }

    private fun updateSaveButton() {
        if (addr_et.text.isNotEmpty() && label_et.text.isNotEmpty() && (!memoEnabled || tag_et.text.isNotEmpty())) {
            save_tv.isEnabled = true
            save_tv.textColor = requireContext().getColor(R.color.white)
        } else {
            save_tv.isEnabled = false
            save_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
        }
    }
}
