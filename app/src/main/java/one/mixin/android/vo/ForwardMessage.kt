package one.mixin.android.vo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.websocket.AudioMessagePayload
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.DataMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import one.mixin.android.websocket.toJson
import one.mixin.android.websocket.toLocationData
import java.io.File
import java.util.UUID

@SuppressLint("ParcelCreator")
@Parcelize
data class ForwardMessage(
    val category: ForwardCategory,
    val content: String,
    val messageId: String? = null
) : Parcelable

sealed class ForwardCategory : Parcelable {
    @Parcelize
    object Sticker : ForwardCategory() {
        override fun toString() = "Sticker"
    }

    @Parcelize
    object Video : ForwardCategory() {
        override fun toString() = "Video"
    }

    @Parcelize
    object Data : ForwardCategory() {
        override fun toString() = "Data"
    }

    @Parcelize
    object Audio : ForwardCategory() {
        override fun toString() = "Audio"
    }

    @Parcelize
    object Location : ForwardCategory() {
        override fun toString() = "Location"
    }

    @Parcelize
    object Transcript : ForwardCategory() {
        override fun toString() = "Transcript"
    }
}

sealed class ShareCategory : ForwardCategory() {
    @Parcelize
    object Text : ShareCategory() {
        override fun toString() = "Text"
    }

    @Parcelize
    object Post : ShareCategory() {
        override fun toString() = "Post"
    }

    @Parcelize
    object Image : ShareCategory() {
        override fun toString() = "Image"
        fun getErrorStringOrNull(code: Int) =
            when (code) {
                -1 -> R.string.error_image
                -2 -> R.string.error_format
                else -> null
            }
    }

    @Parcelize
    object Live : ShareCategory() {
        override fun toString() = "Live"
    }

    @Parcelize
    object Contact : ShareCategory() {
        override fun toString() = "Contact"
    }

    @Parcelize
    object AppCard : ShareCategory() {
        override fun toString() = "App_Card"
    }

    @Parcelize
    object Transcript : ShareCategory() {
        override fun toString() = "Transcript"
    }
}

sealed class ForwardAction(
    open val conversationId: String? = null,
    open val name: String? = null
) : Parcelable {
    @Parcelize
    data class System(
        override val conversationId: String? = null,
        override val name: String? = null
    ) : ForwardAction()

    @Parcelize
    data class Combine(
        override val conversationId: String? = null,
        override val name: String? = null
    ) : ForwardAction()

    sealed class App(
        override val conversationId: String? = null,
        override val name: String? = null
    ) : ForwardAction(conversationId, name) {
        @Parcelize
        data class Resultful(
            override val conversationId: String? = null,
            override val name: String? = null
        ) : App()

        @Parcelize
        data class Resultless(
            override val conversationId: String? = null,
            override val name: String? = null
        ) : App()
    }
}

fun ForwardMessage.addTo(list: MutableList<ForwardMessage>) {
    list.add(this)
}

inline fun <reified T : ForwardCategory> Uri.systemMediaToMessage(category: T): ForwardMessage? {
    val url = this.getFilePath(MixinApplication.appContext) ?: return null
    return url.systemMediaToMessage(category)
}

inline fun <reified T : ForwardCategory> String.systemMediaToMessage(
    category: T,
    name: String? = null,
    mimeType: String? = null,
): ForwardMessage {
    val content = when (category) {
        ShareCategory.Image -> ShareImageData(this).toJson()
        ForwardCategory.Video -> VideoMessagePayload(this).toJson()
        ForwardCategory.Data -> {
            val attachment = MixinApplication.get().getAttachment(this.toUri(), mimeType)
            attachment?.toDataMessagePayload(name)?.toJson()
        }
        else -> null
    } ?: ""
    return ForwardMessage(category, content)
}

val ShareCategories = arrayOf(
    ShareCategory.Text,
    ShareCategory.Image,
    ShareCategory.Live,
    ShareCategory.Contact,
    ShareCategory.Post,
    ShareCategory.AppCard,
    ShareCategory.Transcript
)
val ForwardCategories = arrayOf(
    ShareCategories,
    ForwardCategory.Sticker,
    ForwardCategory.Video,
    ForwardCategory.Data,
    ForwardCategory.Audio,
    ForwardCategory.Location
)

fun String.getShareCategory() =
    ShareCategories.find { this.contains(it.toString(), true) }

fun String.getForwardCategory() =
    ForwardCategories.any { this.contains(it.toString(), true) }

fun generateForwardMessage(m: Message): ForwardMessage? {
    return when {
        m.category.endsWith("_TEXT") ->
            m.content.notNullWithElse<String, ForwardMessage?>(
                { c ->
                    ForwardMessage(ShareCategory.Text, c, m.id)
                },
                { null }
            )
        m.category.endsWith("_IMAGE") ->
            m.mediaUrl.notNullWithElse<String, ForwardMessage?>(
                {
                    ForwardMessage(
                        ShareCategory.Image,
                        ShareImageData(
                            requireNotNull(m.absolutePath()),
                            m.content
                        ).toJson()
                    )
                },
                { null }
            )
        m.category.endsWith("_DATA") -> {
            if (m.absolutePath()?.fileExists() != true) {
                return null
            }
            m.name ?: return null
            m.mediaMimeType ?: return null
            m.mediaSize ?: return null
            val dataMessagePayload = DataMessagePayload(
                requireNotNull(m.absolutePath()),
                m.name,
                m.mediaMimeType,
                m.mediaSize,
                m.content,
            )
            ForwardMessage(
                ForwardCategory.Data,
                dataMessagePayload.toJson(),
                m.id
            )
        }
        m.category.endsWith("_VIDEO") -> {
            if (m.absolutePath()?.fileExists() != true) {
                return null
            }
            val videoData = VideoMessagePayload(
                requireNotNull(m.absolutePath()),
                UUID.randomUUID().toString(),
                nowInUtc(),
                m.content,
            )
            ForwardMessage(ForwardCategory.Video, videoData.toJson(), m.id)
        }
        m.category.endsWith("_CONTACT") -> {
            val shareUserId = m.sharedUserId ?: return null
            val contactData = ContactMessagePayload(shareUserId)
            ForwardMessage(ShareCategory.Contact, contactData.toJson(), m.id)
        }
        m.category.endsWith("_STICKER") -> {
            val stickerData = StickerMessagePayload(
                name = m.name,
                stickerId = m.stickerId
            )
            ForwardMessage(ForwardCategory.Sticker, stickerData.toJson(), m.id)
        }
        m.category.endsWith("_AUDIO") -> {
            val url = m.absolutePath() ?: return null
            if (!File(url.getFilePath()).exists()) return null
            val duration = m.mediaDuration?.toLongOrNull() ?: return null
            val waveForm = m.mediaWaveform ?: return null
            val audioData = AudioMessagePayload(
                UUID.randomUUID().toString(),
                url,
                duration,
                waveForm,
                m.content,
            )
            ForwardMessage(ForwardCategory.Audio, audioData.toJson(), m.id)
        }
        m.category.endsWith("_LIVE") -> {
            if (m.mediaWidth == null ||
                m.mediaWidth == 0 ||
                m.mediaHeight == null ||
                m.mediaHeight == 0 ||
                m.mediaUrl.isNullOrBlank()
            ) {
                return null
            }
            val shareable = try {
                getTypeAdapter<LiveMessagePayload>(LiveMessagePayload::class.java).fromJson(requireNotNull(m.content))?.shareable
            } catch (e: Exception) {
                null
            }
            val liveData = LiveMessagePayload(
                m.mediaWidth,
                m.mediaHeight,
                m.thumbUrl ?: "",
                m.mediaUrl,
                shareable
            )
            ForwardMessage(ShareCategory.Live, liveData.toJson(), m.id)
        }
        m.category.endsWith("_POST") ->
            m.content.notNullWithElse<String, ForwardMessage?>(
                { c ->
                    ForwardMessage(ShareCategory.Post, c, m.id)
                },
                { null }
            )
        m.category.endsWith("_LOCATION") ->
            m.content.notNullWithElse<String, ForwardMessage?>(
                { c ->
                    ForwardMessage(
                        ForwardCategory.Location,
                        toLocationData(c)?.toJson() ?: "",
                        m.id
                    )
                },
                { null }
            )
        m.category == MessageCategory.APP_CARD.name ->
            m.content.notNullWithElse<String, ForwardMessage?>(
                { c ->
                    ForwardMessage(ShareCategory.AppCard, c, m.id)
                },
                { null }
            )
        m.category.endsWith("_TRANSCRIPT") ->
            m.content.notNullWithElse<String, ForwardMessage?>(
                { c ->
                    ForwardMessage(ForwardCategory.Transcript, c, m.id)
                },
                { null }
            )
        else -> null
    }
}
