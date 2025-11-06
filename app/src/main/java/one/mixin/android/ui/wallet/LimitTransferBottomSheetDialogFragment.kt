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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.response.CreateLimitOrderResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsBottom
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.biometric.isUtxoException
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.reportException
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.max
import one.mixin.android.ui.wallet.AssetChanges
import one.mixin.android.ui.wallet.ItemPriceContent
import one.mixin.android.ui.wallet.ItemUserContent
import java.math.RoundingMode

@AndroidEntryPoint
class LimitTransferBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "LimitTransferBottomSheetDialogFragment"
        private const val ARGS_LINK = "args_link"
        private const val ARGS_IN_AMOUNT = "args_in_amount"
        private const val ARGS_IN_ASSET = "args_in_asset"
        private const val ARGS_OUT_AMOUNT = "args_out_amount"
        private const val ARGS_OUT_ASSET = "args_out_asset"

        fun newInstance(order: CreateLimitOrderResponse, from: SwapToken, to: SwapToken): LimitTransferBottomSheetDialogFragment {
            return LimitTransferBottomSheetDialogFragment().withArgs {
                putString(ARGS_LINK, order.tx)
                putParcelable(ARGS_IN_ASSET, from)
                putParcelable(ARGS_OUT_ASSET, to)
                putString(ARGS_IN_AMOUNT, order.order.payAmount)
                putString(ARGS_OUT_AMOUNT, order.order.expectedReceiveAmount ?: "0")
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

    enum class Step { Pending, Sending, Done, Error }

    private val inAsset by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_IN_ASSET, SwapToken::class.java)) }
    private val inAmount by lazy { BigDecimal(requireNotNull(requireArguments().getString(ARGS_IN_AMOUNT))) }
    private val outAsset by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_OUT_ASSET, SwapToken::class.java)) }
    private val outAmount by lazy { BigDecimal(requireNotNull(requireArguments().getString(ARGS_OUT_AMOUNT))) }

    private val self by lazy { requireNotNull(Session.getAccount()).toUser() }
    private val link by lazy { Uri.parse(requireNotNull(requireArguments().getString(ARGS_LINK))) }

    private var step by mutableStateOf(Step.Pending)
    private var parsedLink: ParsedLink? = null
    private var receiver: User? by mutableStateOf(null)
    private var errorInfo: String? by mutableStateOf(null)

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
                            .weight(weight = 1f, fill = true),
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
                    ItemContent(title = stringResource(id = R.string.Order_Type).uppercase(), subTitle = stringResource(id = R.string.Forever))
                    Box(modifier = Modifier.height(20.dp))
                    ItemLimitPriceContent(title = stringResource(id = R.string.Price).uppercase(), inAmount = inAmount, inAsset = inAsset, outAmount = outAmount, outAsset = outAsset)
                    Box(modifier = Modifier.height(20.dp))
                    // Receiver
                    ItemUserContent(title = stringResource(id = R.string.Receivers).uppercase(), user = receiver, address = null)
                    // Sender
                    ItemWalletContent(title = stringResource(id = R.string.Senders).uppercase(), fontSize = 16.sp)
                    Box(modifier = Modifier.height(20.dp))
                    ItemContent(title = stringResource(id = R.string.Memo).uppercase(), subTitle = parsedLink?.memo ?: stringResource(id = R.string.None))
                    Box(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.height(16.dp))
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
                                confirmAction = { showPin() },
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
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop() - view.getSafeAreaInsetsBottom()
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
        } catch (e: Exception) { handleException(e) }
    }

    private suspend fun handleTransactionResponse(response: MixinResponse<*>) {
        if (response.isSuccess) {
            context?.updatePinCheck()
            step = Step.Done
            AnalyticsTracker.trackSwapSend()
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

    private fun handleException(t: Throwable) {
        Timber.e(t)
        errorInfo = if (t.isUtxoException()) {
            t.getUtxoExceptionMsg(requireContext())
        } else {
            t.message ?: t.toString()
        }
        reportException("$TAG handleException", t)
        step = Step.Error
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

    private fun parse() = lifecycleScope.launch {
        val uri = link
        val assetId = requireNotNull(uri.getQueryParameter("asset"))
        val amount = requireNotNull(uri.getQueryParameter("amount"))
        val receiverId = requireNotNull(uri.lastPathSegment)
        receiver = bottomViewModel.refreshUser(receiverId)
        val receiverIds = listOf(receiverId)
        val memo = uri.getQueryParameter("memo")
        val traceId = uri.getQueryParameter("trace") ?: UUID.randomUUID().toString()
        parsedLink = ParsedLink(assetId, amount, receiverIds, memo, traceId)
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
    val price = outAmount.divide(inAmount, 8, RoundingMode.HALF_UP)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = "1 ${inAsset.symbol} = ${price.toPlainString()} ${outAsset.symbol}",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
        }
        Box(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = "1 ${outAsset.symbol} = ${BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP).toPlainString()} ${inAsset.symbol}",
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400
        )
        Box(modifier = Modifier.height(4.dp))
    }
}
