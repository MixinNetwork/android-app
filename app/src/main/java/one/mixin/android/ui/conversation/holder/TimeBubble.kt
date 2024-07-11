package one.mixin.android.ui.conversation.holder

import android.graphics.drawable.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus

@Composable
fun TimeBubble(
    modifier:Modifier,
    createdAt: String,
    isMe: Boolean = false,
    status: String? = null,
    isPin: Boolean = false,
    isRepresentative: Boolean = false,
    isSecret: Boolean = false,
    isWhite: Boolean = false
) {
    Row(
        modifier = modifier
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (isPin) {
            Image(
                painter = painterResource(
                    id = if (isWhite) R.drawable.ic_chat_pin_white else R.drawable.ic_chat_pin
                ),
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (isRepresentative) {
            Image(
                painter = painterResource(
                    id = if (isWhite) R.drawable.ic_chat_representative_white else R.drawable.ic_chat_representative
                ),
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (isSecret) {
            Image(
                painter = painterResource(
                    id = if (isWhite) R.drawable.ic_chat_secret_white else R.drawable.ic_chat_secret
                ),
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = createdAt.timeAgoClock(),
            color = if (isWhite) Color.White else Color(0xFF83919EL),
            fontSize = 10.sp,
        )
        if (isMe && status != null) {
            Spacer(modifier = Modifier.width(2.dp))
            val statusIcon = when (status) {
                MessageStatus.SENDING.name -> if (isWhite) R.drawable.ic_status_sending_white_static else R.drawable.ic_status_sending_static
                MessageStatus.SENT.name -> if (isWhite) R.drawable.ic_status_sent_white else R.drawable.ic_status_sent
                MessageStatus.DELIVERED.name -> if (isWhite) R.drawable.ic_status_delivered_white else R.drawable.ic_status_delivered
                MessageStatus.READ.name -> R.drawable.ic_status_read
                else -> null
            }
            statusIcon?.let {
                val drawable = painterResource(id = it)
                Image(
                    painter = drawable,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                if (status == MessageStatus.SENDING.name && drawable is Animatable) {
                    LaunchedEffect(Unit) { (drawable as Animatable).start() }
                }
            }
        }
    }
}
