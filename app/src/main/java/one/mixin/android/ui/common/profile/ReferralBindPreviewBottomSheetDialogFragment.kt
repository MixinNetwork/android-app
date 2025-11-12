package one.mixin.android.ui.common.profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getSafeAreaInsetsBottom
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.wallet.ItemUserContent
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.User

@AndroidEntryPoint
class ReferralBindPreviewBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG: String = "ReferralBindPreviewBottomSheetDialogFragment"
        private const val ARG_CODE: String = "arg_code"
        private const val ARG_INVITER: String = "arg_inviter"
        private const val ARG_PERCENT: String = "arg_percent"

        fun newInstance(code: String, inviter: User, inviteePercent: String): ReferralBindPreviewBottomSheetDialogFragment {
            return ReferralBindPreviewBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CODE, code)
                    putParcelable(ARG_INVITER, inviter)
                    putString(ARG_PERCENT, inviteePercent)
                }
            }
        }
    }

    private val viewModel: BottomSheetViewModel by viewModels()

    private var onDoneAction: (() -> Unit)? = null

    fun setOnDone(callback: () -> Unit): ReferralBindPreviewBottomSheetDialogFragment {
        onDoneAction = callback
        return this
    }

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

    enum class Step { Pending, Sending, Done, Error, Retry }

    private var step by mutableStateOf(Step.Pending)
    private var errorInfo by mutableStateOf<String?>(null)

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val code = requireNotNull(requireArguments().getString(ARG_CODE))
            val inviter = requireNotNull(requireArguments().getParcelable<User>(ARG_INVITER))
            val percent = requireNotNull(requireArguments().getString(ARG_PERCENT))
            val scope = rememberCoroutineScope()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.height(36.dp))
                when (step) {
                    Step.Pending -> {
                        Image(
                            painter = painterResource(R.drawable.ic_bind_referral),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                    Step.Sending -> {
                        CircularProgressIndicator(
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                    Step.Done -> {
                        Image(
                            painter = painterResource(R.drawable.ic_order_success),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                    Step.Error, Step.Retry -> {
                        Image(
                            painter = painterResource(R.drawable.ic_order_failed),
                            contentDescription = null,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }
                Box(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.bind_referral_code),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Box(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    HighlightedTextWithClick(
                        fullText = stringResource(R.string.bind_referral_footer),
                        modifier = Modifier.alpha(if (step == Step.Error || step == Step.Retry) 0f else 1f),
                        stringResource(R.string.Learn_More),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        onTextClick = { context?.openUrl(getString(R.string.referral_url)) }
                    )
                    if (step == Step.Error || step == Step.Retry) {
                        Text(
                            text = errorInfo ?: stringResource(R.string.Unknown),
                            color = MixinAppTheme.colors.tipError,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow)
                )
                Box(modifier = Modifier.height(20.dp))
                ItemContent(
                    title = stringResource(R.string.referral_hint).uppercase(),
                    subTitle = code,
                    subTitleFontWeight = FontWeight.W500
                )
                Box(modifier = Modifier.height(20.dp))
                ItemContent(
                    title = stringResource(R.string.Invitee_Commission).uppercase(),
                    subTitle = "${(percent.toFloatOrNull() ?: 0f) * 100f}%",
                    subTitleFontWeight = FontWeight.W500
                )
                Box(modifier = Modifier.height(20.dp))
                ItemUserContent(
                    title = stringResource(R.string.Inviter).uppercase(),
                    user = inviter,
                    address = null,
                    onClick = {
                        UserBottomSheetDialogFragment.newInstance(inviter)
                            ?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                    }
                )
                Box(modifier = Modifier.height(20.dp))
                Spacer(modifier = Modifier.weight(1f))
                when (step) {
                    Step.Done, Step.Error -> {
                        Row(
                            modifier = Modifier
                                .background(MixinAppTheme.colors.background)
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    if (step == Step.Done) {
                                        onDoneAction?.invoke()
                                    }
                                    dismiss()
                                },
                                colors = androidx.compose.material.ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = MixinAppTheme.colors.accent
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 36.dp, vertical = 11.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (step == Step.Done) {
                                            R.string.Got_it
                                        } else {
                                            R.string.Done
                                        }
                                    ), color = Color.White
                                )
                            }
                        }
                    }

                    Step.Retry -> {
                        ActionBottom(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            cancelTitle = stringResource(R.string.Cancel),
                            confirmTitle = stringResource(R.string.Retry),
                            cancelAction = { dismiss() },
                            confirmAction = { doBind(code, scope) }
                        )
                    }
                    Step.Pending -> {
                        ActionBottom(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            cancelTitle = stringResource(R.string.Cancel),
                            confirmTitle = stringResource(R.string.Confirm),
                            cancelAction = { dismiss() },
                            confirmAction = { doBind(code, scope) }
                        )
                    }
                    Step.Sending -> {
                        // No buttons during sending
                    }
                }
                Box(modifier = Modifier.height(36.dp))
            }
        }
    }

    private fun doBind(code: String, scope: CoroutineScope) {
        scope.launch {
            step = Step.Sending
            requestRouteAPI(
                invokeNetwork = {
                    viewModel.bindReferral(code)
                },
                successBlock = {
                    step = Step.Done
                    true
                },
                failureBlock = {
                    errorInfo = requireContext().getMixinErrorStringByCode(it.errorCode, it.errorDescription)
                    step = Step.Error
                    true
                },
                exceptionBlock = {
                    errorInfo = ErrorHandler.getErrorMessage(it)
                    step = Step.Retry
                    true
                },
                requestSession = {
                    viewModel.fetchSessionsSuspend(listOf(one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID))
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop() - view.getSafeAreaInsetsBottom()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (step == Step.Done) {
            onDoneAction?.invoke()
        }
    }

    override fun showError(error: String) {}
}
