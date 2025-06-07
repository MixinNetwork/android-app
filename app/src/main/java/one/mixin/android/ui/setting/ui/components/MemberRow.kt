package one.mixin.android.ui.setting.ui.components


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MemberRow(title: String, text: String, iconRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            val annotatedText = buildAnnotatedString {
                text.split(" ").forEach { word ->
                    if (word.matches(Regex("\\d+"))) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MixinAppTheme.colors.textPrimary)) {
                            append("$word ")
                        }
                    } else {
                        append("$word ")
                    }
                }
            }

            Text(
                text = annotatedText,
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textAssist
            )
        }
    }
}
