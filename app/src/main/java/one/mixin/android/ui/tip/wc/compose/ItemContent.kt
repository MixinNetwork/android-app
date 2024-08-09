package one.mixin.android.ui.tip.wc.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun ItemContent(
    title: String,
    subTitle: String,
    footer: String? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(4.dp))
        Text(
            text = subTitle,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        if (footer != null) {
            Box(modifier = Modifier.height(4.dp))
            Text(
                text = footer,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
        }
    }
}
