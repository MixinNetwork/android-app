package one.mixin.android.vo

import one.mixin.android.util.GsonHelper

class VideoClip(
    val uri: String,
    val startProgress: Float,
    val endProgress: Float
)

fun VideoClip.toJson(): String = GsonHelper.customGson.toJson(this)

fun toVideoClip(content: String?, mediaUrl: String?): VideoClip {
    return if (content != null) {
        try {
            GsonHelper.customGson.fromJson(content, VideoClip::class.java)
        } catch (e: Exception) {
            VideoClip(content, 0f, 1f)
        }
    } else {
        VideoClip(requireNotNull(mediaUrl), 0f, 1f)
    }
}
