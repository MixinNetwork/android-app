package one.mixin.android.util

import android.content.Intent
import android.net.Uri
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getFilePath
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.addTo

class ShareHelper {

    companion object {
        @Volatile
        private var INSTANCE: ShareHelper? = null

        fun get(): ShareHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShareHelper()
            }
    }

    fun generateForwardMessageList(intent: Intent): ArrayList<ForwardMessage>? {
        val action = intent.action
        val type = intent.type
        if (action == null || type == null) {
            return null
        }
        val result = arrayListOf<ForwardMessage>()
        if (Intent.ACTION_SEND == action) {
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ForwardMessage(MessageCategory.SIGNAL_TEXT.name, content = text).addTo(result)
            } else if (type.startsWith("image/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                generateImageMessage(imageUri)?.addTo(result)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (type.startsWith("image/")) {
                val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                for (item in imageUriList) {
                    generateImageMessage(item)?.addTo(result)
                }
            }
        }
        return result
    }

    private fun generateImageMessage(imageUri: Uri?): ForwardMessage? {
        if (imageUri == null) {
            return null
        }
        return ForwardMessage(MessageCategory.SIGNAL_IMAGE.name,
            mediaUrl = imageUri.getFilePath(MixinApplication.appContext))
    }
}