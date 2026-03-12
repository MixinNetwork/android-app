package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.Web3RawTransactionRequest
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.buildTransaction
import one.mixin.android.db.web3.vo.getChainFromName
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.buildTipGas
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.biometric.isUtxoException
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.reportException
import one.mixin.android.util.tickerFlow
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import org.sol4kt.VersionedTransactionCompat
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class LimitTransferBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "LimitTransferBottomSheetDialogFragment"
        private const val ARGS_LINK = "args_link"
        private const val ARGS_IN_AMOUNT = "args_in_amount"
        private const val ARGS_IN_ASSET = "args_in_asset"
        private const val ARGS_OUT_AMOUNT = "args_out_amount"
        private const val ARGS_OUT_ASSET = "args_out_asset"
        private const val ARGS_EXPIRED_AT = "args_expired_at"
        private const val ARGS_DEPOSIT_DESTINATION = "args_deposit_destination"
        private const val ARGS_WALLET_ID = "args_wallet_id"
        private const val ARGS_DISPLAY_USER_ID = "args_display_user_id"

        fun newInstance(order: CreateLimitOrderResponse, from: SwapToken, to: SwapToken, walletId: String): LimitTransferBottomSheetDialogFragment {
            return LimitTransferBottomSheetDialogFragment().withArgs {
                putString(ARGS_LINK, order.tx)
                putString(ARGS_DEPOSIT_DESTINATION, order.depositDestination)
                putParcelable(ARGS_IN_ASSET, from)
                putParcelable(ARGS_OUT_ASSET, to)
                putString(ARGS_IN_AMOUNT, order.order.payAmount)
                putString(ARGS_OUT_AMOUNT, order.order.expectedReceiveAmount ?: "0")
                putString(ARGS_EXPIRED_AT, order.order.expiredAt)
                putString(ARGS_WALLET_ID, walletId)
                putString(ARGS_DISPLAY_USER_ID, order.displayUserId)
            }
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    private val bottomViewModel by viewModels<BottomSheetViewModel>()
    private val web3ViewModel by viewModels<Web3ViewModel>()

    enum class Step { Pending, Sending, Done, Error }

    @Inject
    lateinit var rpc: Rpc

    private val inAsset by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_IN_ASSET, SwapToken::class.java)) }
    private val inAmount by lazy { BigDecimal(requireNotNull(requireArguments().getString(ARGS_IN_AMOUNT))) }
    private val outAsset by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_OUT_ASSET, SwapToken::class.java)) }
    private val outAmount by lazy { BigDecimal(requireNotNull(requireArguments().getString(ARGS_OUT_AMOUNT))) }
    private val senderWalletId by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_ID)) }

    private val self by lazy { requireNotNull(Session.getAccount()).toUser() }
    private val link by lazy { 
        val linkStr = requireArguments().getString(ARGS_LINK)
        if (!linkStr.isNullOrBlank()) Uri.parse(linkStr) else null
    }
    private val depositDestination by lazy { requireArguments().getString(ARGS_DEPOSIT_DESTINATION) }
    private val expiredAt: String? by lazy { requireArguments().getString(ARGS_EXPIRED_AT) }
    private val displayUserId by lazy { requireArguments().getString(ARGS_DISPLAY_USER_ID) }

    private var step by mutableStateOf(Step.Pending)
    private var parsedLink: ParsedLink? = null
    private var receiver: User? by mutableStateOf(null)
    private var errorInfo: String? by mutableStateOf(null)

    private var web3Transaction: JsSignMessage? by mutableStateOf(null)
    private var tipGas: TipGas? by mutableStateOf(null)
    private var solanaFee: BigDecimal? by mutableStateOf(null)
    private var solanaTx: VersionedTransactionCompat? by mutableStateOf(null)
    private var chainToken: Web3TokenItem? by mutableStateOf(null)
    private var token: Web3TokenItem? by mutableStateOf(null)
    private var insufficientGas by mutableStateOf(false)
    private var walletName by mutableStateOf<String?>(null)
    private var isWeb3 by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        parse()
        return view
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier =
                    Modifier
                        .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(MixinAppTheme.colors.background),
            ) {
                // No wallet label for mixin internal transfer
                Column(
                    modifier =
                        Modifier
                            .weight(weight = 1f, fill = true)
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.height(50.dp))
                    when (step) {
                        Step.Sending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(70.dp),
                                color = MixinAppTheme.colors.accent,
                            )
                        }
                        Step.Error -> {
                            Icon(
                                modifier = Modifier.size(70.dp),
                                painter = painterResource(id = R.drawable.ic_transfer_status_failed),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                        Step.Done -> {
                            Icon(
                                modifier = Modifier.size(70.dp),
                                painter = painterResource(id = R.drawable.ic_transfer_status_success),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                        else ->
                            Box(
                                modifier = Modifier.wrapContentWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(70.dp)
                                            .offset(x = (-27).dp)
                                            .border(
                                                1.5.dp,
                                                MixinAppTheme.colors.background,
                                                CircleShape
                                            )
                                ) {
                                    CoilImage(
                                        model = inAsset.icon,
                                        placeholder = R.drawable.ic_avatar_place_holder,
                                        modifier = Modifier
                                            .size(67.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                    )
                                }
                                Box(
                                    modifier =
                                        Modifier
                                            .size(70.dp)
                                            .offset(x = 27.dp)
                                            .border(
                                                1.5.dp,
                                                MixinAppTheme.colors.background,
                                                CircleShape
                                            )
                                ) {
                                    CoilImage(
                                        model = outAsset.icon,
                                        placeholder = R.drawable.ic_avatar_place_holder,
                                        modifier = Modifier
                                            .size(67.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                    }
                    Box(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            id = when (step) {
                                Step.Pending -> R.string.swap_confirmation
                                Step.Done -> R.string.web3_sending_success
                                Step.Error -> R.string.swap_failed
                                Step.Sending -> R.string.Sending
                            }
                        ),
                        style = TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                    Box(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = errorInfo ?: stringResource(id = if (step == Step.Done) R.string.swap_message_success else R.string.swap_inner_desc),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = if (errorInfo != null) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textMinor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                        ),
                        maxLines = 3,
                        minLines = 3,
                    )
                    Box(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow),
                    )
                    Box(modifier = Modifier.height(20.dp))
                    AssetChanges(title = stringResource(id = R.string.Balance_Change).uppercase(), inAmount = inAmount, inAsset = inAsset, outAmount = outAmount, outAsset = outAsset)
                    Box(modifier = Modifier.height(20.dp))
                    ItemContent(title = stringResource(id = R.string.Order_Type).uppercase(), subTitle = stringResource(id = R.string.order_type_limit))
                    Box(modifier = Modifier.height(20.dp))
                    ItemLimitPriceContent(title = stringResource(id = R.string.Price).uppercase(), inAmount = inAmount, inAsset = inAsset, outAmount = outAmount, outAsset = outAsset)
                    Box(modifier = Modifier.height(20.dp))
                    val expiryLabel = remember(expiredAt) { mapExpiryToLabel(expiredAt) }
                    ItemContent(title = stringResource(id = R.string.expiry).uppercase(), subTitle = stringResource(id = expiryLabel))
                    Box(modifier = Modifier.height(20.dp))
                    ItemUserContent(title = stringResource(id = R.string.Receiver).uppercase(), user = receiver, address = null)
                    Box(modifier = Modifier.height(20.dp))
                    val isPrivacyWallet = senderWalletId == Session.getAccountId()
                    ItemWalletContent(
                        title = stringResource(id = R.string.Sender).uppercase(),
                        fontSize = 16.sp,
                        walletId = if (isPrivacyWallet) null else senderWalletId,
                        walletName = if (isPrivacyWallet) null else walletName
                    )
                    if (parsedLink?.memo.isNullOrBlank().not()) {
                        Box(modifier = Modifier.height(20.dp))
                        ItemContent(title = stringResource(id = R.string.Memo).uppercase(), subTitle = parsedLink?.memo ?: stringResource(id = R.string.None))
                    }
                    Box(modifier = Modifier.height(20.dp))
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    when (step) {
                        Step.Done -> {
                            Row(
                                modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(MixinAppTheme.colors.background)
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            ) {
                                Button(
                                    onClick = { onDoneAction?.invoke(); dismiss() },
                                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MixinAppTheme.colors.accent),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                                ) { Text(text = stringResource(id = R.string.Done), color = Color.White) }
                            }
                        }
                        Step.Error -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Retry),
                                cancelAction = { dismiss() },
                                confirmAction = {
                                    errorInfo = null
                                    showPin()
                                },
                            )
                        }

                        Step.Pending -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Continue),
                                cancelAction = { dismiss() },
                                confirmAction = { showPin() },
                            )
                        }
                        Step.Sending -> {}
                    }
                }
                Box(modifier = Modifier.height(36.dp))
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    private var onDoneAction: (() -> Unit)? = null
    private var onDestroyAction: (() -> Unit)? = null

    fun setOnDone(callback: () -> Unit): LimitTransferBottomSheetDialogFragment { onDoneAction = callback; return this }
    fun setOnDestroy(callback: () -> Unit): LimitTransferBottomSheetDialogFragment { onDestroyAction = callback; return this }

    override fun onDismiss(dialog: DialogInterface) { super.onDismiss(dialog); onDestroyAction?.invoke() }

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1).setOnPinComplete { pin ->
            lifecycleScope.launch(CoroutineExceptionHandler { _, error -> handleException(error) }) {
                doAfterPinComplete(pin)
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun doAfterPinComplete(pin: String) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            step = Step.Sending
            if (isWeb3 && web3Transaction != null) {
                try {
                    when (web3Transaction!!.type) {
                        JsSignMessage.TYPE_RAW_TRANSACTION -> {
                            val priv = bottomViewModel.getWeb3Priv(requireContext(), pin, inAsset.chain.chainId)
                            val tx = Web3Signer.signSolanaTransaction(priv, requireNotNull(solanaTx) { "required solana tx can not be null" }) {
                                val blockhash = rpc.getLatestBlockhash() ?: throw IllegalArgumentException("failed to get blockhash")
                                return@signSolanaTransaction blockhash
                            }
                            val sig = tx.signatures.first()
                            val rawTx = tx.serialize().base64Encode()
                            val request = Web3RawTransactionRequest(Constants.ChainId.Solana, rawTx, tx.message.accounts[0].toBase58(), null)
                            val response = bottomViewModel.postRawTx(rawTx, Constants.ChainId.Solana, tx.message.accounts[0].toBase58(), inAsset.assetId)
                            defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                            context?.updatePinCheck()
                            step = Step.Done
                            val wallet = if (inAsset.isWeb3) AnalyticsTracker.TradeWallet.WEB3 else AnalyticsTracker.TradeWallet.MAIN
                            AnalyticsTracker.trackTradeEnd(wallet, inAmount, inAsset.price)
                        }

                        JsSignMessage.TYPE_TRANSACTION -> {
                            val transaction = requireNotNull(web3Transaction!!.wcEthereumTransaction)
                            val priv = bottomViewModel.getWeb3Priv(requireContext(), pin, inAsset.chain.chainId)
                            val pair = Web3Signer.ethSignTransaction(priv, transaction, tipGas!!, chain = token?.getChainFromName()) { address ->
                                val nonce = rpc.nonceAt(inAsset.chain.chainId, address) ?: throw IllegalArgumentException("failed to get nonce")
                                return@ethSignTransaction nonce
                            }
                            val result = bottomViewModel.postRawTx(pair.first, inAsset.chain.chainId, pair.second, inAsset.assetId)
                            defaultSharedPreferences.putLong(Constants.BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                            context?.updatePinCheck()
                            step = Step.Done
                            val wallet = if (inAsset.isWeb3) AnalyticsTracker.TradeWallet.WEB3 else AnalyticsTracker.TradeWallet.MAIN
                            AnalyticsTracker.trackTradeEnd(wallet, inAmount, inAsset.price)
                        }

                        else -> {
                            throw IllegalArgumentException("invalid signMessage type ${web3Transaction!!.type}")
                        }
                    }
                } catch (e: Exception) {
                    handleException(e)
                    return@launch
                }
            } else {
                val parsed = this@LimitTransferBottomSheetDialogFragment.parsedLink ?: return@launch
                val consolidationAmount = bottomViewModel.checkUtxoSufficiency(parsed.assetId, parsed.amount)
                val token = bottomViewModel.findAssetItemById(parsed.assetId)
                if (consolidationAmount != null && token != null) {
                    UtxoConsolidationBottomSheetDialogFragment.newInstance(
                        buildTransferBiometricItem(Session.getAccount()!!.toUser(), token, consolidationAmount, UUID.randomUUID().toString(), null, null)
                    ).show(parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
                    step = Step.Pending
                    return@launch
                } else if (token == null) {
                    errorInfo = getString(R.string.Data_error)
                    step = Step.Error
                    return@launch
                }
                val response = bottomViewModel.kernelTransaction(parsed.assetId, parsed.receiverIds, 1.toByte(), parsed.amount, pin, parsed.traceId, parsed.memo)
                handleTransactionResponse(response)
            }
        } catch (e: Exception) { handleException(e) }
    }

    private suspend fun handleTransactionResponse(response: MixinResponse<*>) {
        if (response.isSuccess) {
            context?.updatePinCheck()
            step = Step.Done
            val wallet = if (inAsset.isWeb3) AnalyticsTracker.TradeWallet.WEB3 else AnalyticsTracker.TradeWallet.MAIN
            AnalyticsTracker.trackTradeEnd(wallet, inAmount, inAsset.price)
        } else {
            errorInfo = handleError(response.error) ?: response.errorDescription
            step = Step.Error
        }
    }

    private suspend fun handleError(error: ResponseError?): String? {
        if (error != null) {
            val errorCode = error.code
            val errorDescription = error.description
            val info = when (errorCode) {
                ErrorHandler.TOO_MANY_REQUEST -> requireContext().getString(R.string.error_pin_check_too_many_request)
                ErrorHandler.PIN_INCORRECT -> {
                    val errorCount = bottomViewModel.errorCount()
                    requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
                }
                else -> requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
            }
            return info
        } else {
            return null
        }
    }

    fun getBiometricInfo() = BiometricInfo(getString(R.string.Verify_by_Biometric), "", "")

    // No explicit BottomSheetBehavior callback needed with MixinComposeBottomSheetDialogFragment

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { _ -> realFragmentCount++ }
            if (realFragmentCount <= 0) { activity?.finish() }
        }
    }

    override fun dismiss() { dismissAllowingStateLoss() }

    inner class ParsedLink(
        val assetId: String,
        val amount: String,
        val receiverIds: List<String>,
        val memo: String?,
        val traceId: String,
    )

    private fun mapExpiryToLabel(expiredAt: String?): Int {
        if (expiredAt.isNullOrBlank()) return R.string.expiry_never
        return runCatching {
            val target = Instant.parse(expiredAt)
            val now = Instant.now()
            val seconds = Duration.between(now, target).seconds
            if (seconds >= Duration.ofDays(365L * 50L).seconds) return R.string.expiry_never
            val mins = seconds / 60
            val hours = seconds / 3600
            val days = seconds / (24 * 3600)
            return when {
                mins <= 15 -> R.string.expiry_10_min
                hours <= 1 -> R.string.expiry_1_hour
                days <= 1 -> R.string.expiry_1_day
                days <= 3 -> R.string.expiry_3_days
                days <= 7 -> R.string.expiry_1_week
                days <= 30 -> R.string.expiry_1_month
                else -> R.string.expiry_1_year
            }
        }.getOrElse { R.string.expiry_never }
    }

    private fun parse() = lifecycleScope.launch {
        val uri = link
        if (uri != null) {
            val assetId = requireNotNull(uri.getQueryParameter("asset"))
            val amount = requireNotNull(uri.getQueryParameter("amount"))
            val receiverId = requireNotNull(uri.lastPathSegment)
            receiver = bottomViewModel.refreshUser(receiverId)
            val receiverIds = listOf(receiverId)
            val memo = uri.getQueryParameter("memo")
            val traceId = uri.getQueryParameter("trace") ?: UUID.randomUUID().toString()
            parsedLink = ParsedLink(assetId, amount, receiverIds, memo, traceId)
            isWeb3 = false
        } else if (!depositDestination.isNullOrBlank()) {
            if (!displayUserId.isNullOrBlank()) {
                receiver = bottomViewModel.refreshUser(displayUserId!!)
            }
            isWeb3 = true
            val wallet = web3ViewModel.findWalletById(Web3Signer.currentWalletId)
            walletName = wallet?.name.takeIf { !it.isNullOrEmpty() } ?: getString(R.string.Common_Wallet)
            
            val token = bottomViewModel.web3TokenItemById(Web3Signer.currentWalletId, inAsset.assetId)
            if (token != null) {
                try {
                    val transaction = token.buildTransaction(
                        rpc,
                        if (token.chainId == Constants.ChainId.Solana) Web3Signer.solanaAddress else Web3Signer.evmAddress,
                        depositDestination!!,
                        inAmount.toPlainString()
                    )
                    web3Transaction = transaction
                    this@LimitTransferBottomSheetDialogFragment.token = token
                    
                    val chain = token.getChainFromName()
                    refreshEstimatedGasAndAsset(chain)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to build transaction")
                    errorInfo = e.message
                    step = Step.Error
                }
            }
        }
    }

    private fun refreshEstimatedGasAndAsset(chain: Chain) {
        if (chain == Chain.Solana) {
            refreshSolana()
            return
        }

        val assetId = chain.assetId
        val transaction = web3Transaction?.wcEthereumTransaction ?: return

        tickerFlow(15.seconds)
            .onEach {
                try {
                    tipGas = withContext(Dispatchers.IO) {
                        val r = bottomViewModel.estimateFee(
                            EstimateFeeRequest(
                                assetId,
                                null,
                                transaction.data,
                                transaction.from,
                                transaction.to,
                                transaction.value,
                            )
                        )
                        if (!r.isSuccess) {
                            step = Step.Error
                            errorInfo = r.errorDescription
                            return@withContext null
                        }
                        buildTipGas(chain.chainId, r.data!!)
                    } ?: return@onEach
                    chainToken = bottomViewModel.web3TokenItemById(Web3Signer.currentWalletId, token?.chainId ?: "")
                    insufficientGas = checkGas(token, chainToken, tipGas, transaction.value, transaction.maxFeePerGas)
                    if (insufficientGas) {
                        handleException(IllegalArgumentException(requireContext().getString(R.string.insufficient_gas, chainToken?.symbol ?: chain.symbol)))
                    }

                    val hex = Web3Signer.ethPreviewTransaction(
                        Web3Signer.evmAddress,
                        transaction,
                        tipGas!!,
                        chain = token?.getChainFromName()
                    ) { _ ->
                        val nonce = rpc.nonceAt(chain.assetId, Web3Signer.evmAddress) ?: throw IllegalArgumentException("failed to get nonce")
                        return@ethPreviewTransaction nonce
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun refreshSolana() {
        tickerFlow(15.seconds)
            .onEach {
                try {
                    if (web3Transaction?.type == JsSignMessage.TYPE_RAW_TRANSACTION) {
                        val tx = solanaTx ?: VersionedTransactionCompat.from(web3Transaction?.data ?: "").apply {
                            solanaTx = this
                        }
                        solanaFee = solanaTx?.calcFee(tx.message.accounts[0].toBase58())
                    }
                } catch (e: Exception) {
                    handleException(e)
                }
            }.launchIn(lifecycleScope)
    }

    private fun checkGas(
        web3Token: Web3TokenItem?,
        chainToken: Web3TokenItem?,
        tipGas: TipGas?,
        value: String?,
        maxFeePerGas: String?,
    ): Boolean {
        return if (web3Token != null) {
            if (chainToken == null) {
                true
            } else if (tipGas != null) {
                val maxGas = tipGas.displayValue(maxFeePerGas) ?: BigDecimal.ZERO
                if (web3Token.assetId == chainToken.assetId && web3Token.chainId == chainToken.chainId) {
                    Convert.fromWei(Numeric.decodeQuantity(value ?: "0x0").toBigDecimal(), Convert.Unit.ETHER) + maxGas > BigDecimal(chainToken.balance)
                } else {
                    maxGas > BigDecimal(chainToken.balance)
                }
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun handleException(t: Throwable) {
        Timber.e(t)
        errorInfo = if (t.isTipNodeException()) {
            t.getTipExceptionMsg(requireContext(), null)
        } else if (t.isUtxoException()) {
            t.getUtxoExceptionMsg(requireContext())
        } else {
            t.message ?: t.toString()
        }
        reportException("$TAG handleException", t)
        step = Step.Error
    }

    override fun showError(error: String) {
    }
}

@Composable
fun ItemLimitPriceContent(
    title: String,
    inAmount: BigDecimal,
    inAsset: SwapToken,
    outAmount: BigDecimal,
    outAsset: SwapToken
) {

    val price = runCatching {
        if (inAmount.compareTo(BigDecimal.ZERO) == 0) null else outAmount.divide(inAmount, 8, RoundingMode.HALF_UP)
    }.getOrNull()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "1 ${inAsset.symbol} = ${price.stripTrailingZeros().toPlainString()} ${outAsset.symbol}",
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W400
                )
            }
            Box(modifier = Modifier.height(4.dp))
            val inverted = runCatching { BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP) }.getOrNull()
            if (inverted != null) {
                Text(
                    text = "1 ${outAsset.symbol} = ${inverted.stripTrailingZeros().toPlainString()} ${inAsset.symbol}",
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400
                )
            }
            Box(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "-",
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400
            )
            Box(modifier = Modifier.height(4.dp))
        }
    }
}
