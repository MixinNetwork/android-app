package one.mixin.android.ui.common.profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.compose.MaterialInputField
import one.mixin.android.ui.home.web3.components.ActionButton
import one.mixin.android.util.SystemUIManager
import one.mixin.android.extension.dp as dip

@AndroidEntryPoint
class InputReferralBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputReferralBottomSheetDialogFragment"
        private const val ARG_DEFAULT_VALUE = "default_value"

        fun newInstance(defaultValue: String = "") = InputReferralBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_DEFAULT_VALUE, defaultValue)
            }
        }
    }

    private var behavior: BottomSheetBehavior<*>? = null

    var onConfirm: ((String) -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().resources.configuration.uiMode and 0x30 != 0x20)
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            roundTopOrBottom(12.dip.toFloat(), top = true, bottom = false)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme {
                    val defaultValue = arguments?.getString(ARG_DEFAULT_VALUE) ?: ""
                    var input by remember { mutableStateOf(defaultValue) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Color.Transparent, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(R.drawable.bg_referral),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Image(
                                painter = painterResource(R.drawable.icon_referral),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            )
                        }
                        Spacer(Modifier.height(30.dp))
                        Text(
                            text = stringResource(R.string.Apply_Referral_Code),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.W600,
                            color = MixinAppTheme.colors.textPrimary,
                        )


                        MaterialInputField(
                            value = input,
                            onValueChange = { input = it },
                            hint = stringResource(R.string.referral_hint),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.weight(1f))

                        ActionButton(
                            text = stringResource(R.string.Confirm),
                            onClick = {
                                if (input.isNotBlank()) {
                                    onConfirm?.invoke(input.trim())
                                    dismiss()
                                }
                            },
                            backgroundColor = MixinAppTheme.colors.accent,
                            contentColor = Color.White,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                        )
                    }
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


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(window, !requireContext().resources.configuration.uiMode.and(0x30).equals(0x20))
        }
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    onDismissCallback?.invoke()
                    dismissAllowingStateLoss()
                }

                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onDestroyView() {
        behavior?.removeBottomSheetCallback(bottomSheetBehaviorCallback)
        super.onDestroyView()
    }
}
