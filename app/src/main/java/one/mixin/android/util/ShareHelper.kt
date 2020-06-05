package one.mixin.android.util

import android.content.Intent
import android.net.Uri
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getFilePath
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.addTo

class ShareHelper {

    companion object {
        @Volatile
        private var INSTANCE: ShareHelper? = null

        fun get(): ShareHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShareHelper().also { INSTANCE = it }
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
                if (text.isNullOrEmpty()) {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                        ForwardMessage(
                            ForwardCategory.DATA.name,
                            mediaUrl = it.getFilePath(MixinApplication.appContext)
                        ).addTo(result)
                    }
                } else {
                    ForwardMessage(ForwardCategory.TEXT.name, content = text).addTo(result)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val fm = generateShareMessage(imageUri)
                fm?.mimeType = type
                fm?.addTo(result)
            } else if (type.startsWith("video/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                generateShareMessage(imageUri, ForwardCategory.VIDEO.name)?.addTo(result)
            } else if (type.startsWith("application/") || type.startsWith("audio/")) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    ForwardMessage(
                        ForwardCategory.DATA.name,
                        mediaUrl = it.getFilePath(MixinApplication.appContext)
                    ).addTo(result)
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (type.startsWith("image/")) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { item ->
                    generateShareMessage(item)?.addTo(result)
                }
            } else if (type.startsWith("video/")) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { item ->
                    generateShareMessage(item, ForwardCategory.VIDEO.name)?.addTo(result)
                }
            }
        }
        return result
    }

    private fun generateShareMessage(imageUri: Uri?, type: String = ForwardCategory.IMAGE.name): ForwardMessage? {
        if (imageUri == null) {
            return null
        }
        val imageUrl = imageUri.getFilePath(MixinApplication.appContext) ?: return null
        return ForwardMessage(type, mediaUrl = imageUrl)
    }
}
