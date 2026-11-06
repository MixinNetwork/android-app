package one.mixin.android.ui.wallet

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.widget.components.DotText
import one.mixin.android.widget.components.MixinButton

@AndroidEntryPoint
class NetworkFeeInfoBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    companion object {
        const val TAG = "NetworkFeeInfoBottomSheetDialogFragment"
        private const val ARGS_CHAIN_NAME = "args_chain_name"

        fun newInstance(chainName: String) =
            NetworkFeeInfoBottomSheetDialogFragment().withArgs {
                putString(ARGS_CHAIN_NAME, chainName)
            }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val bodyStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 36.dp, top = 40.dp, end = 36.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.Network_Fee),
                    fontSize = 22.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.network_fee_description),
                    style = bodyStyle,
                    color = MixinAppTheme.colors.textMinor,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                DotText(
                    text = stringResource(R.string.network_fee_charged_by, requireArguments().getString(ARGS_CHAIN_NAME).orEmpty()),
                    color = MixinAppTheme.colors.textMinor,
                    style = bodyStyle,
                )
                Spacer(Modifier.height(4.dp))
                DotText(
                    text = stringResource(R.string.network_fee_independent_of_amount),
                    color = MixinAppTheme.colors.textMinor,
                    style = bodyStyle,
                )
                Spacer(Modifier.height(34.dp))
                MixinButton(
                    onClick = { dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                ) {
                    Text(stringResource(R.string.Got_it), color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int = view.measuredHeight

    override fun showError(error: String) {}
}
