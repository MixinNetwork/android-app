package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import one.mixin.android.R
import one.mixin.android.vo.MessageStatus

interface ShareMessageRenderer {
    fun setStatusIcon(
        context: Context,
        status: String,
        isSecret: Boolean,
        isWhite: Boolean,
        handleAction: (Drawable?, Drawable?) -> Unit
    ) {
        val secretIcon = if (isSecret) {
            if (isWhite) {
                AppCompatResources.getDrawable(context, R.drawable.ic_secret_white)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.ic_secret)
            }
        } else {
            null
        }
        val statusIcon: Drawable? =
            when (status) {
                MessageStatus.SENDING.name ->
                    AppCompatResources.getDrawable(
                        context,
                        if (isWhite) {
                            R.drawable.ic_status_sending_white
                        } else {
                            R.drawable.ic_status_sending
                        }
                    )
                MessageStatus.SENT.name ->
                    AppCompatResources.getDrawable(
                        context,
                        if (isWhite) {
                            R.drawable.ic_status_sent_white
                        } else {
                            R.drawable.ic_status_sent
                        }
                    )
                MessageStatus.DELIVERED.name ->
                    AppCompatResources.getDrawable(
                        context,
                        if (isWhite) {
                            R.drawable.ic_status_delivered_white
                        } else {
                            R.drawable.ic_status_delivered
                        }
                    )
                MessageStatus.READ.name ->
                    AppCompatResources.getDrawable(context, R.drawable.ic_status_read)
                else -> null
            }
        handleAction(statusIcon, secretIcon)
    }
}
