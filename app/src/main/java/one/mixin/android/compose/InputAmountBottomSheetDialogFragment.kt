package one.mixin.android.compose

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.wallet.DepositShareActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class InputAmountBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputAmountBottomSheetDialogFragment"
        private const val ARGS_TOKEN = "args_token"
        private const val ARGS_ADDRESS = "args_address"

        fun newInstance(
            token: TokenItem,
            address: String? = null
        ) = InputAmountBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_TOKEN, token)
            putString(ARGS_ADDRESS, address)
        }
    }

    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
            ?: throw IllegalArgumentException("TokenItem is required")
    }

    private val address by lazy {
        requireArguments().getString(ARGS_ADDRESS)
    }

    // Calculate USD price from token
    private val price by lazy {
        (token.priceUsd.toDoubleOrNull() ?: 1.0) * Fiats.getRate()
    }

    private var behavior: BottomSheetBehavior<*>? = null

    var onNumberClick: ((String) -> Unit)? = null
    var onDeleteClick: (() -> Unit)? = null
    var onSwitchClick: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    var onAmountChanged: ((primary: String, minor: String) -> Unit)? = null
    var onShareClick: ((amount: String, depositUri: String) -> Unit)? = null
    var onForwardClick: ((depositUri: String) -> Unit)? = null
    var onCopyClick: ((depositUri: String) -> Unit)? = null

    override fun getTheme() = R.style.AppTheme_Dialog

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

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            roundTopOrBottom(12.dp.toFloat(), top = true, bottom = false)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var inputAmount by remember { mutableStateOf("0") }
                var isPrimaryMode by remember { mutableStateOf(true) }

                val formattedPrimaryAmount = remember(inputAmount, isPrimaryMode) {
                    if (isPrimaryMode) {
                        formatAmount(inputAmount, token.symbol)
                    } else {
                        // Calculate primary from minor
                        val minorValue = inputAmount.toDoubleOrNull() ?: 0.0
                        val primaryValue = if (price > 0) minorValue / price else 0.0
                        formatAmount(primaryValue.toString(), token.symbol)
                    }
                }

                val formattedMinorAmount = remember(inputAmount, isPrimaryMode) {
                    if (!isPrimaryMode) {
                        formatAmount(inputAmount, Fiats.getAccountCurrencyAppearance())
                    } else {
                        // Calculate minor from primary
                        val primaryValue = inputAmount.toDoubleOrNull() ?: 0.0
                        val minorValue = if (price > 0) primaryValue * price else 0.0
                        formatAmount(minorValue.toString(), Fiats.getAccountCurrencyAppearance())
                    }
                }

                MixinAppTheme {
                    InputAmountFlow(
                        primaryAmount = formattedPrimaryAmount,
                        minorAmount = formattedMinorAmount,
                        token = token,
                        address = address,
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
                            isPrimaryMode = !isPrimaryMode
                            // Convert current input to the other currency when switching
                            val currentValue = inputAmount.toDoubleOrNull() ?: 0.0
                            inputAmount = if (isPrimaryMode) {
                                // Switched to primary mode, convert from minor to primary
                                if (price > 0) (currentValue / price).toString() else "0"
                            } else {
                                // Switched to minor mode, convert from primary to minor
                                if (price > 0) (currentValue * price).toString() else "0"
                            }
                            // Reset to "0" if conversion results in very small numbers
                            if (inputAmount.toDoubleOrNull()?.let { it < 0.000001 } == true) {
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
                        onForward =  { depositUri ->
                            onForwardClick?.invoke(depositUri)
                            dismiss()
                        }
                    )
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight = requireContext().screenHeight()
                behavior?.isDraggable = true
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        onDismiss?.invoke()
                        dismissAllowingStateLoss()
                    }

                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
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

    override fun onDestroyView() {
        behavior?.removeBottomSheetCallback(bottomSheetBehaviorCallback)
        super.onDestroyView()
    }
}
