package one.mixin.android.util.video

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class VideoEditedInfo(
    var originalPath: String,
    var duration: Long,
    var rotationValue: String,
    var originalWidth: Int = 0,
    var originalHeight: Int = 0,
    var resultWidth: Int = 0,
    var resultHeight: Int = 0,
    var thumbnail: String? = null,
    var fileName: String,
    var bitrate: Int,
    var needChange: Boolean = true
) : Parcelable
