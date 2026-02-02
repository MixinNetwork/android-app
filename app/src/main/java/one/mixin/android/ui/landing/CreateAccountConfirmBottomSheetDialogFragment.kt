package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class CreateAccountConfirmBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG: String = "CreateAccountConfirmBottomSheetDialogFragment"

        fun newInstance(): CreateAccountConfirmBottomSheetDialogFragment = CreateAccountConfirmBottomSheetDialogFragment()
    }

    private var onCreateAccount: (() -> Unit)? = null
    private var onPrivacyPolicy: (() -> Unit)? = null
    private var onTermsOfService: (() -> Unit)? = null

    fun setOnCreateAccount(callback: () -> Unit): CreateAccountConfirmBottomSheetDialogFragment {
        onCreateAccount = callback
        return this
    }

    fun setOnPrivacyPolicy(callback: () -> Unit): CreateAccountConfirmBottomSheetDialogFragment {
        onPrivacyPolicy = callback
        return this
    }

    fun setOnTermsOfService(callback: () -> Unit): CreateAccountConfirmBottomSheetDialogFragment {
        onTermsOfService = callback
        return this
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Icon(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    painter = painterResource(R.drawable.ic_mnemonic_phrase_creaeting),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_title),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_1_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_1_desc),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textMinor
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_2_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_2_desc),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textMinor
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_3_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_feature_3_desc),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textMinor
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onCreateAccount?.invoke()
                        dismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MixinAppTheme.colors.accent),
                    elevation = ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    )
                ) {
                    Text(text = stringResource(R.string.create_account_confirm_action_create), color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                val privacyPolicyText = stringResource(R.string.Privacy_Policy)
                val termsOfServiceText = stringResource(R.string.Terms_of_Service)
                val landingIntroduction = stringResource(R.string.landing_introduction, privacyPolicyText, termsOfServiceText)
                HighlightedTextWithClick(
                    fullText = landingIntroduction,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    privacyPolicyText,
                    termsOfServiceText,
                    fontSize = 12.sp,
                    lineHeight = 16.8.sp,
                ) { clickedText ->
                    when (clickedText) {
                        privacyPolicyText -> onPrivacyPolicy?.invoke()
                        termsOfServiceText -> onTermsOfService?.invoke()
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }


    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

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

}
