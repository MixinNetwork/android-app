package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.widget.components.MixinButton

@AndroidEntryPoint
class PerpsInfoBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    companion object {
        const val TAG = "PerpsInfoBottomSheetDialogFragment"
        private const val ARGS_TITLE = "args_title"
        private const val ARGS_DESCRIPTION = "args_description"

        fun newInstance(@StringRes title: Int, @StringRes description: Int) =
            PerpsInfoBottomSheetDialogFragment().withArgs {
                putInt(ARGS_TITLE, title)
                putInt(ARGS_DESCRIPTION, description)
            }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(requireArguments().getInt(ARGS_TITLE)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(requireArguments().getInt(ARGS_DESCRIPTION)),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(40.dp))
                MixinButton(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.Got_it), color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int = view.measuredHeight

    override fun showError(error: String) {}
}
