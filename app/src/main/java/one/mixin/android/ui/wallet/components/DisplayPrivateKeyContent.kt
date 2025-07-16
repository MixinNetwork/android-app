package one.mixin.android.ui.wallet.components
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast

@Composable
fun DisplayPrivateKeyContent(
    securityContent: String?,
    pop: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Icon(
            painter = painterResource(R.drawable.ic_web3_security),
            tint = Color.Unspecified,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(R.string.Your_Private_Key),
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            color = MixinAppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.Write_it_down_on_a_piece_of_paper_and_keep_it_in_a_safe_place),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box (
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    color = MixinAppTheme.colors.backgroundWindow,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = securityContent ?: "",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            Icon(
                painter = painterResource(R.drawable.ic_paste),
                contentDescription = null,
                tint = MixinAppTheme.colors.iconGray,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clickable {
                        securityContent?.let { content ->
                            val clipboard = context.getClipboardManager()
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Private Key", content
                                )
                            )
                            toast(R.string.copied_to_clipboard)
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "•",
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.Store_in_vault_or_safe_place),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "•",
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.Dont_share_it_with_a_network),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            onClick = pop,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = ButtonDefaults.elevation(
                pressedElevation = 0.dp,
                defaultElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.Done),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}