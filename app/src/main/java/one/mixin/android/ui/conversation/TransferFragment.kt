package one.mixin.android.ui.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentTransferBinding
import one.mixin.android.databinding.ItemTransferTypeBinding
import one.mixin.android.databinding.ViewWalletTransferTypeBottomBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.checkNumber
import one.mixin.android.extension.clearCharacterStyle
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.equalsIgnoreCase
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
import one.mixin.android.extension.sp
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.address.AddressAddFragment.Companion.ARGS_ADDRESS
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment.Companion.FROM_TRANSFER
import one.mixin.android.ui.conversation.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransferOutViewFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.displayAddress
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import one.mixin.android.widget.getMaxCustomViewHeight
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("InflateParams")
class TransferFragment() : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSET_PREFERENCE = "TRANSFER_ASSET"
        const val ARGS_SWITCH_ASSET = "args_switch_asset"

        const val POST_TEXT = 0
        const val POST_PB = 1

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

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun newInstance(testRegistry: ActivityResultRegistry) = TransferFragment(testRegistry)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isAdded) {
            operateKeyboard(false)
        }
        super.onDismiss(dialog)
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val chatViewModel by viewModels<ConversationViewModel>()

    private var assets = listOf<AssetItem>()
    private var currentAsset: AssetItem? = null
        set(value) {
            field = value
            adapter.currentAsset = value
            activity?.defaultSharedPreferences!!.putString(ASSET_PREFERENCE, value?.assetId)
        }

    private val adapter = TypeAdapter()

    private val userId: String? by lazy { requireArguments().getString(ARGS_USER_ID) }
    private var address: Address? = null
    private val supportSwitchAsset by lazy { requireArguments().getBoolean(ARGS_SWITCH_ASSET) }

    private var user: User? = null

    private var swapped = false
    private var bottomValue = 0.0

    private var transferBottomOpened = false

    private val assetsViewBinding: ViewWalletTransferTypeBottomBinding by lazy {
        val viewBinding = ViewWalletTransferTypeBottomBinding.inflate(LayoutInflater.from(context), null, false)
        viewBinding.typeRv.adapter = adapter
        context?.let { c ->
            val topOffset = c.appCompatActionBarHeight()
            viewBinding.root.heightOffset = topOffset
        }
        viewBinding
    }

    private val assetsBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity(), needFocus = true, softInputResize = false)
        val bottomSheet = builder.create()
        builder.setCustomView(assetsViewBinding.root)
        bottomSheet.setOnDismissListener {
            if (isAdded) {
                assetsViewBinding.searchEt.et.text?.clear()
                operateKeyboard(true)
            }
        }
        bottomSheet
    }

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

    private fun callbackScan(data: Intent?) {
        val memo = data?.getStringExtra(ARGS_FOR_SCAN_RESULT)
        binding.transferMemo.setText(memo)
    }

    private val binding by viewBinding(FragmentTransferBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        jobManager.addJobInBackground(RefreshAssetsJob())
        binding.titleView.leftIb.setOnClickListener { dismiss() }
        binding.amountEt.addTextChangedListener(mWatcher)
        binding.amountEt.filters = arrayOf(inputFilter)
        binding.amountEt.setAdapter(autoCompleteAdapter)
        binding.amountRl.setOnClickListener { operateKeyboard(true) }
        binding.swapIv.setOnClickListener {
            currentAsset?.let {
                swapped = !swapped
                updateAssetUI(it)
            }
        }
        binding.memoIv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.CAMERA)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
        assetsViewBinding.searchEt.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }

            override fun onSearch() {
            }
        }

        binding.titleView.rightAnimator.isVisible = true
        binding.titleView.rightIb.setImageResource(R.drawable.ic_transaction)
        if (isInnerTransfer()) {
            handleInnerTransfer()
        } else {
            handleAddressTransfer()
        }
        binding.titleView.rightIb.setOnClickListener {
            currentAsset?.let { asset ->
                TransferOutViewFragment.newInstance(asset.assetId, userId, user?.avatarUrl, asset.symbol, address)
                    .show(parentFragmentManager, TransferOutViewFragment.TAG)
            }
        }

        binding.continueTv.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            operateKeyboard(false)
            prepareTransferBottom()
        }
    }

    private fun handleAddressTransfer() {
        binding.avatar.setNet(requireContext().dpToPx(16f))
        binding.expandIv.isVisible = false
        binding.assetRl.setOnClickListener(null)
        currentAsset = requireArguments().getParcelable(ARGS_ASSET)
        currentAsset?.let { updateAssetUI(it) }

        address = requireArguments().getParcelable(ARGS_ADDRESS)
        if (address == null || currentAsset == null) return

        chatViewModel.observeAddress(address!!.addressId).observe(
            this
        ) {
            address = it
            binding.titleView.setSubTitle(
                getString(R.string.send_to, it.label),
                it.displayAddress().formatPublicKey()
            )
            binding.memoRl.isVisible = isInnerTransfer()

            binding.feeTv.visibility = VISIBLE
            val reserveDouble = it.reserve.toDoubleOrNull()
            val dustDouble = it.dust?.toDoubleOrNull()
            val color = requireContext().colorFromAttribute(R.attr.text_primary)

            val networkSpan =
                SpannableStringBuilder(getString(R.string.withdrawal_network_fee)).apply {
                    bold {
                        append(' ')
                        color(color) {
                            append(it.fee + " " + currentAsset!!.chainSymbol)
                        }
                    }
                }
            val dustSpan = if (dustDouble != null && dustDouble > 0) {
                SpannableStringBuilder().apply {
                    append(getString(R.string.withdrawal_minimum_withdrawal))
                    color(color) {
                        bold {
                            append(" ${it.dust} ${currentAsset!!.symbol}")
                        }
                    }
                }
            } else SpannableStringBuilder()
            val reserveSpan = if (reserveDouble != null && reserveDouble > 0) {
                SpannableStringBuilder().apply {
                    append(getString(R.string.withdrawal_minimum_reserve))
                    color(color) {
                        bold {
                            append(" ${it.reserve} ${currentAsset!!.symbol}")
                        }
                    }
                }
            } else SpannableStringBuilder()
            binding.feeTv.text =
                buildBulletLines(requireContext(), networkSpan, dustSpan, reserveSpan)
        }
    }

    private fun handleInnerTransfer() {
        if (supportSwitchAsset) {
            binding.assetRl.setOnClickListener {
                operateKeyboard(false)
                context?.let {
                    if (assets.isNullOrEmpty()) {
                        assetsViewBinding.emptyTv.isVisible = true
                        assetsViewBinding.typeRv.isVisible = false
                    } else {
                        assetsViewBinding.emptyTv.isVisible = false
                        assetsViewBinding.typeRv.isVisible = true
                    }
                    adapter.submitList(assets)
                    adapter.setTypeListener(
                        object : OnTypeClickListener {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onTypeClick(asset: AssetItem) {
                                currentAsset = asset
                                updateAssetUI(asset)
                                adapter.notifyDataSetChanged()
                                assetsBottomSheet.dismiss()
                            }
                        }
                    )

                    assetsViewBinding.closeIv.setOnClickListener {
                        assetsBottomSheet.dismiss()
                    }
                    assetsBottomSheet.show()
                    assetsViewBinding.searchEt.remainFocusable()
                }

                assetsBottomSheet.setCustomViewHeight(assetsBottomSheet.getMaxCustomViewHeight())
            }
        } else {
            binding.expandIv.isVisible = false
        }
        chatViewModel.findUserById(userId!!).observe(
            this
        ) { u ->
            if (u == null) {
                jobManager.addJobInBackground(RefreshUserJob(listOf(userId!!)))
            } else {
                user = u
                binding.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                binding.titleView.setSubTitle(
                    getString(R.string.send_to, u.fullName),
                    u.identityNumber
                )
            }
        }

        chatViewModel.assetItemsWithBalance().observe(
            this,
            Observer { r: List<AssetItem>? ->
                if (transferBottomOpened) return@Observer

                if (r != null && r.isNotEmpty()) {
                    assets = r
                    adapter.submitList(r)

                    r.find {
                        it.assetId == activity?.defaultSharedPreferences!!.getString(ASSET_PREFERENCE, "")
                    }.notNullWithElse(
                        { a ->
                            updateAssetUI(a)
                            currentAsset = a
                        },
                        {
                            val a = assets[0]
                            updateAssetUI(a)
                            currentAsset = a
                        }
                    )
                }
            }
        )
    }

    private fun filter(s: String) {
        val assetList = assets.filter {
            it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
        }.sortedByDescending { it.name.equalsIgnoreCase(s) || it.symbol.equalsIgnoreCase(s) }
        adapter.submitList(assetList)
    }

    @SuppressLint("SetTextI18n")
    private fun updateAssetUI(asset: AssetItem) {
        val price = asset.priceUsd.toFloatOrNull()
        val valuable = if (price == null) false else price > 0f
        if (valuable) {
            binding.swapIv.visibility = VISIBLE
        } else {
            swapped = false
            binding.swapIv.visibility = GONE
        }
        checkInputForbidden(binding.amountEt.text.toString())
        if (binding.amountEt.text.isNullOrEmpty()) {
            if (swapped) {
                binding.amountEt.hint = "0.00 ${Fiats.getAccountCurrencyAppearance()}"
                binding.amountAsTv.text = "0.00 ${asset.symbol}"
            } else {
                binding.amountEt.hint = "0.00 ${asset.symbol}"
                binding.amountAsTv.text = "0.00 ${Fiats.getAccountCurrencyAppearance()}"
            }
            binding.symbolTv.text = ""
        } else {
            binding.amountEt.hint = ""
            binding.symbolTv.text = getTopSymbol()
            binding.amountAsTv.text = getBottomText()
        }

        if (!isInnerTransfer() && asset.assetId == RIPPLE_CHAIN_ID) {
            binding.transferMemo.setHint(R.string.Tag)
        } else {
            binding.transferMemo.setHint(R.string.Memo)
        }
        binding.assetName.text = asset.name
        binding.assetDesc.text = asset.balance.numberFormat()
        binding.descEnd.text = asset.symbol
        binding.assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        operateKeyboard(true)
        updateAssetAutoComplete(asset)
    }

    private fun isInnerTransfer() = userId != null
    private val autoCompleteAdapter by lazy {
        ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            mutableListOf("")
        )
    }

    private fun updateAssetAutoComplete(asset: AssetItem) {
        binding.amountEt.dropDownWidth = measureText(asset.balance) + 24.dp
        autoCompleteAdapter.clear()
        autoCompleteAdapter.add(asset.balance)
    }

    private fun getAmountView() = binding.amountEt
    private val paint by lazy {
        TextPaint().apply {
            typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_regular)
            textSize = 14.sp.toFloat()
        }
    }

    private fun measureText(text: String): Int {
        return paint.measureText(text).toInt()
    }

    private fun getAmount(): String {
        return if (swapped) {
            bottomValue.toString()
        } else {
            val s = binding.amountEt.text.toString()
            val symbol = if (swapped) currentAsset?.symbol ?: "" else Fiats.getAccountCurrencyAppearance()
            val index = s.indexOf(symbol)
            return if (index != -1) {
                s.substring(0, index)
            } else {
                s
            }.trim()
        }
    }

    private fun getTopSymbol(): String {
        return if (swapped) Fiats.getAccountCurrencyAppearance() else currentAsset?.symbol ?: ""
    }

    private fun getBottomText(): String {
        val amount = binding.amountEt.text.toString().toDoubleOrNull() ?: 0.0
        val rightSymbol = if (swapped) currentAsset!!.symbol else Fiats.getAccountCurrencyAppearance()
        val value = try {
            if (currentAsset == null || currentAsset!!.priceFiat().toDouble() == 0.0) {
                BigDecimal(0)
            } else if (swapped) {
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
        return "${if (swapped) {
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

    private fun prepareTransferBottom() = lifecycleScope.launch {
        if (currentAsset == null || (user == null && address == null)) {
            return@launch
        }
        val amount = getAmount()
        try {
            BigDecimal(amount)
        } catch (e: NumberFormatException) {
            return@launch
        }

        if (user == null) {
            val dust = address!!.dust?.toDoubleOrNull()
            val amountDouble = amount.toDoubleOrNull()
            if (dust != null && amountDouble != null && amountDouble < dust) {
                toast(getString(R.string.withdrawal_minimum_amount, address!!.dust, currentAsset!!.symbol))
                return@launch
            }
        }

        val memo = binding.transferMemo.text.toString()
        if (memo.toByteArray().size > 140) {
            toast("${binding.transferMemo.hint} ${getString(R.string.Content_too_long)}")
            return@launch
        }
        binding.continueVa.displayedChild = POST_PB
        val traceId = UUID.randomUUID().toString()
        val pair = chatViewModel.findLatestTrace(user?.userId, address?.destination, address?.tag, amount, currentAsset!!.assetId)
        if (pair.second) {
            binding.continueVa.displayedChild = POST_TEXT
            return@launch
        }

        val trace = pair.first
        val biometricItem = if (user != null) {
            TransferBiometricItem(user!!, currentAsset!!, amount, null, traceId, memo, PaymentStatus.pending.name, trace)
        } else {
            WithdrawBiometricItem(
                address!!.destination, address!!.tag, address!!.addressId, address!!.label, address!!.fee,
                currentAsset!!, amount, null, traceId, memo, PaymentStatus.pending.name, trace
            )
        }
        binding.continueVa.displayedChild = POST_TEXT

        val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, FROM_TRANSFER)
        preconditionBottom.callback = object : PreconditionBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                showTransferBottom(biometricItem)
            }

            override fun onCancel() {
            }
        }
        preconditionBottom.showNow(parentFragmentManager, PreconditionBottomSheetDialogFragment.TAG)
    }

    private fun showTransferBottom(biometricItem: BiometricItem) {
        val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
        bottom.callback = object : BiometricBottomSheetDialogFragment.Callback() {
            override fun onSuccess() {
                dialog?.dismiss()
                callback?.onSuccess()
            }
        }
        bottom.onDestroyListener = object : TransferBottomSheetDialogFragment.OnDestroyListener {
            override fun onDestroy() {
                transferBottomOpened = false
            }
        }
        bottom.show(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        transferBottomOpened = true
    }

    private val inputFilter = InputFilter { source, _, _, _, dstart, _ ->
        val dotIndex = binding.amountEt.text.indexOf('.')
        val modifyDecimal = dotIndex != -1 && dstart > dotIndex
        val s = if (forbiddenInput and !ignoreFilter and modifyDecimal) "" else source
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
            if (swapped) {
                val tailLen = tail.length
                if (tailLen > 2) {
                    ignoreFilter = true
                    binding.amountEt.setText(s.subSequence(0, num[0].length + 3))
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
            binding.amountEt.clearCharacterStyle()
            checkInputForbidden(s)
            if (s.isNotEmpty() && binding.assetRl.isEnabled && s.toString().checkNumber()) {
                binding.continueTv.isEnabled = true
                binding.continueTv.textColor = requireContext().getColor(R.color.white)
                if (binding.amountRl.isVisible && currentAsset != null) {
                    binding.amountEt.hint = ""
                    binding.symbolTv.text = getTopSymbol()
                    binding.amountAsTv.text = getBottomText()
                }
            } else {
                binding.continueTv.isEnabled = false
                binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                if (binding.amountRl.isVisible) {
                    binding.amountEt.hint = "0.00 ${if (swapped) Fiats.getAccountCurrencyAppearance() else currentAsset?.symbol}"
                    binding.symbolTv.text = ""
                    binding.amountAsTv.text = "0.00 ${if (swapped) currentAsset?.symbol else Fiats.getAccountCurrencyAppearance()}"
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
            val binding = ItemTransferTypeBinding.bind(holder.itemView)
            binding.typeAvatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            binding.typeAvatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            binding.name.text = itemAssert.name
            binding.value.text = itemAssert.balance.numberFormat()
            binding.valueEnd.text = itemAssert.symbol
            currentAsset?.let {
                binding.checkIv.visibility = if (itemAssert.assetId == currentAsset?.assetId) VISIBLE else INVISIBLE
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
