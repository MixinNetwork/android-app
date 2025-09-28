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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.constraintlayout.compose.Dimension
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.datetime.Month
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.ActionButton
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.landing.components.NumberedText
import one.mixin.android.ui.setting.member.MixinMemberUpgradeBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.extension.dp as dip

@AndroidEntryPoint
class ReferralBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "InputReferralBottomSheetDialogFragment"

        fun newInstance() = ReferralBottomSheetDialogFragment()
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
                    var input by remember { mutableStateOf("") }

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
                            text = stringResource(R.string.Mixin_Referral),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.W600,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.referral_description),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Column {
                            NumberedText(
                                modifier = Modifier
                                    .fillMaxWidth(), numberStr = "1", instructionStr = stringResource(R.string.referral_commission)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NumberedText(
                                modifier = Modifier
                                    .fillMaxWidth(), numberStr = "2", instructionStr = stringResource(R.string.referral_member_rebate)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NumberedText(
                                modifier = Modifier
                                    .fillMaxWidth(), numberStr = "3", instructionStr = stringResource(R.string.referral_lifetime),
                                color = MixinAppTheme.colors.red
                            )
                        }

                        HighlightedTextWithClick(
                            stringResource(R.string.referral_paid_only, stringResource(R.string.Learn_More)),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            stringResource(R.string.Learn_More),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        ) {
                            // Todo
                            context.openUrl(Constants.HelpLink.TIP)
                        }

                        Spacer(Modifier.weight(1f))

                        ActionButton(
                            text = stringResource(R.string.Upgrade_Now),
                            onClick = {
                                MixinMemberUpgradeBottomSheetDialogFragment.newInstance().showNow(parentFragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG)
                                dismissNow()
                            },
                            backgroundColor = MixinAppTheme.colors.accent,
                            contentColor = Color.White,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.Not_Now),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.fillMaxWidth().padding(13.dp).clickable {
                                dismissNow()
                            }
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
