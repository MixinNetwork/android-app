package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment

@AndroidEntryPoint
class PerpetualGuideFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpetualGuideFragment"

        fun newInstance() = PerpetualGuideFragment()
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            PerpetualGuidePage(
                pop = {
                    dismiss()
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }
}
