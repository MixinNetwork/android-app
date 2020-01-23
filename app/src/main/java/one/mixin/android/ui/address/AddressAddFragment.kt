package one.mixin.android.ui.address

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_address_add.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
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
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_ACCOUNT_NAME_RESULT
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_ADDRESS_RESULT
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ACCOUNT_NAME
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ADDRESS
import one.mixin.android.ui.qr.CaptureActivity.Companion.REQUEST_CODE
import one.mixin.android.ui.qr.CaptureActivity.Companion.RESULT_CODE
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ADD
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ARGS_TYPE
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import org.jetbrains.anko.textColor

class AddressAddFragment : BaseFragment() {

    companion object {
        const val TAG = "AddressAddFragment"

        const val ARGS_ADDRESS = "args_address"

        fun newInstance(
            asset: AssetItem,
            address: Address? = null,
            type: Int = ADD
        ) = AddressAddFragment().apply {
            val b = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
                address?.let { putParcelable(ARGS_ADDRESS, it) }
                putInt(ARGS_TYPE, type)
            }
            arguments = b
        }
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)!!
    }
    private val address: Address? by lazy {
        arguments!!.getParcelable<Address?>(ARGS_ADDRESS)
    }

    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }
    private var memoEnabled = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_address_add, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_animator.isEnabled = false
        title_view.left_ib.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            if (label_et.isFocused) label_et.hideKeyboard()
            if (addr_et.isFocused) addr_et.hideKeyboard()
            if (tag_et.isFocused) tag_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.title_tv.text = getString(
            if (type == ADD) R.string.withdrawal_addr_new
            else R.string.withdrawal_addr_modify, asset.symbol
        )
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        save_tv.setOnClickListener {
            val destination = addr_et.text.toString().let {
                if (it.startsWith("http://", true) || it.startsWith("https://", true)) {
                    it
                } else {
                    it.substringAfter(":")
                }
            }
            val bottomSheet =
                PinAddrBottomSheetDialogFragment.newInstance(
                    asset.assetId,
                    asset.name,
                    assetUrl = asset.iconUrl,
                    chainIconUrl = asset.chainIconUrl,
                    label = label_et.text.toString(),
                    destination = destination,
                    tag = if (memoEnabled) {
                        tag_et.text.toString()
                    } else {
                        ""
                    },
                    type = type
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
        handleMemo(address)
        address?.let {
            label_et.setText(it.label)
            addr_et.setText(it.destination)
            title_view.title_tv.text = getString(R.string.withdrawal_addr_modify, asset.symbol)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_CODE) {
            val addr = data?.getStringExtra(ARGS_ADDRESS_RESULT)
            if (addr != null) {
                if (isIcapAddress(addr)) {
                    addr_et.setText(decodeICAP(addr))
                } else {
                    addr_et.setText(addr)
                }
                return
            }
            val tag = data?.getStringExtra(ARGS_ACCOUNT_NAME_RESULT) ?: return
            tag_et.setText(tag)
        }
    }

    private fun handleClick(isAddr: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    CaptureActivity.show(requireActivity()) {
                        it.putExtra(if (isAddr) ARGS_FOR_ADDRESS else ARGS_FOR_ACCOUNT_NAME, true)
                        startActivityForResult(it, REQUEST_CODE)
                    }
                } else {
                    context?.openPermissionSetting()
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
