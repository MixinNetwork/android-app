package one.mixin.android.util

import android.content.Intent
import android.net.Uri
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getFilePath
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.vo.addTo
import one.mixin.android.vo.systemMediaToMessage
import one.mixin.android.websocket.VideoMessagePayload

class ShareHelper {

    companion object {
        @Volatile
        private var INSTANCE: ShareHelper? = null

        fun get(): ShareHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShareHelper().also { INSTANCE = it }
            }
    }

    fun generateForwardMessageList(intent: Intent): ArrayList<ForwardMessage<ForwardCategory>>? {
        val action = intent.action
        val type = intent.type
        if (action == null || type == null) {
            return null
        }
        val result = arrayListOf<ForwardMessage<ForwardCategory>>()
        // TODO handle */*
        if (Intent.ACTION_SEND == action) {
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text.isNullOrEmpty()) {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                        it.getFilePath(MixinApplication.appContext)?.let { url ->
                            url.systemMediaToMessage(ForwardCategory.Data)?.addTo(result)
                        }
                    }
                } else {
                    ForwardMessage<ForwardCategory>(ShareCategory.Text, text).addTo(result)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                imageUri?.systemMediaToMessage(ShareCategory.Image)?.addTo(result)
            } else if (type.startsWith("video/")) {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                imageUri?.systemMediaToMessage(ForwardCategory.Video)?.addTo(result)
            } else if (type.startsWith("application/") || type.startsWith("audio/")) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    it.getFilePath(MixinApplication.appContext)?.let { url ->
                        url.systemMediaToMessage(ForwardCategory.Data)?.addTo(result)
                    }
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (type.startsWith("image/")) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { item ->
                    item.systemMediaToMessage(ShareCategory.Image)?.addTo(result)
                }
            } else if (type.startsWith("video/")) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { item ->
                    item.systemMediaToMessage(ForwardCategory.Video)?.addTo(result)
                }
            }
        }
        return result
    }

    private fun generateShareMessage(
        mediaUri: Uri?,
        category: ForwardCategory = ShareCategory.Image
    ): ForwardMessage<ForwardCategory>? {
        if (mediaUri == null) {
            return null
        }
        val mediaUrl = mediaUri.getFilePath(MixinApplication.appContext) ?: return null
        return ForwardMessage(
            category,
            GsonHelper.customGson.toJson(
                if (category is ForwardCategory.Video) {
                    VideoMessagePayload(mediaUrl)
                } else {
                    ShareImageData(mediaUrl)
                }
            )
        )
    }
}
