package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.components.HighlightedTextWithClick

@Composable
fun AddPhonePage(hasPhone: Boolean, next: () -> Unit) {
    val context = LocalContext.current
    MixinAppTheme {
        Column {
            MixinTopAppBar(
                title = {
                    Text(stringResource(R.string.Mobile_Number))
                },
                actions = {

                },
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(50.dp))
                Icon(modifier = Modifier.align(Alignment.CenterHorizontally), painter = painterResource(R.drawable.ic_moblie_number), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(36.dp))
                if (hasPhone) {
                    HighlightedTextWithClick(
                        stringResource(R.string.Change_Phone_desc, Session.getAccount()?.phone ?: "", stringResource(R.string.More_Information)),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        stringResource(R.string.More_Information),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    ) {
                        context.openUrl(Constants.HelpLink.TIP)
                    }
                } else {
                    HighlightedTextWithClick(
                        stringResource(R.string.Add_Phone_desc, stringResource(R.string.More_Information)),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        stringResource(R.string.More_Information),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    ) {
                        context.openUrl(Constants.HelpLink.TIP)
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
                ClickItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { next.invoke() },
                    stringResource(if (hasPhone) R.string.Change_Mobile_Number else R.string.Add_Mobile_Number), ""
                )
            }
        }
    }
}