package one.mixin.android.vo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.android.parcel.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getFilePath
import one.mixin.android.util.GsonHelper
import one.mixin.android.websocket.VideoMessagePayload

@SuppressLint("ParcelCreator")
@Parcelize
data class ForwardMessage<T : ForwardCategory>(
    val category: T,
    val content: String
) : Parcelable

sealed class ForwardCategory : Parcelable {
    @Parcelize object Sticker : ForwardCategory() {
        override fun toString() = "Sticker"
    }
    @Parcelize object Video : ForwardCategory() {
        override fun toString() = "Video"
    }
    @Parcelize object Data : ForwardCategory() {
        override fun toString() = "Data"
    }
    @Parcelize object Audio : ForwardCategory() {
        override fun toString() = "Audio"
    }
    @Parcelize object Location : ForwardCategory() {
        override fun toString() = "Location"
    }
}

sealed class ShareCategory : ForwardCategory() {
    @Parcelize object Text : ShareCategory() {
        override fun toString() = "Text"
    }
    @Parcelize object Post : ShareCategory() {
        override fun toString() = "Post"
    }
    @Parcelize object Image : ShareCategory() {
        override fun toString() = "Image"

        fun getErrorStringOrNull(code: Int) =
            when (code) {
                -1 -> R.string.error_image
                -2 -> R.string.error_format
                else -> null
            }
    }
    @Parcelize object Live : ShareCategory() {
        override fun toString() = "Live"
    }
    @Parcelize object Contact : ShareCategory() {
        override fun toString() = "Contact"
    }
    @Parcelize object AppCard : ShareCategory() {
        override fun toString() = "App_Card"
    }
}

sealed class ForwardAction(
    open val conversationId: String? = null,
    open val name: String? = null
) : Parcelable {
    @Parcelize data class System(
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

inline fun <reified T : ForwardCategory> ForwardMessage<T>.addTo(list: MutableList<ForwardMessage<T>>) {
    list.add(this)
}

inline fun <reified T : ForwardCategory> Uri.systemMediaToMessage(category: T): ForwardMessage<ForwardCategory>? {
    val url = this.getFilePath(MixinApplication.appContext) ?: return null
    return url.systemMediaToMessage(category)
}

inline fun <reified T : ForwardCategory> String.systemMediaToMessage(category: T): ForwardMessage<ForwardCategory>? =
    ForwardMessage(
        category,
        GsonHelper.customGson.toJson(
            when (category) {
                ShareCategory.Image -> ShareImageData(this)
                ForwardCategory.Video -> VideoMessagePayload(this)
                ForwardCategory.Data -> {
                    val attachment = MixinApplication.get().getAttachment(this.toUri())
                    attachment?.toDataMessagePayload()
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
    ShareCategory.AppCard
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
