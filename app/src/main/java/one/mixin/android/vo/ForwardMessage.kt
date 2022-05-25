package one.mixin.android.vo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.GsonHelper
import one.mixin.android.websocket.AudioMessagePayload
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.DataMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import one.mixin.android.websocket.toLocationData
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
                -1 -> R.string.File_error
                -2 -> R.string.Format_not_supported
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
        override val name: String? = null,
        val needEdit: Boolean = true
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

inline fun <reified T : ForwardCategory> Uri.systemMediaToMessage(
    category: T,
    name: String? = null,
    mimeType: String? = null,
): ForwardMessage =
    ForwardMessage(
        category,
        GsonHelper.customGson.toJson(
            when (category) {
                ShareCategory.Image -> ShareImageData(this.toString())
                ForwardCategory.Video -> VideoMessagePayload(this.toString())
                ForwardCategory.Data -> {
                    val attachment = MixinApplication.get().getAttachment(this, mimeType)
                    attachment?.toDataMessagePayload(name)
                }
                else -> null
            }
        )
    )

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
                        GsonHelper.customGson.toJson(
                            ShareImageData(
                                requireNotNull(m.absolutePath()),
                                m.content
                            )
                        )
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
                GsonHelper.customGson.toJson(dataMessagePayload),
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
            ForwardMessage(ForwardCategory.Video, GsonHelper.customGson.toJson(videoData), m.id)
        }
        m.category.endsWith("_CONTACT") -> {
            val shareUserId = m.sharedUserId ?: return null
            val contactData = ContactMessagePayload(shareUserId)
            ForwardMessage(ShareCategory.Contact, GsonHelper.customGson.toJson(contactData), m.id)
        }
        m.category.endsWith("_STICKER") -> {
            val stickerData = StickerMessagePayload(
                name = m.name,
                stickerId = m.stickerId
            )
            ForwardMessage(ForwardCategory.Sticker, GsonHelper.customGson.toJson(stickerData), m.id)
        }
        m.category.endsWith("_AUDIO") -> {
            if (m.absolutePath()?.fileExists() != true) {
                return null
            }
            val duration = m.mediaDuration?.toLongOrNull() ?: return null
            val waveForm = m.mediaWaveform ?: return null
            val audioData = AudioMessagePayload(
                UUID.randomUUID().toString(),
                requireNotNull(m.absolutePath()),
                duration,
                waveForm,
                m.content,
            )
            ForwardMessage(ForwardCategory.Audio, GsonHelper.customGson.toJson(audioData), m.id)
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
                GsonHelper.customGson.fromJson(m.content, LiveMessagePayload::class.java).shareable
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
            ForwardMessage(ShareCategory.Live, GsonHelper.customGson.toJson(liveData), m.id)
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
                        GsonHelper.customGson.toJson(
                            toLocationData(c)
                        ),
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
