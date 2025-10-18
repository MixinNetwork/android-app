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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.compose.MaterialInputField
import one.mixin.android.ui.home.web3.components.ActionButton
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.landing.components.NumberedText
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.extension.dp as dip
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI

private sealed class UiState {
    object Initial : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Failure(val error: String?) : UiState()
}

@AndroidEntryPoint
class InputReferralBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputReferralBottomSheetDialogFragment"
        private const val ARG_DEFAULT_VALUE = "default_value"

        fun newInstance(defaultValue: String = "") =
            InputReferralBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEFAULT_VALUE, defaultValue)
                }
            }
    }

    var onConfirm: ((String) -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    private val viewModel: BottomSheetViewModel by viewModels()

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                requireContext().resources.configuration.uiMode and 0x30 != 0x20
            )
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val defaultValue = arguments?.getString(ARG_DEFAULT_VALUE) ?: ""
            var input by remember { mutableStateOf(defaultValue) }
            var uiState by remember { mutableStateOf<UiState>(UiState.Initial) }
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                when (val state = uiState) {
                    is UiState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
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
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_order_success),
                                    contentDescription = null
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.referral_code_applied),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.W600,
                                    color = MixinAppTheme.colors.textPrimary,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .imePadding(),
                        ) {
                            Spacer(Modifier.height(27.dp))
                            Text(
                                text = stringResource(R.string.referral_code_applied_header),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W400,
                                color = MixinAppTheme.colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Column {
                                NumberedText(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    numberStr = "1",
                                    instructionStr = stringResource(R.string.referral_program_introduction_1)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NumberedText(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    numberStr = "2",
                                    instructionStr = stringResource(R.string.referral_program_introduction_2)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NumberedText(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    numberStr = "3",
                                    instructionStr = stringResource(R.string.referral_program_introduction_3),
                                    color = MixinAppTheme.colors.red
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            HighlightedTextWithClick(
                                stringResource(
                                    R.string.referral_code_applied_footer,
                                    stringResource(R.string.Learn_More)
                                ),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                stringResource(R.string.Learn_More),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Start
                            ) {
                                context?.openUrl(getString(R.string.referral_url))
                            }

                            Spacer(Modifier.weight(1f))

                            ActionButton(
                                text = stringResource(R.string.got_it),
                                onClick = {
                                    dismiss()
                                },
                                backgroundColor = MixinAppTheme.colors.accent,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.Upgrade),
                                color = MixinAppTheme.colors.accent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clickable {
                                        MixinMemberUpgradeBottomSheetDialogFragment.newInstance()
                                            .showNow(
                                                parentFragmentManager,
                                                MixinMemberUpgradeBottomSheetDialogFragment.TAG
                                            )
                                        dismissNow()
                                    }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    else -> {
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
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .imePadding(), horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(30.dp))
                            Text(
                                text = stringResource(R.string.apply_referral_code),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600,
                                color = MixinAppTheme.colors.textPrimary,
                            )

                            Spacer(Modifier.height(30.dp))
                            MaterialInputField(
                                value = input,
                                onValueChange = {
                                    input = it.uppercase()
                                    if (uiState is UiState.Failure) {
                                        uiState = UiState.Initial
                                    }
                                },
                                hint = stringResource(R.string.referral_hint),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(id = R.string.apply_referral_code_benefit),
                                color = MixinAppTheme.colors.textRemarks,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                textAlign = TextAlign.Start,
                            )

                            Spacer(Modifier.weight(1f))

                            if (state is UiState.Failure) {
                                Text(
                                    text = state.error
                                        ?: stringResource(id = R.string.Unknown),
                                    color = MixinAppTheme.colors.red,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            if (state is UiState.Loading) {
                                CircularProgressIndicator(
                                    color = MixinAppTheme.colors.iconGray,
                                    modifier = Modifier.padding(13.dp)
                                )
                                Spacer(Modifier.height(40.dp))
                            } else {
                                ActionButton(
                                    text = stringResource(R.string.Confirm),
                                    enabled = input.trim().length >= 8,
                                    onClick = {
                                        scope.launch {
                                            uiState = UiState.Loading
                                            requestRouteAPI(
                                                invokeNetwork = {
                                                    viewModel.bindReferral(
                                                        input.trim()
                                                    )
                                                },
                                                successBlock = {
                                                    uiState = UiState.Success
                                                    true
                                                },
                                                failureBlock = {
                                                    uiState =
                                                        UiState.Failure(it.errorDescription)
                                                    true
                                                },
                                                exceptionBlock = {
                                                    uiState =
                                                        UiState.Failure(it.localizedMessage)
                                                    true
                                                },
                                                requestSession = {
                                                    viewModel.fetchSessionsSuspend(
                                                        listOf(ROUTE_BOT_USER_ID)
                                                    )
                                                },
                                            )
                                        }
                                    },
                                    backgroundColor = MixinAppTheme.colors.accent,
                                    disabledBackgroundColor = MixinAppTheme.colors.backgroundGray,
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White,
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .padding(horizontal = 20.dp)
                                        .fillMaxWidth()
                                )

                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.Later),
                                    color = MixinAppTheme.colors.accent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W500,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                dismissNow()
                                            })
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().resources.configuration.uiMode.and(0x30).equals(0x20)
            )
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

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }
}
