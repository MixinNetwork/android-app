package one.mixin.android.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MiddleEllipsisText(
    text: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 12,
    style: TextStyle = TextStyle.Default
) {
    val displayText = remember(text) {
        if (text.length <= maxLength) {
            text
        } else {
            val keep = (maxLength - 3) / 2
            text.take(keep) + "..." + text.takeLast(keep)
        }
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}