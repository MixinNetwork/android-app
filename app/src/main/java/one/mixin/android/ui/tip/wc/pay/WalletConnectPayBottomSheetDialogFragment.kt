package one.mixin.android.ui.tip.wc.pay

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.reown.walletkit.client.Wallet
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletConnectActivity
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.SystemUIManager
import timber.log.Timber

@AndroidEntryPoint
class WalletConnectPayBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletConnectPayBottomSheetDialogFragment"
        private const val ARGS_PAYMENT_LINK = "args_payment_link"

        fun newInstance(paymentLink: String) =
            WalletConnectPayBottomSheetDialogFragment().withArgs {
                putString(ARGS_PAYMENT_LINK, paymentLink)
            }
    }

    enum class Step { Loading, SelectOption, CollectData, Done, Error }

    private val viewModel by viewModels<WalletConnectPayViewModel>()
    private val paymentLink: String by lazy { requireArguments().getString(ARGS_PAYMENT_LINK, "") }

    private var step by mutableStateOf(Step.Loading)
    private var errorInfo: String? by mutableStateOf(null)
    private var paymentOptions: Wallet.Model.PaymentOptionsResponse? by mutableStateOf(null)
    private var selectedOptionIndex by mutableIntStateOf(0)
    private var collectDataUrl: String? by mutableStateOf(null)

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            when (step) {
                Step.Loading -> LoadingContent()
                Step.SelectOption -> {
                    val options = paymentOptions
                    if (options != null) {
                        SelectOptionContent(options)
                    } else {
                        LoadingContent()
                    }
                }
                Step.CollectData -> {
                    val url = collectDataUrl
                    if (url != null) {
                        CollectDataContent(url)
                    } else {
                        LoadingContent()
                    }
                }
                Step.Done -> DoneContent()
                Step.Error -> ErrorContent(errorInfo)
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        }
    }

    @Composable
    private fun SelectOptionContent(options: Wallet.Model.PaymentOptionsResponse) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.wc_payment),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            options.info?.let { info ->
                info.merchant.name.let { merchantName ->
                    Text(
                        text = merchantName,
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
                info.amount.let { amount ->
                    Text(
                        text = amount.formatDisplay(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.select_payment_option),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            options.options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedOptionIndex == index)
                                Color(0xFF3D75E3).copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        )
                        .clickable { selectedOptionIndex = index }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = selectedOptionIndex == index,
                        onClick = { selectedOptionIndex = index },
                    )
                    Column {
                        Text(
                            text = option.amount.formatDisplay(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = option.account,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onOptionSelected() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = stringResource(R.string.Pay))
            }
            Button(
                onClick = { dismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = stringResource(R.string.Cancel))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun CollectDataContent(url: String) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wc_payment),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_close_black),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { dismiss() },
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onDataCollectionComplete(json: String) {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        onCollectDataComplete(json)
                                    }
                                }
                            },
                            "AndroidWallet",
                        )
                        loadUrl(url)
                    }
                },
            )
        }
    }

    @Composable
    private fun DoneContent() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_transfer_done),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF26A17B),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.payment_success),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { dismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = stringResource(R.string.Done))
            }
        }
    }

    @Composable
    private fun ErrorContent(error: String?) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.payment_failed),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
            )
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { dismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = stringResource(R.string.Done))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchPaymentOptions()
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is WalletConnectActivity) {
            if (parentFragmentManager.fragments.isEmpty()) {
                activity?.finish()
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int =
        requireContext().screenHeight() - view.getSafeAreaInsetsTop()

    override fun showError(error: String) {}

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(window, !requireContext().booleanFromAttribute(R.attr.flag_night))
        }
    }

    private fun fetchPaymentOptions() {
        val exHandler = CoroutineExceptionHandler { _, e ->
            Timber.e(e)
            errorInfo = e.message
            step = Step.Error
        }
        lifecycleScope.launch(exHandler) {
            val options = viewModel.getPaymentOptions(paymentLink)
            paymentOptions = options
            step = Step.SelectOption
        }
    }

    private fun onOptionSelected() {
        val options = paymentOptions ?: return
        val option = options.options.getOrNull(selectedOptionIndex) ?: return
        val collectData = option.collectData
        if (collectData != null) {
            collectDataUrl = collectData.url
            step = Step.CollectData
        } else {
            showPin(options.paymentId, option.id)
        }
    }

    private fun onCollectDataComplete(json: String) {
        val options = paymentOptions ?: return
        val option = options.options.getOrNull(selectedOptionIndex) ?: return
        showPin(options.paymentId, option.id)
    }

    private fun showPin(paymentId: String, optionId: String) {
        PinInputBottomSheetDialogFragment
            .newInstance(biometricInfo = BiometricInfo(getString(R.string.Verify_by_Biometric), "", ""), from = 1)
            .setOnPinComplete { pin ->
                lifecycleScope.launch(
                    CoroutineExceptionHandler { _, e ->
                        handleException(e)
                    },
                ) {
                    doPayment(pin, paymentId, optionId)
                }
            }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private suspend fun doPayment(pin: String, paymentId: String, optionId: String) {
        step = Step.Loading
        try {
            val priv = withContext(Dispatchers.IO) { viewModel.getWeb3Priv(requireContext(), pin) }
            val actions = viewModel.getRequiredPaymentActions(paymentId, optionId)
            viewModel.signAndConfirm(priv, paymentId, optionId, actions)
            step = Step.Done
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(e: Throwable) {
        errorInfo = when (e) {
            is TipNetworkException -> "code: ${e.error.code}, message: ${e.error.description}"
            else -> e.message ?: e.stackTraceToString()
        }
        Timber.e(e)
        step = Step.Error
    }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
}

private fun Wallet.Model.PaymentAmount.formatDisplay(): String {
    val symbol = display?.assetSymbol ?: unit.substringAfterLast("/")
    val decimals = display?.decimals ?: 0
    val formatted = if (decimals > 0) {
        BigDecimal(value).movePointLeft(decimals).stripTrailingZeros().toPlainString()
    } else {
        value
    }
    return "$formatted $symbol"
}
