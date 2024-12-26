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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.putLong
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.common.biometric.getUtxoExceptionMsg
import one.mixin.android.ui.common.biometric.isUtxoException
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.tip.wc.sessionrequest.FeeInfo
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.reportException
import one.mixin.android.vo.User
import one.mixin.android.vo.membershipIcon
import one.mixin.android.vo.toUser
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@AndroidEntryPoint
class SwapTransferBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SwapTransferBottomSheetDialogFragment"
        private const val ARGS_LINK = "args_link"
        private const val ARGS_IN_AMOUNT = "args_in_amount"
        private const val ARGS_IN_ASSET = "args_in_asset"
        private const val ARGS_OUT_AMOUNT = "args_out_amount"
        private const val ARGS_OUT_ASSET = "args_out_asset"
        fun newInstance(swapResult: SwapResponse, inAsset: SwapToken, outAssetItem: SwapToken): SwapTransferBottomSheetDialogFragment {
            return SwapTransferBottomSheetDialogFragment()
                .withArgs {
                    putString(ARGS_LINK, swapResult.tx)
                    putParcelable(ARGS_IN_ASSET, inAsset)
                    putParcelable(ARGS_OUT_ASSET, outAssetItem)
                    putString(ARGS_IN_AMOUNT, swapResult.quote.inAmount)
                    putString(ARGS_OUT_AMOUNT, swapResult.quote.outAmount)
                }
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    private val inAsset by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_IN_ASSET, SwapToken::class.java))
    }

    private val inAmount by lazy {
        BigDecimal(requireNotNull(requireArguments().getString(ARGS_IN_AMOUNT)))
    }

    private val outAsset by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_OUT_ASSET, SwapToken::class.java))
    }

    private val outAmount by lazy {
        BigDecimal(requireNotNull(requireArguments().getString(ARGS_OUT_AMOUNT)))
    }

    private val self by lazy {
        requireNotNull(Session.getAccount()).toUser()
    }

    private val link by lazy {
        Uri.parse(requireNotNull(requireArguments().getString(ARGS_LINK)))
    }

    private var step by mutableStateOf(Step.Pending)

    private var parsedLink: ParsedLink? = null
    private var receiver: User? by mutableStateOf(null)

    private var errorInfo: String? by mutableStateOf(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme {
                    Column(
                        modifier =
                        Modifier
                            .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(MixinAppTheme.colors.background),
                    ) {
                        Column(
                            modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
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
                                                .border(1.5.dp, MixinAppTheme.colors.background, CircleShape)
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
                                                .border(1.5.dp, MixinAppTheme.colors.background, CircleShape)
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
                                text =
                                stringResource(
                                    id =
                                    when (step) {
                                        Step.Pending -> R.string.swap_confirmation
                                        Step.Done -> R.string.web3_sending_success
                                        Step.Error -> R.string.swap_failed
                                        Step.Sending -> R.string.Sending
                                    }
                                ),
                                style =
                                TextStyle(
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.W600,
                                ),
                            )
                            Box(modifier = Modifier.height(8.dp))
                            Text(
                                modifier =
                                Modifier
                                    .padding(horizontal = 24.dp),
                                text =
                                errorInfo ?: stringResource(
                                    id =
                                    when (step) {
                                        Step.Done -> R.string.swap_message_success
                                        Step.Error -> R.string.Data_error
                                        else -> R.string.swap_inner_desc
                                    },
                                ),
                                textAlign = TextAlign.Center,
                                style =
                                TextStyle(
                                    color = if (errorInfo != null) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textMinor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W400,
                                ),
                                maxLines = 3,
                                minLines = 3,
                            )
                            Box(modifier = Modifier.height(10.dp))

                            Box(
                                modifier =
                                Modifier
                                    .height(10.dp)
                                    .fillMaxWidth()
                                    .background(MixinAppTheme.colors.backgroundWindow),
                            )
                            Box(modifier = Modifier.height(20.dp))
                            AssetChanges(title = stringResource(id = R.string.Balance_Change).uppercase(), inAmount = inAmount, inAsset = inAsset, outAmount = outAmount, outAsset = outAsset)
                            Box(modifier = Modifier.height(20.dp))
                            ItemPriceContent(title = stringResource(id = R.string.Price).uppercase(), inAmount = inAmount, inAsset = inAsset, outAmount = outAmount, outAsset = outAsset)
                            Box(modifier = Modifier.height(20.dp))
                            FeeInfo("0", BigDecimal("0"))
                            Box(modifier = Modifier.height(20.dp))
                            ItemUserContent(title = stringResource(id = R.string.Senders).uppercase(), self)
                            Box(modifier = Modifier.height(20.dp))
                            ItemUserContent(title = stringResource(id = R.string.Receivers).uppercase(), receiver)
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
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Button(
                                            onClick = {
                                                onDoneAction?.invoke()
                                                dismiss()
                                            },
                                            colors =
                                            ButtonDefaults.outlinedButtonColors(
                                                backgroundColor = MixinAppTheme.colors.accent,
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                                        ) {
                                            Text(text = stringResource(id = R.string.Done), color = Color.White)
                                        }
                                    }
                                }
                                Step.Error -> {
                                    ActionBottom(
                                        modifier =
                                        Modifier
                                            .align(Alignment.BottomCenter),
                                        cancelTitle = stringResource(R.string.Cancel),
                                        confirmTitle = stringResource(id = R.string.Retry),
                                        cancelAction = { dismiss() },
                                        confirmAction = { showPin() },
                                    )
                                }
                                Step.Pending -> {
                                    ActionBottom(
                                        modifier =
                                        Modifier
                                            .align(Alignment.BottomCenter),
                                        cancelTitle = stringResource(R.string.Cancel),
                                        confirmTitle = stringResource(id = R.string.Continue),
                                        cancelAction = { dismiss() },
                                        confirmAction = { showPin() },
                                    )
                                }

                                Step.Sending -> {

                                }
                            }
                        }
                        Box(modifier = Modifier.height(36.dp))
                    }
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                val ctx = requireContext()
                behavior?.peekHeight = ctx.realSize().y - ctx.statusBarHeight() - ctx.navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }

            parseLink()
        }

    private var onDoneAction: (() -> Unit)? = null
    private var onDestroyAction: (() -> Unit)? = null

    fun setOnDone(callback: () -> Unit): SwapTransferBottomSheetDialogFragment {
        onDoneAction = callback
        return this
    }

    fun setOnDestroy(callback: () -> Unit): SwapTransferBottomSheetDialogFragment {
        onDestroyAction = callback
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDestroyAction?.invoke()
    }

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1).setOnPinComplete { pin ->
            lifecycleScope.launch(
                CoroutineExceptionHandler { _, error ->
                    handleException(error)
                },
            ) {
                doAfterPinComplete(pin)
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    inner class ParsedLink(
        val assetId: String,
        val amount: String,
        val receiverIds: List<String>,
        val memo: String?,
        val traceId: String,
    )

    private fun parseLink() = lifecycleScope.launch {
        val assetId = requireNotNull(link.getQueryParameter("asset"))
        val amount = requireNotNull(link.getQueryParameter("amount"))
        val receiverId = requireNotNull(link.lastPathSegment)
        receiver = bottomViewModel.refreshUser(receiverId)
        val receiverIds = listOf(receiverId)
        val memo = link.getQueryParameter("memo")
        val traceId = link.getQueryParameter("trace") ?: UUID.randomUUID().toString()
        parsedLink = ParsedLink(assetId, amount, receiverIds, memo, traceId)
    }

    private fun doAfterPinComplete(pin: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val parsedLink = this@SwapTransferBottomSheetDialogFragment.parsedLink ?: return@launch
                step = Step.Sending
                val consolidationAmount = bottomViewModel.checkUtxoSufficiency(parsedLink.assetId, parsedLink.amount)
                val token = bottomViewModel.findAssetItemById(parsedLink.assetId)
                if (consolidationAmount != null && token != null) {
                    UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), token, consolidationAmount, UUID.randomUUID().toString(), null, null))
                        .show(parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
                    step = Step.Pending
                    return@launch
                } else if (token == null) {
                    errorInfo = getString(R.string.Data_error)
                    step = Step.Error
                    return@launch
                }
                val response = bottomViewModel.kernelTransaction(parsedLink.assetId, parsedLink.receiverIds, 1.toByte(), parsedLink.amount, pin, parsedLink.traceId, parsedLink.memo)
                if (response.isSuccess) {
                    defaultSharedPreferences.putLong(
                        Constants.BIOMETRIC_PIN_CHECK,
                        System.currentTimeMillis(),
                    )
                    context?.updatePinCheck()
                    step = Step.Done
                } else {
                    errorInfo = handleError(response.error) ?: response.errorDescription
                    step = Step.Error
                }

            } catch (e: Exception) {
                handleException(e)
            }
        }

    private suspend fun handleError(
        error: ResponseError?,
    ): String? {
        if (error != null) {
            val errorCode = error.code
            val errorDescription = error.description
            val errorInfo =
                when (errorCode) {
                    ErrorHandler.TOO_MANY_REQUEST -> {
                        requireContext().getString(R.string.error_pin_check_too_many_request)
                    }

                    ErrorHandler.PIN_INCORRECT -> {
                        val errorCount = bottomViewModel.errorCount()
                        requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount)
                    }

                    else -> {
                        requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
                    }
                }
            return errorInfo
        } else {
            return null
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

    fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
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

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismiss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
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

    override fun onDetach() {
        super.onDetach()
        if (activity is WalletConnectActivity || activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }
}

@Composable
fun ItemUserContent(
    title: String,
    user: User?,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(4.dp))
        if (user == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MixinAppTheme.colors.accent,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoilImage(
                    model = user.avatarUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(4.dp))
                UserBadge(user)
            }
        }
    }
}

@Composable
fun UserBadge(
    user: User,
    badgeSize: Dp = 14.dp,
) {
    val badgeResource = when {
        user.isMembership() -> user.membership.membershipIcon()
        user.isVerified == true -> R.drawable.ic_user_verified
        user.isBot() -> R.drawable.ic_bot
        else -> null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${user.fullName} (${user.identityNumber})",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
        )
        badgeResource?.let { res ->
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                modifier = Modifier
                    .size(badgeSize)
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
fun ItemPriceContent(
    title: String,
    inAmount: BigDecimal,
    inAsset: SwapToken,
    outAmount: BigDecimal,
    outAsset: SwapToken
) {
    var isSwitch by remember { mutableStateOf(false) }
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
                text = if (isSwitch) {
                    "1 ${outAsset.symbol} ≈ ${BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP).toPlainString()} ${inAsset.symbol}"
                } else {
                    "1 ${inAsset.symbol} ≈ ${price.toPlainString()} ${outAsset.symbol}"
                },
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        isSwitch = !isSwitch
                    },
                painter = painterResource(id = R.drawable.ic_price_switch),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
        Box(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun AssetChanges(
    title: String,
    inAmount: BigDecimal,
    inAsset: SwapToken,
    outAmount: BigDecimal,
    outAsset: SwapToken
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier =
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(
                model = outAsset.icon,
                modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            Box(modifier = Modifier.width(12.dp))
            Text(
                text = "+${outAmount.toPlainString()} ${outAsset.symbol}",
                color = MixinAppTheme.colors.green,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = outAsset.chain.name,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier =
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(
                model = inAsset.icon,
                modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            Box(modifier = Modifier.width(12.dp))
            Text(
                text = "-${inAmount.toPlainString()} ${inAsset.symbol}",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = inAsset.chain.name,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
        }
    }
}
