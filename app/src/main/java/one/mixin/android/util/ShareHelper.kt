package one.mixin.android.util

import android.content.Intent
import android.net.Uri
import one.mixin.android.extension.getFileName
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableExtraCompat
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
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text.isNullOrEmpty()) {
                    intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.systemMediaToMessage(ForwardCategory.Data)?.addTo(result)
                } else {
                    ForwardMessage(ShareCategory.Text, text).addTo(result)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
                ForwardMessage(ShareCategory.Image, GsonHelper.customGson.toJson(ShareImageData(imageUri.toString()))).addTo(result)
            } else if (type.startsWith("video/")) {
                val videoUri = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
                videoUri?.systemMediaToMessage(ForwardCategory.Video)?.addTo(result)
            } else if (type.startsWith("application/") || type.startsWith("audio/")) {
                intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    val fileName = it.getFileName()
                    it.systemMediaToMessage(
                        ForwardCategory.Data,
                        fileName,
                        type,
                    ).addTo(result)
                }
            } else {
                val dataUri = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
                dataUri?.systemMediaToMessage(ForwardCategory.Data, dataUri.getFileName())?.addTo(result)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            when {
                type.startsWith("image/") -> {
                    intent.getParcelableArrayListCompat(Intent.EXTRA_STREAM, Uri::class.java)?.forEach { item ->
                        item.systemMediaToMessage(ShareCategory.Image).addTo(result)
                    }
                }
                type.startsWith("video/") -> {
                    intent.getParcelableArrayListCompat(Intent.EXTRA_STREAM, Uri::class.java)?.forEach { item ->
                        item.systemMediaToMessage(ForwardCategory.Video).addTo(result)
                    }
                }
                else -> {
                    intent.getParcelableArrayListCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { list ->
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
