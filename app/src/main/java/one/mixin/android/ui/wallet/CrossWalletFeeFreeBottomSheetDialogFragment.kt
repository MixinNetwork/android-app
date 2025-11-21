package one.mixin.android.ui.wallet

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.landing.components.NumberedText
import one.mixin.android.util.SystemUIManager
import one.mixin.android.extension.dp as dip

class CrossWalletFeeFreeBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG: String = "CrossWalletFeeFreeBottomSheetDialogFragment"
        fun newInstance() = CrossWalletFeeFreeBottomSheetDialogFragment()
    }

    private var behavior: BottomSheetBehavior<*>? = null

    override fun getTheme(): Int = R.style.AppTheme_Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            roundTopOrBottom(12.dip.toFloat(), top = true, bottom = false)
            setContent {
                MixinAppTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_limit_free),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.limited_time_free),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.cross_wallet_transaction_free_description),
                            color = MixinAppTheme.colors.textMinor,
                            fontSize = 14.sp,
                            lineHeight = 19.6.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NumberedText(
                            modifier = Modifier.fillMaxWidth(),
                            numberStr = "1",
                            instructionStr = stringResource(R.string.cross_wallet_transaction_free_condition_1)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NumberedText(
                            modifier = Modifier.fillMaxWidth(),
                            numberStr = "2",
                            instructionStr = stringResource(R.string.cross_wallet_transaction_free_condition_2)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        HighlightedTextWithClick(
                            stringResource(R.string.cross_wallet_transaction_free_refund),
                            modifier = Modifier.align(Alignment.Start),
                            stringResource(R.string.more_information),
                            textAlign = TextAlign.Start,
                            color = MixinAppTheme.colors.textMinor,
                            fontSize = 14.sp,
                            lineHeight = 19.6.sp,
                        ) {
                            context.openUrl(requireContext().getString(R.string.url_cross_wallet_transaction_free))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { dismiss() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = MixinAppTheme.colors.accent,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                        ) {
                            Text(text = stringResource(R.string.Got_it), color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight = 600.dip
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }

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

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }
}
