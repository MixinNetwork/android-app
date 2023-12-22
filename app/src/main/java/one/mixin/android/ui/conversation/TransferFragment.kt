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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.databinding.FragmentTransferBinding
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
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.OutputBottomSheetDialogFragment
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.WaitingBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.ValuableBiometricBottomSheetDialogFragment.Companion.ARGS_BIOMETRIC_ITEM
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment.Companion.FROM_TRANSFER
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_TRANSFER
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.ui.wallet.NetworkFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.UserTransactionBottomSheetFragment
import one.mixin.android.ui.wallet.WithdrawalSuspendedBottomSheet
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getChainName
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.displayAddress
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("InflateParams")
class TransferFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSET_PREFERENCE = "TRANSFER_ASSET"

        const val POST_TEXT = 0
        const val POST_PB = 1

        inline fun <reified T : BiometricItem> newInstance(t: T) =
            TransferFragment().withArgs {
                putParcelable(ARGS_BIOMETRIC_ITEM, t)
            }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val chatViewModel by viewModels<ConversationViewModel>()

    private val t: AssetBiometricItem by lazy {
        requireArguments().getParcelableCompat(ARGS_BIOMETRIC_ITEM, AssetBiometricItem::class.java)!!
    }
    private val supportSwitchAsset: Boolean by lazy { t.asset == null }

    private val fees: ArrayList<NetworkFee> = arrayListOf()
    private var currentFee: NetworkFee? = null

    private var swapped = false
    private var bottomValue = BigDecimal.ZERO

    private var transferBottomOpened = false

    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getScanResult = registerForActivityResult(CaptureActivity.CaptureContract(), requireActivity().activityResultRegistry, ::callbackScan)
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
            t.asset?.let {
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
        val t = this.t
        val defaultToken = t.asset
        if (defaultToken != null) {
            updateAssetUI(defaultToken)
        }
        if (isInnerTransfer()) {
            handleInnerTransfer(t)
        } else {
            handleAddressTransfer(t as WithdrawBiometricItem)
        }
        if (t is TransferBiometricItem && t.users.size == 1) {
            binding.titleView.rightIb.setOnClickListener {
                UserTransactionBottomSheetFragment.newInstance(t.users.first().userId)
                    .showNow(parentFragmentManager, UserTransactionBottomSheetFragment.TAG)
            }
        } else {
            binding.titleView.rightIb.isVisible = false
        }

        binding.continueTv.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            operateKeyboard(false)
            lifecycleScope.launch {
                val rawTransaction = bottomViewModel.firstUnspentTransaction()
                if (rawTransaction!=null) {
                    WaitingBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, WaitingBottomSheetDialogFragment.TAG)
                } else {
                    checkUtxo {
                        prepareTransferBottom()
                    }
                }
            }
        }
        val amount = t.amount.toDoubleOrNull()
        if (amount != null && amount > 0) {
            binding.amountEt.setText(t.amount)
            binding.amountEt.isEnabled = false
        }
        val memo = t.memo
        if (!memo.isNullOrBlank()) {
            binding.transferMemo.setText(memo)
            binding.transferMemo.isEnabled = false
            binding.memoIv.isEnabled = false
        }
        chatViewModel.assetItemsWithBalance().observe(
            this,
            Observer { r: List<TokenItem>? ->
                if (transferBottomOpened) return@Observer
                if (r.isNullOrEmpty()) return@Observer

                if (defaultToken != null) {
                    val token = r.firstOrNull { it.assetId == defaultToken.assetId }
                    if (token != null) {
                        t.asset = token
                        updateAssetUI(token)
                    }
                } else {
                    val token = r.firstOrNull { it.assetId == requireActivity().defaultSharedPreferences.getString(ASSET_PREFERENCE, "") }
                    if (token != null) {
                        t.asset = token
                        updateAssetUI(token)
                    } else {
                        val a = r[0]
                        t.asset = a
                        updateAssetUI(a)
                    }
                }
            },
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isAdded) {
            operateKeyboard(false)
        }
        super.onDismiss(dialog)
    }

    private fun isInnerTransfer() = t !is WithdrawBiometricItem

    @OptIn(UnstableApi::class)
    private fun handleInnerTransfer(t: AssetBiometricItem) {
        if (supportSwitchAsset) {
            binding.assetRl.setOnClickListener {
                operateKeyboard(false)
                selectAsset()
            }
        } else {
            binding.expandIv.isVisible = false
        }
        if (t is TransferBiometricItem) {
            handleUuidTransfer(t)
        } else {
            t as AddressTransferBiometricItem
            handleMainnetAddressTransfer(t)
        }
    }

    private fun handleUuidTransfer(t: TransferBiometricItem) {
        if (t.users.size == 1) {
            val uid = t.users.first().userId
            chatViewModel.findUserById(uid).observe(
                this,
            ) { u ->
                if (u == null) {
                    jobManager.addJobInBackground(RefreshUserJob(listOf(uid)))
                } else {
                    t.users = listOf(u)
                    binding.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                    binding.titleView.setSubTitle(
                        getString(R.string.send_to, u.fullName),
                        u.identityNumber,
                    )
                }
            }
        } else {
            binding.avatar.isVisible = false
            binding.receiversView.isVisible = true
            binding.receiversView.addList(t.users)
            binding.receiversView.setOnClickListener {
                showUserList(t.users, false)
            }
        }
    }

    private fun handleMainnetAddressTransfer(t: AddressTransferBiometricItem) {
        binding.avatar.isVisible = false
        binding.titleView.setSubTitle(
            getString(R.string.Transfer),
            t.address.formatPublicKey(),
        )
    }

    private fun handleAddressTransfer(t: WithdrawBiometricItem) {
        binding.avatar.isVisible = false
        binding.expandIv.isVisible = false
        binding.assetRl.setOnClickListener(null)
        if (t.address.addressId.isBlank()) { // mock address
            binding.titleView.setSubTitle(
                getString(R.string.send_to, t.address.label),
                t.displayAddress().formatPublicKey(),
            )
            updateFeeUI(t)
        } else {
            chatViewModel.observeAddress(t.address.addressId).observe(
                this,
            ) {
                t.address = it
                binding.titleView.setSubTitle(
                    getString(R.string.send_to, it.label),
                    it.displayAddress().formatPublicKey(),
                )
                updateFeeUI(t)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFeeUI(t: WithdrawBiometricItem) =
        lifecycleScope.launch {
            val token = t.asset ?: return@launch
            val address = t.address
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
        val amount = asset.balance.toDoubleOrNull()
        if (amount != null && amount <= 0) {
            binding.amountEt.isEnabled = false
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

    private val autoCompleteAdapter by lazy {
        ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            mutableListOf(""),
        )
    }

    private fun selectAsset() {
        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_TRANSFER, currentAssetId = t.asset?.assetId)
            .setOnAssetClick { asset ->
                t.asset = asset
                updateAssetUI(asset)
            }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
    }

    private fun showUserList(
        userList: List<User>,
        isSender: Boolean,
    ) {
        val t = t as TransferBiometricItem
        val title =
            if (isSender) {
                getString(R.string.Senders)
            } else {
                getString(R.string.multisig_receivers_threshold, "${t.threshold}/${t.users.size}")
            }
        UserListBottomSheetDialogFragment.newInstance(ArrayList(userList), title)
            .showNow(parentFragmentManager, UserListBottomSheetDialogFragment.TAG)
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
            val symbol = if (swapped) t.asset?.symbol ?: "" else Fiats.getAccountCurrencyAppearance()
            val index = s.indexOf(symbol)
            return if (index != -1) {
                s.substring(0, index)
            } else {
                s
            }.trim()
        }
    }

    private fun getTopSymbol(): String {
        return if (swapped) Fiats.getAccountCurrencyAppearance() else t.asset?.symbol ?: ""
    }

    private fun getBottomText(): String {
        val asset = t.asset ?: return ""
        val amount = binding.amountEt.text.toString().toDoubleOrNull() ?: 0.0
        val rightSymbol = if (swapped) asset.symbol else Fiats.getAccountCurrencyAppearance()
        val value =
            try {
                if (asset.priceFiat().toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else if (swapped) {
                    BigDecimal(amount).divide(asset.priceFiat(), 8, RoundingMode.HALF_UP)
                } else {
                    BigDecimal(amount) * asset.priceFiat()
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

    private fun checkUtxo(callback: () -> Unit) {
        val token = t.asset ?: return
        lifecycleScope.launch {
            var amount = getAmount()
            try {
                amount = amount.stripAmountZero()
            } catch (e: NumberFormatException) {
                return@launch
            }
            val consolidationAmount = bottomViewModel.checkUtxoSufficiency(token.assetId, amount)
            if (consolidationAmount != null) {
                UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), t.asset, consolidationAmount, UUID.randomUUID().toString(), null, null))
                    .show(parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
            } else {
                callback.invoke()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun prepareTransferBottom() =
        lifecycleScope.launch {
            val t = this@TransferFragment.t
            if (t !is TransferBiometricItem && t !is AddressTransferBiometricItem && t !is WithdrawBiometricItem) {
                return@launch
            }
            val asset = t.asset ?: return@launch

            var amount = getAmount()
            try {
                amount = amount.stripAmountZero()
            } catch (e: NumberFormatException) {
                return@launch
            }
            t.amount = amount

            if (!isInnerTransfer()) {
                t as WithdrawBiometricItem
                val address = t.address
                val dust = address.dust?.toBigDecimalOrNull()
                val amountDouble = amount.toBigDecimalOrNull()
                if (dust != null && amountDouble != null && amountDouble < dust) {
                    toast(getString(R.string.withdrawal_minimum_amount, address.dust, asset.symbol))
                    return@launch
                }
            }

            val memo = binding.transferMemo.text.toString()
            if (memo.toByteArray().size > 140) {
                toast("${binding.transferMemo.hint} ${getString(R.string.Content_too_long)}")
                return@launch
            }
            t.memo = memo

            binding.continueVa.displayedChild = POST_PB
            val traceId = t.traceId
            val pair =
                if (t is TransferBiometricItem && t.users.size == 1) {
                    chatViewModel.findLatestTrace(t.users.first().userId, null, null, amount, asset.assetId)
                } else if (t is WithdrawBiometricItem) {
                    chatViewModel.findLatestTrace(null, t.address.destination, t.address.tag, amount, asset.assetId)
                } else {
                    Pair(null, false)
                }
            if (pair.second) {
                binding.continueVa.displayedChild = POST_TEXT
                return@launch
            }
            if (t is TransferBiometricItem && t.users.size == 1) {
                t.trace = pair.first
            } else if (t is WithdrawBiometricItem) {
                t.trace = pair.first
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
            t.state =
                if (isTraceNotFound) {
                    PaymentStatus.pending.name
                } else if (tx != null) {
                    PaymentStatus.paid.name
                } else {
                    binding.continueVa.displayedChild = POST_TEXT
                    return@launch
                }

            if (t is WithdrawBiometricItem) {
                val fee = requireNotNull(currentFee) { "withdrawal currentFee can not be null" }
                t.fee = fee
            }

            binding.continueVa.displayedChild = POST_TEXT
            val preconditionBottom = PreconditionBottomSheetDialogFragment.newInstance(t, FROM_TRANSFER)
            preconditionBottom.callback =
                object : PreconditionBottomSheetDialogFragment.Callback {
                    override fun onSuccess() {
                        showTransferBottom(t)
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
                    dismiss()
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
                    if (binding.amountRl.isVisible && t.asset != null) {
                        binding.amountEt.hint = ""
                        binding.symbolTv.text = getTopSymbol()
                        binding.amountAsTv.text = getBottomText()
                    }
                } else {
                    binding.continueTv.isEnabled = false
                    binding.continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                    if (binding.amountRl.isVisible) {
                        binding.amountEt.hint = "0.00 ${if (swapped) Fiats.getAccountCurrencyAppearance() else t.asset?.symbol}"
                        binding.symbolTv.text = ""
                        binding.amountAsTv.text = "0.00 ${if (swapped) t.asset?.symbol else Fiats.getAccountCurrencyAppearance()}"
                    }
                }
            }
        }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}
