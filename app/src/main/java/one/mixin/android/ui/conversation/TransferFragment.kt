package one.mixin.android.ui.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.InputFilter
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
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentTransferBinding
import one.mixin.android.databinding.ItemTransferTypeBinding
import one.mixin.android.extension.checkNumber
import one.mixin.android.extension.clearCharacterStyle
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.sp
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.address.AddressAddFragment.Companion.ARGS_ADDRESS
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.OutputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment.Companion.FROM_TRANSFER
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_TRANSFER
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.ui.wallet.NetworkFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransferOutViewFragment
import one.mixin.android.ui.wallet.WithdrawalSuspendedBottomSheet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.displayAddress
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@UnstableApi @AndroidEntryPoint
@SuppressLint("InflateParams")
class TransferFragment() : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSET_PREFERENCE = "TRANSFER_ASSET"
        const val ARGS_SWITCH_ASSET = "args_switch_asset"
        const val ARGS_MAINNET_ADDRSS = "args_mainnet_address"
        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_MEMO = "args_memo"
        const val ARGS_TRACE = "args_trace"
        const val ARGS_RETURN_TO = "args_return_to"

        const val POST_TEXT = 0
        const val POST_PB = 1

        fun newInstance(
            userId: String? = null,
            asset: TokenItem? = null,
            address: Address? = null,
            mainnetAddress: String? = null,
            amount: String? = null,
            memo: String? = null,
            trace: String? = null,
            returnTo: String? = null,
            supportSwitchAsset: Boolean = false,
        ) = TransferFragment().withArgs {
            userId?.let { putString(ARGS_USER_ID, it) }
            asset?.let { putParcelable(ARGS_ASSET, it) }
            address?.let { putParcelable(ARGS_ADDRESS, it) }
            mainnetAddress?.let { putString(ARGS_MAINNET_ADDRSS, it) }
            amount?.let { putString(ARGS_AMOUNT, it) }
            memo?.let { putString(ARGS_MEMO, it) }
            trace?.let { putString(ARGS_TRACE, it) }
            returnTo?.let { putString(ARGS_RETURN_TO, it) }
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

    private var assets = listOf<TokenItem>()
    private var currentAsset: TokenItem? = null
        set(value) {
            field = value
            activity?.defaultSharedPreferences!!.putString(ASSET_PREFERENCE, value?.assetId)
        }

    private val userId: String? by lazy { requireArguments().getString(ARGS_USER_ID) }
    private var address: Address? = null
    private val mainnetAddress: String? by lazy { requireArguments().getString(ARGS_MAINNET_ADDRSS) }
    private val supportSwitchAsset by lazy { requireArguments().getBoolean(ARGS_SWITCH_ASSET) }

    private val fees: ArrayList<NetworkFee> = arrayListOf()
    private var currentFee: NetworkFee? = null

    private var user: User? = null

    private var swapped = false
    private var bottomValue = BigDecimal.ZERO

    private var transferBottomOpened = false

    private fun selectAsset() {
        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_TRANSFER)
            .setOnAssetClick { asset ->
                currentAsset = asset
                updateAssetUI(asset)
            }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
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
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.titleView.leftIb.setOnClickListener {
            contentView.hideKeyboard()
            dismiss()
        }
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

        binding.titleView.rightAnimator.isVisible = true
        binding.titleView.rightIb.setImageResource(R.drawable.ic_transaction)
        address = requireArguments().getParcelableCompat(ARGS_ADDRESS, Address::class.java)
        if (isInnerTransfer()) {
            handleInnerTransfer()
        } else {
            handleAddressTransfer()
        }
        if (mainnetAddress == null) {
            binding.titleView.rightIb.setOnClickListener {
                currentAsset?.let { asset ->
                    TransferOutViewFragment.newInstance(asset.assetId, userId, user?.avatarUrl, asset.symbol, address)
                        .show(parentFragmentManager, TransferOutViewFragment.TAG)
                }
            }
        } else {
            binding.titleView.rightIb.isVisible = false
        }

        binding.continueTv.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            operateKeyboard(false)
            prepareTransferBottom()
        }
        requireArguments().getString(ARGS_AMOUNT)?.let {
            binding.amountEt.setText(it)
            binding.amountEt.isEnabled = false
        }
        requireArguments().getString(ARGS_MEMO)?.let {
            binding.transferMemo.setText(it)
            binding.transferMemo.isEnabled = false
            binding.memoIv.isEnabled = false
        }
    }

    private fun handleAddressTransfer() {
        binding.avatar.isVisible = false
        binding.expandIv.isVisible = false
        binding.assetRl.setOnClickListener(null)
        val currentAsset = requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)
        this.currentAsset = currentAsset
        currentAsset?.let { updateAssetUI(it) }

        val address = this.address
        if (address == null || currentAsset == null) return

        if (address.addressId.isBlank()) { // mock address
            binding.titleView.setSubTitle(
                getString(R.string.send_to, address.label),
                address.displayAddress().formatPublicKey(),
            )
            updateFeeUI(currentAsset, address)
        } else {
            chatViewModel.observeAddress(address.addressId).observe(
                this,
            ) {
                this.address = it
                binding.titleView.setSubTitle(
                    getString(R.string.send_to, it.label),
                    it.displayAddress().formatPublicKey(),
                )
                updateFeeUI(currentAsset, it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFeeUI(
        token: TokenItem,
        address: Address,
    ) =
        lifecycleScope.launch {
            if (address.feeAssetId.isBlank()) {
                binding.apply {
                    memoRl.isVisible = false
                    networkHtv.isVisible = false
                    dustHtv.isVisible = false
                    reserveHtv.isVisible = false
                    feeHtv.isVisible = false
                    continueVa.displayedChild = POST_PB
                    continueVa.setBackgroundResource(R.drawable.selector_round_bn_gray)
                }
            } else {
                binding.continueVa.displayedChild = POST_PB
                val feeAsset = chatViewModel.refreshAsset(address.feeAssetId)
                if (feeAsset == null) {
                    jobManager.addJobInBackground(RefreshTokensJob(address.feeAssetId))
                    return@launch
                }
                val reserveDouble = address.reserve.toBigDecimalOrNull()
                val dustDouble = address.dust?.toBigDecimalOrNull()
                binding.apply {
                    memoRl.isVisible = isInnerTransfer()
                    networkHtv.isVisible = true
                    feeHtv.isVisible = true
                    continueVa.setBackgroundResource(R.drawable.bg_round_blue_btn)
                    networkHtv.tail.text = getChainName(token.chainId, token.chainName, token.assetKey)
                    if (dustDouble != null && dustDouble != BigDecimal.ZERO) {
                        dustHtv.isVisible = true
                        dustHtv.tail.text = "${address.dust} ${token.symbol}"
                    }
                    if (reserveDouble != null && reserveDouble != BigDecimal.ZERO) {
                        reserveHtv.isVisible = true
                        reserveHtv.tail.text = "${address.reserve} ${token.symbol}"
                    }
                }
                var success = refreshFees(token, address)
                while (!success) {
                    success = refreshFees(token, address)
                    delay(500)
                }
                updateFeeUI()
            }
        }

    @SuppressLint("SetTextI18n")
    private fun updateFeeUI() {
        currentFee?.let { fee ->
            binding.apply {
                if (fee.fee.toDouble() == 0.0) {
                    feeHtv.tail.text = "0"
                } else {
                    feeHtv.tail.text = "${fee.fee} ${fee.token.symbol}"
                    if (fees.size > 1) {
                        val drawable =
                            AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.ic_keyboard_arrow_down,
                            )?.apply {
                                setBounds(0, 0, 12.dp, 12.dp)
                            }
                        feeHtv.tail.setCompoundDrawables(null, null, drawable, null)
                        feeHtv.tail.compoundDrawablePadding = 4.dp
                        feeHtv.tail.setOnClickListener {
                            if (fees.isEmpty()) return@setOnClickListener

                            NetworkFeeBottomSheetDialogFragment.newInstance(
                                fees,
                                currentFee?.token?.assetId,
                            ).apply {
                                callback = { networkFee ->
                                    currentFee = networkFee
                                    updateFeeUI()
                                    dismiss()
                                }
                            }.showNow(
                                parentFragmentManager,
                                NetworkFeeBottomSheetDialogFragment.TAG,
                            )
                        }
                    }
                }
                if (continueVa.displayedChild != POST_TEXT) {
                    continueVa.displayedChild = POST_TEXT
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun handleInnerTransfer() {
        if (supportSwitchAsset) {
            binding.assetRl.setOnClickListener {
                operateKeyboard(false)
                selectAsset()
            }
        } else {
            binding.expandIv.isVisible = false
        }
        val uid = userId
        if (uid != null) {
            chatViewModel.findUserById(uid).observe(
                this,
            ) { u ->
                if (u == null) {
                    jobManager.addJobInBackground(RefreshUserJob(listOf(uid)))
                } else {
                    user = u
                    binding.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                    binding.titleView.setSubTitle(
                        getString(R.string.send_to, u.fullName),
                        u.identityNumber,
                    )
                }
            }
        } else {
            binding.avatar.isVisible = false
            binding.titleView.setSubTitle(
                getString(R.string.Transfer),
                mainnetAddress?.formatPublicKey() ?: "",
            )
        }

        chatViewModel.assetItemsWithBalance().observe(
            this,
            Observer { r: List<TokenItem>? ->
                if (transferBottomOpened) return@Observer
                if (!r.isNullOrEmpty()) {
                    if (assets == r) {
                        return@Observer
                    }
                    assets = r
                    r.find {
                        it.assetId == activity?.defaultSharedPreferences!!.getString(ASSET_PREFERENCE, "")
                    }.let { asset ->
                        currentAsset =
                            if (asset != null) {
                                updateAssetUI(asset)
                                asset
                            } else {
                                val a = assets[0]
                                updateAssetUI(a)
                                a
                            }
                    }
                }
            },
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateAssetUI(asset: TokenItem) {
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
            binding.transferMemo.setHint(R.string.transfer_memo)
        }
        binding.assetName.text = asset.name
        binding.assetDesc.text = asset.balance.numberFormat()
        binding.descEnd.text = asset.symbol
        binding.assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        if (!binding.transferMemo.isFocused) {
            operateKeyboard(true)
        }
        updateAssetAutoComplete(asset)
    }

    private fun isInnerTransfer() = address == null

    private val autoCompleteAdapter by lazy {
        ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            mutableListOf(""),
        )
    }

    private fun updateAssetAutoComplete(asset: TokenItem) {
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
        val value =
            try {
                if (currentAsset == null || currentAsset!!.priceFiat().toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else if (swapped) {
                    BigDecimal(amount).divide(currentAsset!!.priceFiat(), 8, RoundingMode.HALF_UP)
                } else {
                    BigDecimal(amount) * currentAsset!!.priceFiat()
                }
            } catch (e: ArithmeticException) {
                BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        bottomValue = value
        return "${if (swapped) {
            value.numberFormat8()
        } else {
            value.numberFormat2()
        }} $rightSymbol"
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

    @OptIn(UnstableApi::class)
    private fun prepareTransferBottom() =
        lifecycleScope.launch {
            if (currentAsset == null || (user == null && (address == null || currentFee == null) && mainnetAddress == null)) {
                return@launch
            }
            var amount = getAmount()
            try {
                amount = amount.stripAmountZero()
            } catch (e: NumberFormatException) {
                return@launch
            }

            if (!isInnerTransfer()) {
                val dust = address!!.dust?.toBigDecimalOrNull()
                val amountDouble = amount.toBigDecimalOrNull()
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
            val traceId = requireArguments().getString(ARGS_TRACE) ?: UUID.randomUUID().toString()
            val pair = chatViewModel.findLatestTrace(user?.userId, address?.destination, address?.tag, amount, currentAsset!!.assetId)
            if (pair.second) {
                binding.continueVa.displayedChild = POST_TEXT
                return@launch
            }
            var isTraceNotFound = false
            val tx =
                handleMixinResponse(
                    invokeNetwork = { bottomViewModel.getTransactionsById(traceId) },
                    successBlock = { r -> r.data },
                    failureBlock = {
                        isTraceNotFound = it.errorCode == ErrorHandler.NOT_FOUND
                        return@handleMixinResponse isTraceNotFound
                    },
                )
            val status =
                if (isTraceNotFound) {
                    PaymentStatus.pending.name
                } else if (tx != null) {
                    PaymentStatus.paid.name
                } else {
                    return@launch
                }

            val trace = pair.first
            val returnTo = requireArguments().getString(ARGS_RETURN_TO)
            val biometricItem =
                if (user != null) {
                    TransferBiometricItem(user!!, currentAsset!!, amount, null, traceId, memo, status, trace, returnTo)
                } else if (mainnetAddress != null) {
                    AddressTransferBiometricItem(mainnetAddress!!, currentAsset!!, amount, null, traceId, memo, status, returnTo)
                } else {
                    val fee = requireNotNull(currentFee) { "withdrawal currentFee can not be null" }
                    WithdrawBiometricItem(
                        address!!.destination, address!!.tag, address!!.addressId, address!!.label, fee.fee, fee.token.assetId, fee.token.symbol, fee.token.priceFiat(),
                        currentAsset!!, amount, null, traceId, memo, status, trace,
                    )
                }
            binding.continueVa.displayedChild = POST_TEXT

            val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(biometricItem, FROM_TRANSFER)
            preconditionBottom.callback =
                object : PreconditionBottomSheetDialogFragment.Callback {
                    override fun onSuccess() {
                        showTransferBottom(biometricItem)
                    }

                    override fun onCancel() {
                    }
                }
            preconditionBottom.showNow(parentFragmentManager, PreconditionBottomSheetDialogFragment.TAG)
        }

    private suspend fun refreshFees(
        token: TokenItem,
        address: Address,
    ): Boolean {
        return handleMixinResponse(
            invokeNetwork = { bottomViewModel.getFees(token.assetId, address.destination) },
            failureBlock = {
                if (it.errorCode == ErrorHandler.WITHDRAWAL_SUSPEND) {
                    WithdrawalSuspendedBottomSheet.newInstance(token).show(parentFragmentManager, WithdrawalSuspendedBottomSheet.TAG)
                    dismissNow()
                    true
                } else {
                    false
                }
            },
            successBlock = { resp ->
                val data = requireNotNull(resp.data) { "required list can not be null" }
                val ids = data.mapNotNull { it.assetId }
                val tokens = bottomViewModel.findTokenItems(ids)
                fees.clear()
                fees.addAll(
                    data.mapNotNull { d ->
                        tokens.find { t -> t.assetId == d.assetId }?.let {
                            NetworkFee(it, d.amount!!)
                        }
                    },
                )
                if (currentFee == null) {
                    currentFee = fees.firstOrNull()
                }
                return@handleMixinResponse true
            },
        ) ?: false
    }

    private fun showTransferBottom(biometricItem: BiometricItem) {
        val bottom = OutputBottomSheetDialogFragment.newInstance(biometricItem)
        bottom.setCallback(
            object : BiometricBottomSheetDialogFragment.Callback() {
                override fun onDismiss(success: Boolean) {
                    if (success) {
                        dialog?.dismiss()
                        callback?.onSuccess()
                    }
                }
            },
        )
        bottom.onDestroyListener =
            object : OutputBottomSheetDialogFragment.OnDestroyListener {
                override fun onDestroy() {
                    transferBottomOpened = false
                }
            }
        bottom.show(parentFragmentManager, OutputBottomSheetDialogFragment.TAG)
        transferBottomOpened = true
    }

    private val inputFilter =
        InputFilter { source, _, _, _, dstart, _ ->
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
        forbiddenInput =
            if (index == -1) {
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

    private val mWatcher: TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
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

    class TypeAdapter : ListAdapter<TokenItem, ItemHolder>(TokenItem.DIFF_CALLBACK) {
        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transfer_type, parent, false))

        override fun onBindViewHolder(
            holder: ItemHolder,
            position: Int,
        ) {
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

        var currentAsset: TokenItem? = null
    }

    interface OnTypeClickListener {
        fun onTypeClick(asset: TokenItem)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}
