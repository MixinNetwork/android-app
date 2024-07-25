package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.MiddleEllipsisText
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.toast

@Composable
fun SwapTokenPage(
    token: SwapToken,
    confirmAction: () -> Unit,
) {
    MixinAppTheme {
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                CoilImage(
                    model = token.icon,
                    modifier =
                        Modifier
                            .size(45.dp)
                            .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = token.name, fontSize = 16.sp, fontWeight = FontWeight.W500, color = MixinAppTheme.colors.textPrimary)
                    Text(text = token.symbol, fontSize = 15.sp, fontWeight = FontWeight.W400, color = MixinAppTheme.colors.textMinor)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.network), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Text(text = token.chain.name, fontSize = 14.sp, color = MixinAppTheme.colors.textMinor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.Contract), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Row(
                    modifier =
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            clipboardManager.setText(AnnotatedString(token.address))
                            toast(R.string.copied_to_clipboard)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiddleEllipsisText(text = token.address, style = TextStyle(fontSize = 14.sp, color = MixinAppTheme.colors.textMinor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy_gray),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textMinor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick =
                confirmAction,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent,
                    ),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                elevation =
                    ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
            ) {
                Text(
                    text = stringResource(id = R.string.view_on_explorer),
                    color = Color.White,
                )
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
fun SwapTokenPagePreView() {
    SwapTokenPage(token = SwapToken("1111111111111111111111111", "", 9, "Solana", "SOL", "", SwapChain("",9, "Solana", "SOL", "", ""))) {
    }
}
