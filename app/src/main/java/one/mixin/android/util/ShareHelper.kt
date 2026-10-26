package one.mixin.android.util

import android.content.Intent
import android.net.Uri
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getFileName
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.isSafeShareUri
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.vo.addTo
import one.mixin.android.vo.systemMediaToMessage

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
            val stream =
                intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
                    ?.takeIf { MixinApplication.appContext.isSafeShareUri(it) }
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text.isNullOrEmpty()) {
                    stream?.systemMediaToMessage(ForwardCategory.Data)?.addTo(result)
                } else {
                    ForwardMessage(ShareCategory.Text, text).addTo(result)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = stream ?: return result
                ForwardMessage(ShareCategory.Image, GsonHelper.customGson.toJson(ShareImageData(imageUri.toString()))).addTo(result)
            } else if (type.startsWith("video/")) {
                val videoUri = stream
                videoUri?.systemMediaToMessage(ForwardCategory.Video)?.addTo(result)
            } else if (type.startsWith("application/") || type.startsWith("audio/")) {
                stream?.let {
                    val fileName = it.getFileName()
                    it.systemMediaToMessage(
                        ForwardCategory.Data,
                        fileName,
                        type,
                    ).addTo(result)
                }
            } else {
                val dataUri = stream
                dataUri?.systemMediaToMessage(ForwardCategory.Data, dataUri.getFileName())?.addTo(result)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            val streams =
                intent.getParcelableArrayListCompat(Intent.EXTRA_STREAM, Uri::class.java)
                    ?.filter { MixinApplication.appContext.isSafeShareUri(it) }
            when {
                type.startsWith("image/") -> {
                    streams?.forEach { item ->
                        item.systemMediaToMessage(ShareCategory.Image).addTo(result)
                    }
                }
                type.startsWith("video/") -> {
                    streams?.forEach { item ->
                        item.systemMediaToMessage(ForwardCategory.Video).addTo(result)
                    }
                }
                else -> {
                    streams?.let { list ->
                        list.forEach { item ->
                            item.systemMediaToMessage(ForwardCategory.Data, item.getFileName()).addTo(result)
                        }
                    }
                }
            }
        }
        return result
    }
}
