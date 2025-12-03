package one.mixin.android.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class InputAmountBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputAmountBottomSheetDialogFragment"
        private const val ARGS_TOKEN = "args_token"
        private const val ARGS_WEB3_TOKEN = "args_web3_token"
        private const val ARGS_ADDRESS = "args_address"
        private const val ARGS_MINIMUM = "args_minimum"
        private const val ARGS_MAXIMUM = "args_maximum"

        fun newInstance(
            token: TokenItem,
            address: String? = null,
            minimum: String? = null,
            maximum: String? = null,
        ) = InputAmountBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_TOKEN, token)
            putString(ARGS_ADDRESS, address)
            putString(ARGS_MINIMUM, minimum)
            putString(ARGS_MAXIMUM, maximum)
        }

        fun newInstance(
            web3Token: Web3TokenItem,
            address: String? = null,
        ) = InputAmountBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_WEB3_TOKEN, web3Token)
            putString(ARGS_ADDRESS, address)
        }
    }

    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
    }

    private val web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
    }

    private val address by lazy {
        requireArguments().getString(ARGS_ADDRESS)
    }

    private val minimum by lazy { requireArguments().getString(ARGS_MINIMUM) }
    private val maximum by lazy { requireArguments().getString(ARGS_MAXIMUM) }

    // Calculate USD price from token
    private val price by lazy {
        val priceUsd = token?.priceUsd ?: web3Token?.priceUsd ?: "0"
        (priceUsd.toDoubleOrNull() ?: 1.0) * Fiats.getRate()
    }

    private val tokenSymbol by lazy {
        token?.symbol ?: web3Token?.symbol ?: ""
    }

    @Composable
    override fun ComposeContent() {

        var inputAmount by remember { mutableStateOf("0") }
        var isPrimaryMode by remember { mutableStateOf(true) }

        val minValue = remember(minimum) { minimum?.toBigDecimalOrNull() }
        val maxValue = remember(maximum) { maximum?.toBigDecimalOrNull() }

        val formattedPrimaryAmount = remember(inputAmount, isPrimaryMode) {
            if (isPrimaryMode) {
                formatAmount(inputAmount, tokenSymbol)
            } else {
                // Calculate primary from minor
                val minorValue = inputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val priceDecimal = price.toBigDecimal()
                val primaryValue = if (priceDecimal > BigDecimal.ZERO) {
                    minorValue.divide(priceDecimal, 8, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                } else "0"
                formatAmount(primaryValue, tokenSymbol)
            }
        }

        val formattedMinorAmount = remember(inputAmount, isPrimaryMode) {
            if (!isPrimaryMode) {
                formatAmount(inputAmount, Fiats.getAccountCurrencyAppearance())
            } else {
                // Calculate minor from primary
                val primaryValue = inputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val priceDecimal = price.toBigDecimal()
                val minorValue = if (priceDecimal > BigDecimal.ZERO) {
                    primaryValue.multiply(priceDecimal).setScale(2, RoundingMode.DOWN).toPlainString()
                } else "0"
                formatAmount(minorValue, Fiats.getAccountCurrencyAppearance())
            }
        }

        MixinAppTheme {
            InputAmountFlow(
                inputAmount = inputAmount,
                primaryAmount = if (isPrimaryMode) formattedPrimaryAmount else formattedMinorAmount,
                minorAmount = if (isPrimaryMode) formattedMinorAmount else formattedPrimaryAmount,
                tokenAmount = formattedPrimaryAmount,
                token = token,
                web3Token = web3Token,
                address = address,
                minimum = minValue,
                maximum = maxValue,
                onNumberClick = { number ->
                    val currentCurrency = if (isPrimaryMode) null else Fiats.getAccountCurrencyAppearance()
                    inputAmount = AmountInputHandler.handleNumberInput(inputAmount, number, isPrimaryMode, currentCurrency)
                    onAmountChanged?.invoke(formattedPrimaryAmount, formattedMinorAmount)
                    onNumberClick?.invoke(number)
                },
                onDeleteClick = {
                    inputAmount = AmountInputHandler.handleDeleteInput(inputAmount)
                    onAmountChanged?.invoke(formattedPrimaryAmount, formattedMinorAmount)
                    onDeleteClick?.invoke()
                },
                onSwitchClick = {
                    val currentPrimaryValue = if (isPrimaryMode) {
                        inputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    } else {
                        val minorValue = inputAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val priceDecimal = price.toBigDecimal()
                        if (priceDecimal > BigDecimal.ZERO) {
                            minorValue.divide(priceDecimal, 8, RoundingMode.DOWN)
                        } else {
                            BigDecimal.ZERO
                        }
                    }

                    isPrimaryMode = !isPrimaryMode

                    inputAmount = if (isPrimaryMode) {
                        currentPrimaryValue.stripTrailingZeros().toPlainString()
                    } else {
                        val priceDecimal = price.toBigDecimal()
                        if (priceDecimal > BigDecimal.ZERO) {
                            currentPrimaryValue.multiply(priceDecimal).setScale(2, RoundingMode.DOWN).toPlainString()
                        } else "0"
                    }

                    if (inputAmount.toBigDecimalOrNull()?.let { it < BigDecimal("0.000001") } == true) {
                        inputAmount = "0"
                    }
                    onAmountChanged?.invoke(formattedPrimaryAmount, formattedMinorAmount)
                    onSwitchClick?.invoke()
                },
                onCopyClick = { depositUri ->
                    onCopyClick?.invoke(depositUri)
                    dismiss()
                },
                onCloseClick = {
                    dismiss()
                },
                onShareClick = { depositUri ->
                    onShareClick?.invoke(formattedPrimaryAmount, depositUri)
                    dismiss()
                },
                onForward = { tokenDisplayName, depositUri, amount ->
                    onForwardClick?.invoke(buildForwardMessage(tokenDisplayName, depositUri, amount))
                    dismiss()
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    var onNumberClick: ((String) -> Unit)? = null
    var onDeleteClick: (() -> Unit)? = null
    var onSwitchClick: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    var onAmountChanged: ((primary: String, minor: String) -> Unit)? = null
    var onShareClick: ((amount: String, depositUri: String) -> Unit)? = null
    var onForwardClick: ((message: ForwardMessage) -> Unit)? = null
    var onCopyClick: ((depositUri: String) -> Unit)? = null

    override fun getTheme() = R.style.AppTheme_Dialog
    private fun buildForwardMessage(tokenDisplayName: String, url: String, amount: String): ForwardMessage {
        val description = buildString {
            append(getString(R.string.payment_details, amount, tokenDisplayName, "${Session.getAccount()?.fullName}(${Session.getAccount()?.identityNumber})"))
        }

        val actions = listOf(
            ActionButtonData(
                label = getString(R.string.pay_now),
                color = "#3D75E3",
                action = url
            ),
        )

        val appCard = AppCardData(
            appId = ROUTE_BOT_USER_ID,
            iconUrl = null,
            coverUrl = null,
            cover = null,
            title = getString(R.string.mixin_payment_title),
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = actions,
        )

        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }


    private fun formatAmount(amount: String, symbol: String): String {
        val value = amount.toDoubleOrNull() ?: 0.0
        return if (amount == "0") {
            "0 $symbol"
        } else {
            // Handle special decimal cases following CalculateFragment logic
            val formatted = when {
                amount.endsWith(".") -> {
                    val baseNumber = amount.substringBefore(".")
                    "$baseNumber. $symbol"
                }
                amount.endsWith(".00") -> {
                    val baseNumber = amount.substringBefore(".")
                    "$baseNumber.00 $symbol"
                }
                amount.endsWith(".0") -> {
                    val baseNumber = amount.substringBefore(".")
                    "$baseNumber.0 $symbol"
                }
                amount.contains(".") -> {
                    "$amount $symbol"
                }
                else -> {
                    "$amount $symbol"
                }
            }
            formatted
        }
    }

    override fun showError(error: String) {
    }
}
