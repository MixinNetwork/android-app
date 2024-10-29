package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.components.HighlightedTextWithClick

@Composable
fun RecoveryKitPage(phoneClick: () -> Unit, mnemonicPhraseClick: () -> Unit, recoveryClick: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Recovery_Kit),
            verticalScrollable = false,
            pop = {},
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Icon(painter = painterResource(R.drawable.ic_set_up_pin), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(20.dp))
                HighlightedTextWithClick(
                    stringResource(
                        R.string.Recovery_Kit_instruction,
                        stringResource(R.string.Set_up_Pin_more)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    stringResource(R.string.Set_up_Pin_more)
                ) {

                }
                Spacer(modifier = Modifier.height(36.dp))
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MixinAppTheme.colors.backgroundWindow)
                ) {
                    ClickItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .clickable { phoneClick.invoke() }, stringResource(R.string.Mobile_Number), if (Session.hasPhone()) stringResource(R.string.Added) else stringResource(R.string.Add)
                    )
                    ClickItem(modifier = Modifier.clickable { mnemonicPhraseClick.invoke() }, stringResource(R.string.Mnemonic_Phrase),
                        if (Session.exportedSalt()) stringResource(R.string.Backed_Up) else stringResource(R.string.Backup)
                    )
                    ClickItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .clickable { recoveryClick.invoke() },
                        stringResource(R.string.Recovery_Contact), if (Session.hasEmergencyContact()) stringResource(R.string.Added) else stringResource(R.string.Add)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.Recovery_Kit_Attention), color = MixinAppTheme.colors.red, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ClickItem(modifier: Modifier, title: String, description: String) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = MixinAppTheme.colors.textMinor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(description, fontSize = 15.sp, color = MixinAppTheme.colors.textAssist)
            Icon(
                tint = Color.Unspecified,
                contentDescription = null,
                painter = painterResource(R.drawable.ic_arrow_gray_right)
            )
        }
    }
}
