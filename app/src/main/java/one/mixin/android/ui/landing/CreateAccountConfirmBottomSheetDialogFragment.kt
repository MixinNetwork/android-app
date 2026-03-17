package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
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

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp))
                    .background(MixinAppTheme.colors.background)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        modifier = Modifier.clickable {
                            dismiss()
                        },
                        painter = painterResource(id = R.drawable.ic_circle_close),
                        tint = Color.Unspecified,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Icon(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    painter = painterResource(R.drawable.ic_mnemonic_phrase_creaeting),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.create_account_confirm_title),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(48.dp))
                FeatureRow(
                    iconResId = R.drawable.ic_account_truly,
                    titleResId = R.string.feature_truly_decentralized,
                    descriptionResId = R.string.feature_truly_decentralized_description,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureRow(
                    iconResId = R.drawable.ic_account_privacy,
                    titleResId = R.string.feature_privacy_by_default,
                    descriptionResId = R.string.feature_privacy_by_default_description,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureRow(
                    iconResId = R.drawable.ic_account_all_in_one,
                    titleResId = R.string.feature_all_in_one,
                    descriptionResId = R.string.feature_all_in_one_description,
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        onCreateAccount?.invoke()
                        dismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MixinAppTheme.colors.accent),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp, vertical = 11.dp),
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

    @Composable
    private fun FeatureRow(
        iconResId: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleResId),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(descriptionResId),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
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
