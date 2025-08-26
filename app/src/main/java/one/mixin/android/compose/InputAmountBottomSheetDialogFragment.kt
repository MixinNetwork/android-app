package one.mixin.android.compose

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
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
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.util.Locale

@AndroidEntryPoint
class InputAmountBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputAmountBottomSheetDialogFragment"
        private const val ARGS_TOKEN = "args_token"

        fun newInstance(
            token: TokenItem
        ) = InputAmountBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_TOKEN, token)
        }
    }

    private val token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
            ?: throw IllegalArgumentException("TokenItem is required")
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
                        val primaryValue = if (price > 0) minorValue * price else 0.0
                        formatAmount(primaryValue.toString(), token.symbol)
                    }
                }

                val formattedMinorAmount = remember(inputAmount, isPrimaryMode) {
                    if (!isPrimaryMode) {
                        formatAmount(inputAmount, Fiats.getSymbol())
                    } else {
                        // Calculate minor from primary
                        val primaryValue = inputAmount.toDoubleOrNull() ?: 0.0
                        val minorValue = if (price > 0) primaryValue / price else 0.0
                        formatAmount(minorValue.toString(), Fiats.getSymbol())
                    }
                }

                MixinAppTheme {
                    InputAmountScreen(
                        primaryAmount = formattedPrimaryAmount,
                        minorAmount = formattedMinorAmount,
                        onNumberClick = { number ->
                            inputAmount = handleNumberInput(inputAmount, number)
                            onNumberClick?.invoke(number)
                        },
                        onDeleteClick = {
                            inputAmount = handleDeleteInput(inputAmount)
                            onDeleteClick?.invoke()
                        },
                        onSwitchClick = {
                            isPrimaryMode = !isPrimaryMode
                            // Convert current input to the other currency
                            val currentValue = inputAmount.toDoubleOrNull() ?: 0.0
                            inputAmount = if (isPrimaryMode) {
                                // Switched to primary mode, convert from minor to primary
                                if (price > 0) (currentValue / price).toString() else "0"
                            } else {
                                // Switched to minor mode, convert from primary to minor
                                if (price > 0) (currentValue * price).toString() else "0"
                            }
                            onAmountChanged?.invoke(formattedPrimaryAmount, formattedMinorAmount)
                            onSwitchClick?.invoke()
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

    private fun handleNumberInput(currentAmount: String, number: String): String {
        return when {
            number == "." -> {
                if (!currentAmount.contains(".")) {
                    if (currentAmount == "0") "0." else "$currentAmount."
                } else {
                    currentAmount
                }
            }
            currentAmount == "0" && number != "." -> {
                number
            }
            else -> {
                "$currentAmount$number"
            }
        }
    }

    private fun handleDeleteInput(currentAmount: String): String {
        return when {
            currentAmount.length <= 1 -> "0"
            else -> currentAmount.dropLast(1)
        }
    }

    private fun formatAmount(amount: String, symbol: String): String {
        val value = amount.toDoubleOrNull() ?: 0.0
        return if (value == 0.0) {
            "0 $symbol"
        } else {
            val formatted = if (value % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", value)
            } else {
                String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
            }
            "$formatted $symbol"
        }
    }

    override fun onDestroyView() {
        behavior?.removeBottomSheetCallback(bottomSheetBehaviorCallback)
        super.onDestroyView()
    }
}
