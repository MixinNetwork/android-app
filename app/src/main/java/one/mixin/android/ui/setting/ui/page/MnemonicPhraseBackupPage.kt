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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.HighlightedTextWithClick

@Composable
fun MnemonicPhraseBackupPage(pop: () -> Unit, next: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(50.dp))
                Icon(painter = painterResource(R.drawable.ic_mnemonic_phrase), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(36.dp))
                HighlightedTextWithClick(
                    stringResource(
                        R.string.Mnemonic_Phrase_instruction,
                        stringResource(R.string.Set_up_Pin_more)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    stringResource(R.string.Set_up_Pin_more)
                ) {
                    // Todo
                }
                Spacer(modifier = Modifier.height(36.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { next.invoke() }
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow)
                        .padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.Backup_Mnemonic_Phrase), fontSize = 16.sp, color = MixinAppTheme.colors.textMinor)
                    Icon(
                        tint = Color.Unspecified,
                        contentDescription = null,
                        painter = painterResource(R.drawable.ic_arrow_gray_right)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}