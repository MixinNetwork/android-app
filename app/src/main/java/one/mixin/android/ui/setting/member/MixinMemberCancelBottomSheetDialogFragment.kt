package one.mixin.android.ui.setting.member

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.setting.ui.page.MixinMemberCancelPage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class MixinMemberCancelBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MixinMemberCancelBottomSheetDialogFragment"

        fun newInstance(order: MembershipOrder): MixinMemberCancelBottomSheetDialogFragment {
            return MixinMemberCancelBottomSheetDialogFragment().apply {
                withArgs {
                    putParcelable("order", order)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    val order by lazy {
        requireArguments().getParcelableCompat("order", MembershipOrder::class.java)
            ?: throw IllegalArgumentException("Member order is required")
    }

    @Composable
    override fun ComposeContent() {
        MixinMemberCancelPage(
            order = order,
            onClose = { dismiss() },
        )
    }

    override fun getBottomSheetHeight(view: View): Int {
        return 450.dp + requireContext().navigationBarHeight()
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
            ViewGroup.LayoutParams.WRAP_CONTENT,
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

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

}
