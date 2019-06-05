package one.mixin.android.ui.address

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_address_add.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
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
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }
    private val address: Address? by lazy {
        arguments!!.getParcelable<Address?>(ARGS_ADDRESS)
    }

    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_address_add, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_animator.isEnabled = false
        title_view.left_ib.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            if (label_et.isFocused) label_et.hideKeyboard()
            if (addr_et.isFocused) addr_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.title_tv.text = getString(if (type == ADD) R.string.withdrawal_addr_new
        else R.string.withdrawal_addr_modify, asset.symbol)
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        save_tv.setOnClickListener {
            val bottomSheet = if (noPublicKey()) {
                PinAddrBottomSheetDialogFragment.newInstance(assetId = asset.assetId, assetName = asset.name,
                    assetUrl = asset.iconUrl, chainIconUrl = asset.chainIconUrl, type = type,
                    accountName = label_et.text.toString(), accountTag = addr_et.text.toString())
            } else {
                PinAddrBottomSheetDialogFragment.newInstance(asset.assetId, asset.name,
                    assetUrl = asset.iconUrl,
                    chainIconUrl = asset.chainIconUrl,
                    label = label_et.text.toString(), publicKey = addr_et.text.toString(), type = type)
            }
            bottomSheet.showNow(requireFragmentManager(), PinAddrBottomSheetDialogFragment.TAG)
            bottomSheet.callback = object : PinBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    activity?.onBackPressed()
                }
            }
        }
        if (!asset.accountName.isNullOrEmpty()) {
            label_et.hint = getString(R.string.account_name)
            addr_et.hint = getString(R.string.account_memo)
        }
        label_et.addTextChangedListener(mWatcher)
        addr_et.addTextChangedListener(mWatcher)
        qr_iv.setOnClickListener { handleClick(true) }
        label_iv.setOnClickListener { handleClick(false) }
        label_iv.isVisible = noPublicKey()

        address?.let {
            label_et.setText(if (noPublicKey()) it.accountName else it.label)
            addr_et.setText(if (noPublicKey()) it.accountTag else it.publicKey)
            title_view.title_tv.text = getString(R.string.withdrawal_addr_modify, asset.symbol)
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
            val label = data?.getStringExtra(ARGS_ACCOUNT_NAME_RESULT) ?: return
            label_et.setText(label)
        }
    }

    private fun noPublicKey() = !asset.accountName.isNullOrEmpty()

    @SuppressLint("CheckResult", "AutoDispose")
    private fun handleClick(isAddr: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
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

            if (addr_et.text.isNotEmpty() && label_et.text.isNotEmpty()) {
                save_tv.isEnabled = true
                save_tv.textColor = requireContext().getColor(R.color.white)
            } else {
                save_tv.isEnabled = false
                save_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
            }
        }
    }
}