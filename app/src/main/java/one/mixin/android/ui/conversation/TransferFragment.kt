package one.mixin.android.ui.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import kotlinx.android.synthetic.main.item_transfer_type.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_transfer_type_bottom.view.*
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.Account.PREF_HAS_WITHDRAWAL_ADDRESS_SET
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.checkNumber
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.address.AddressAddFragment.Companion.ARGS_ADDRESS
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.REQUEST_CODE
import one.mixin.android.ui.qr.CaptureActivity.Companion.RESULT_CODE
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.displayAddress
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetLinearLayout
import one.mixin.android.widget.SearchView
import one.mixin.android.widget.getMaxCustomViewHeight
import one.mixin.android.worker.RefreshAssetsWorker
import org.jetbrains.anko.textColor

@SuppressLint("InflateParams")
class TransferFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSET_PREFERENCE = "TRANSFER_ASSET"
        const val ARGS_SWITCH_ASSET = "args_switch_asset"

        fun newInstance(
            userId: String? = null,
            asset: AssetItem? = null,
            address: Address? = null,
            supportSwitchAsset: Boolean = false
        ) = TransferFragment().withArgs {
            userId?.let { putString(ARGS_USER_ID, it) }
            asset?.let { putParcelable(ARGS_ASSET, it) }
            address?.let { putParcelable(ARGS_ADDRESS, it) }
            putBoolean(ARGS_SWITCH_ASSET, supportSwitchAsset)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer, null)
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isAdded) {
            operateKeyboard(false)
        }
        super.onDismiss(dialog)
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private var assets = listOf<AssetItem>()
    private var currentAsset: AssetItem? = null
        set(value) {
            field = value
            adapter.currentAsset = value
            activity?.defaultSharedPreferences!!.putString(ASSET_PREFERENCE, value?.assetId)
        }

    private val adapter = TypeAdapter()

    private val userId: String? by lazy { arguments!!.getString(ARGS_USER_ID) }
    private var address: Address? = null
    private val supportSwitchAsset by lazy { arguments!!.getBoolean(ARGS_SWITCH_ASSET) }

    private var user: User? = null

    private var swaped = false
    private var bottomValue = 0.0

    private val assetsView: View by lazy {
        val view = View.inflate(context, R.layout.view_wallet_transfer_type_bottom, null) as BottomSheetLinearLayout
        view.type_rv.adapter = adapter
        context?.let { c ->
            val topOffset = c.appCompatActionBarHeight()
            view.heightOffset = topOffset
        }
        view
    }

    private val assetsBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity(), needFocus = true, softInputResize = false)
        val bottomSheet = builder.create()
        builder.setCustomView(assetsView)
        bottomSheet.setOnDismissListener {
            if (isAdded) {
                assetsView.search_et.text?.clear()
                operateKeyboard(true)
            }
        }
        bottomSheet
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WorkManager.getInstance(requireContext()).enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.amount_et.addTextChangedListener(mWatcher)
        contentView.amount_et.filters = arrayOf(inputFilter)
        contentView.amount_rl.setOnClickListener { operateKeyboard(true) }
        contentView.swap_iv.setOnClickListener {
            currentAsset?.let {
                swaped = !swaped
                updateAssetUI(it)
            }
        }
        contentView.memo_iv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.CAMERA)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        CaptureActivity.show(requireActivity()) {
                            it.putExtra(CaptureActivity.ARGS_FOR_MEMO, true)
                            startActivityForResult(it, REQUEST_CODE)
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
        assetsView.search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }

            override fun onSearch() {
            }
        }

        if (isInnerTransfer()) {
            handleInnerTransfer()
        } else {
            handleAddressTransfer()
        }

        contentView.continue_tv.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            operateKeyboard(false)

            when {
                isInnerTransfer() -> showTransferBottom()
                shouldShowWithdrawalTip() -> {
                    currentAsset?.let {
                        val withdrawalBottom = WithdrawalTipBottomSheetDialogFragment.newInstance(it)
                        withdrawalBottom.showNow(parentFragmentManager, WithdrawalTipBottomSheetDialogFragment.TAG)
                        withdrawalBottom.callback = object : WithdrawalTipBottomSheetDialogFragment.Callback {
                            override fun onSuccess() {
                                showTransferBottom()
                            }
                        }
                    }
                }
                else -> showTransferBottom()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_CODE) {
            contentView.transfer_memo.setText(data?.getStringExtra(CaptureActivity.ARGS_MEMO_RESULT))
        }
    }

    private fun shouldShowWithdrawalTip(): Boolean {
        if (currentAsset == null && address == null) return false

        try {
            val amount = BigDecimal(getAmount()).toDouble() * currentAsset!!.priceUsd.toDouble()
            if (amount <= 10) {
                return false
            }
        } catch (e: NumberFormatException) {
            return false
        }
        val hasWithdrawalAddressSet = defaultSharedPreferences.getStringSet(PREF_HAS_WITHDRAWAL_ADDRESS_SET, null)
        return if (hasWithdrawalAddressSet == null) {
            true
        } else {
            !hasWithdrawalAddressSet.contains(address!!.addressId)
        }
    }

    private fun handleAddressTransfer() {
        contentView.avatar.setNet(requireContext().dpToPx(16f))
        contentView.expand_iv.isVisible = false
        contentView.asset_rl.setOnClickListener(null)
        currentAsset = arguments!!.getParcelable(ARGS_ASSET)
        currentAsset?.let { updateAssetUI(it) }

        address = arguments!!.getParcelable(ARGS_ADDRESS)
        if (address == null || currentAsset == null) return

        chatViewModel.observeAddress(address!!.addressId).observe(this, Observer {
            address = it
            contentView.title_view.setSubTitle(getString(R.string.send_to, it.label), it.displayAddress().formatPublicKey())
            contentView.memo_rl.isVisible = isInnerTransfer()
            val bold = it.fee + " " + currentAsset!!.chainSymbol
            val str = try {
                val reserveDouble = it.reserve.toDouble()
                if (reserveDouble > 0) {
                    getString(R.string.withdrawal_fee_with_reserve, bold, currentAsset!!.symbol, currentAsset!!.name, it.reserve, currentAsset!!.symbol)
                } else {
                    getString(R.string.withdrawal_fee, bold, currentAsset!!.name)
                }
            } catch (t: Throwable) {
                getString(R.string.withdrawal_fee, bold, currentAsset!!.name)
            }
            val ssb = SpannableStringBuilder(str)
            val start = str.indexOf(bold)
            ssb.setSpan(StyleSpan(Typeface.BOLD), start,
                start + bold.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            contentView.fee_tv?.visibility = VISIBLE
            contentView.fee_tv?.text = ssb
        })
    }

    private fun handleInnerTransfer() {
        if (supportSwitchAsset) {
            contentView.asset_rl.setOnClickListener {
                operateKeyboard(false)
                context?.let {
                    adapter.submitList(assets)
                    adapter.setTypeListener(object : OnTypeClickListener {
                        override fun onTypeClick(asset: AssetItem) {
                            currentAsset = asset
                            updateAssetUI(asset)
                            adapter.notifyDataSetChanged()
                            assetsBottomSheet.dismiss()
                        }
                    })

                    assetsView.close_iv.setOnClickListener {
                        assetsBottomSheet.dismiss()
                    }
                    assetsBottomSheet.show()
                    assetsView.search_et.remainFocusable()
                }

                assetsBottomSheet.setCustomViewHeight(assetsBottomSheet.getMaxCustomViewHeight())
            }
        } else {
            contentView.expand_iv.isVisible = false
        }
        chatViewModel.findUserById(userId!!).observe(this, Observer { u ->
            if (u == null) {
                jobManager.addJobInBackground(RefreshUserJob(listOf(userId!!)))
            } else {
                user = u
                contentView.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                contentView.title_view.setSubTitle(getString(R.string.send_to, u.fullName), u.identityNumber)
            }
        })

        chatViewModel.assetItemsWithBalance().observe(this, Observer { r: List<AssetItem>? ->
            if (r != null && r.isNotEmpty()) {
                assets = r
                adapter.submitList(r)
                contentView.asset_rl.isEnabled = true

                r.find {
                    it.assetId == activity?.defaultSharedPreferences!!.getString(ASSET_PREFERENCE, "")
                }.notNullWithElse({ a ->
                    updateAssetUI(a)
                    currentAsset = a
                }, {
                    val a = assets[0]
                    updateAssetUI(a)
                    currentAsset = a
                })
            } else {
                contentView.asset_rl.isEnabled = false
            }
        })
    }

    private fun filter(s: String) {
        val assetList = assets.filter {
            it.name.contains(s, true) || it.symbol.contains(s, true)
        }.sortedByDescending { it.name == s || it.symbol == s }
        adapter.submitList(assetList)
    }

    @SuppressLint("SetTextI18n")
    private fun updateAssetUI(asset: AssetItem) {
        val valuable = try {
            asset.priceUsd.toFloat() > 0f
        } catch (e: NumberFormatException) {
            false
        }
        if (valuable) {
            contentView.swap_iv.visibility = VISIBLE
        } else {
            swaped = false
            contentView.swap_iv.visibility = GONE
        }
        checkInputForbidden(contentView.amount_et.text.toString())
        if (contentView.amount_et.text.isNullOrEmpty()) {
            if (swaped) {
                contentView.amount_et.hint = "0.00 ${Session.getFiatCurrency()}"
                contentView.amount_as_tv.text = "0.00 ${asset.symbol}"
            } else {
                contentView.amount_et.hint = "0.00 ${asset.symbol}"
                contentView.amount_as_tv.text = "0.00 ${Session.getFiatCurrency()}"
            }
            contentView.symbol_tv.text = ""
        } else {
            contentView.amount_et.hint = ""
            contentView.symbol_tv.text = getTopSymbol()
            contentView.amount_as_tv.text = getBottomText()
        }

        if (!isInnerTransfer() && asset.assetId == RIPPLE_CHAIN_ID) {
            contentView.transfer_memo.setHint(R.string.wallet_transfer_tag)
        } else {
            contentView.transfer_memo.setHint(R.string.wallet_transfer_memo)
        }
        contentView.asset_name.text = asset.name
        contentView.asset_desc.text = asset.balance.numberFormat()
        contentView.desc_end.text = asset.symbol
        contentView.asset_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        operateKeyboard(true)
    }

    private fun isInnerTransfer() = userId != null

    private fun getAmountView() = contentView.amount_et

    private fun getAmount(): String {
        return if (swaped) {
            bottomValue.toString()
        } else {
            val s = contentView.amount_et.text.toString()
            val symbol = if (swaped) currentAsset?.symbol ?: "" else Session.getFiatCurrency()
            val index = s.indexOf(symbol)
            return if (index != -1) {
                s.substring(0, index)
            } else {
                s
            }.trim()
        }
    }

    private fun getTopSymbol(): String {
        return if (swaped) Session.getFiatCurrency() else currentAsset?.symbol ?: ""
    }

    private fun getBottomText(): String {
        val amount = try {
            contentView.amount_et.text.toString().toDouble()
        } catch (e: java.lang.NumberFormatException) {
            0.0
        }
        val rightSymbol = if (swaped) currentAsset!!.symbol else Session.getFiatCurrency()
        val value = try {
            if (currentAsset == null || currentAsset!!.priceFiat().toDouble() == 0.0) {
                BigDecimal(0)
            } else if (swaped) {
                BigDecimal(amount).divide(currentAsset!!.priceFiat(), 8, RoundingMode.HALF_UP)
            } else {
                BigDecimal(amount) * currentAsset!!.priceFiat()
            }
        } catch (e: ArithmeticException) {
            BigDecimal(0)
        } catch (e: NumberFormatException) {
            BigDecimal(0)
        }
        bottomValue = value.toDouble()
        return "${if (swaped) {
            value.numberFormat8()
        } else value.numberFormat2()} $rightSymbol"
    }

    private fun operateKeyboard(show: Boolean) {
        val target = getAmountView()
        target.post {
            if (show) {
                target.showKeyboard()
            } else {
                target.hideKeyboard()
            }
        }
    }

    private fun showTransferBottom() {
        if (currentAsset == null || (user == null && address == null)) {
            return
        }
        val amount = getAmount()
        try {
            BigDecimal(amount)
        } catch (e: NumberFormatException) {
            return
        }

        val memo = contentView.transfer_memo.text.toString()
        if (memo.toByteArray().size > 140) {
            toast("${contentView.transfer_memo.hint} ${getString(R.string.group_edit_too_long)}")
            return
        }

        val biometricItem = if (user != null) {
            TransferBiometricItem(user!!, currentAsset!!, amount, null, UUID.randomUUID().toString(), memo, PaymentStatus.pending.name)
        } else {
            WithdrawBiometricItem(address!!.displayAddress(), address!!.addressId,
                address!!.label, currentAsset!!, amount, null, UUID.randomUUID().toString(), memo, "")
        }

        val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
        bottom.callback = object : BiometricBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                dialog?.dismiss()
                callback?.onSuccess()
            }
        }
        bottom.showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
    }

    private val inputFilter = InputFilter { source, _, _, _, _, _ ->
        val s = if (forbiddenInput and !ignoreFilter) "" else source
        ignoreFilter = false
        return@InputFilter s
    }

    private var forbiddenInput = false
    private var ignoreFilter = false

    private fun checkInputForbidden(s: CharSequence) {
        val index = s.indexOf('.')
        forbiddenInput = if (index == -1) {
            false
        } else {
            val num = s.split('.')
            val tail = num[1]
            if (swaped) {
                val tailLen = tail.length
                if (tailLen > 2) {
                    ignoreFilter = true
                    contentView.amount_et.setText(s.subSequence(0, num[0].length + 3))
                }
                tailLen >= 2
            } else {
                tail.length >= 8
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        @SuppressLint("SetTextI18n")
        override fun afterTextChanged(s: Editable) {
            checkInputForbidden(s)
            if (s.isNotEmpty() && contentView.asset_rl.isEnabled && s.toString().checkNumber()) {
                contentView.continue_tv.isEnabled = true
                contentView.continue_tv.textColor = requireContext().getColor(R.color.white)
                if (contentView.amount_rl.isVisible && currentAsset != null) {
                    contentView.amount_et.hint = ""
                    contentView.symbol_tv.text = getTopSymbol()
                    contentView.amount_as_tv.text = getBottomText()
                }
            } else {
                contentView.continue_tv.isEnabled = false
                contentView.continue_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                if (contentView.amount_rl.isVisible) {
                    contentView.amount_et.hint = "0.00 ${if (swaped) Session.getFiatCurrency() else currentAsset?.symbol}"
                    contentView.symbol_tv.text = ""
                    contentView.amount_as_tv.text = "0.00 ${if (swaped) currentAsset?.symbol else Session.getFiatCurrency()}"
                }
            }
        }
    }

    class TypeAdapter : ListAdapter<AssetItem, ItemHolder>(AssetItem.DIFF_CALLBACK) {
        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transfer_type, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val itemAssert = getItem(position)
            holder.itemView.type_avatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.type_avatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.name.text = itemAssert.name
            holder.itemView.value.text = itemAssert.balance.numberFormat()
            holder.itemView.value_end.text = itemAssert.symbol
            currentAsset?.let {
                holder.itemView.check_iv.visibility = if (itemAssert.assetId == currentAsset?.assetId) VISIBLE else INVISIBLE
            }
            holder.itemView.setOnClickListener {
                typeListener?.onTypeClick(itemAssert)
            }
        }

        fun setTypeListener(listener: OnTypeClickListener) {
            typeListener = listener
        }

        var currentAsset: AssetItem? = null
    }

    interface OnTypeClickListener {
        fun onTypeClick(asset: AssetItem)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}
