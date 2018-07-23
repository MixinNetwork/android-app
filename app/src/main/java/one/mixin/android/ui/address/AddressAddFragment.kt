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
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_address_add.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ADD
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.ARGS_TYPE
import one.mixin.android.ui.wallet.TransactionsFragment
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
        const val ARGS_FROM_MANAGEMENT = "args_from_management"

        fun newInstance(asset: AssetItem, address: Address? = null, type: Int = ADD, fromManagement: Boolean) = AddressAddFragment().apply {
            val b = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
                address?.let { putParcelable(ARGS_ADDRESS, it) }
                putInt(ARGS_TYPE, type)
                putBoolean(ARGS_FROM_MANAGEMENT, fromManagement)
            }
            arguments = b
        }
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(TransactionsFragment.ARGS_ASSET)
    }
    private val address: Address? by lazy {
        arguments!!.getParcelable<Address?>(ARGS_ADDRESS)
    }
    private val fromManagement by lazy { arguments!!.getBoolean(ARGS_FROM_MANAGEMENT) }

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
        title_view.right_animator.setOnClickListener {
            val bottomSheet = if (noPublickKey()) {
                PinAddrBottomSheetDialogFragment.newInstance(assetId = asset.assetId, type = type,
                    accountName = label_et.text.toString(), accountTag = addr_et.text.toString())
            } else {
                PinAddrBottomSheetDialogFragment.newInstance(asset.assetId,
                    label_et.text.toString(), addr_et.text.toString(), type = type)
            }
            bottomSheet.showNow(requireFragmentManager(), PinAddrBottomSheetDialogFragment.TAG)
            bottomSheet.setCallback(object : PinAddrBottomSheetDialogFragment.Callback {
                override fun onSuccess() {
                    if (fromManagement) {
                        fragmentManager?.popBackStackImmediate()
                    } else {
                        activity?.onBackPressed()
                    }
                }
            })
        }
        if (!asset.accountName.isNullOrEmpty()) {
            label_et.hint = getString(R.string.account_name)
            addr_et.hint = getString(R.string.account_memo)
        }
        label_et.addTextChangedListener(mWatcher)
        addr_et.addTextChangedListener(mWatcher)
        qr_iv.setOnClickListener { handleClick(true) }
        label_iv.setOnClickListener { handleClick(false) }

        address?.let {
            label_et.setText(if (noPublickKey()) it.accountName else it.label)
            addr_et.setText(if (noPublickKey()) it.accountTag else it.publicKey)
            title_view.title_tv.text = getString(R.string.withdrawal_addr_modify, asset.symbol)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == CaptureFragment.RESULT_CODE) {
            val addr = data?.getStringExtra(CaptureFragment.ARGS_ADDRESS_RESULT)
            if (addr != null) {
                if (isIcapAddress(addr)) {
                    addr_et.setText(decodeICAP(addr))
                } else {
                    addr_et.setText(addr)
                }
                return
            }
            val label = data?.getStringExtra(CaptureFragment.ARGS_ACCOUNT_NAME_RESULT) ?: return
            label_et.setText(label)
        }
    }

    private fun noPublickKey() = !asset.accountName.isNullOrEmpty()

    @SuppressLint("CheckResult")
    private fun handleClick(isAddr: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) {
                    val intentIntegrator = IntentIntegrator(activity)
                    intentIntegrator.captureActivity = CaptureActivity::class.java
                    intentIntegrator.setBeepEnabled(false)
                    val intent = intentIntegrator.createScanIntent()
                        .putExtra(if (isAddr) CaptureFragment.ARGS_FOR_ADDRESS else CaptureFragment.ARGS_FOR_ACCOUNT_NAME, true)
                    startActivityForResult(intent, REQUEST_CODE)
                    activity?.overridePendingTransition(R.anim.slide_in_bottom, 0)
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
                title_view.right_tv.textColor = resources.getColor(R.color.colorBlue, null)
                title_view.right_animator.isEnabled = true
            } else {
                title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
                title_view.right_animator.isEnabled = false
            }
        }
    }
}