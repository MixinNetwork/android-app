package one.mixin.android.vo

import android.content.Context
import androidx.room.ColumnInfo
import one.mixin.android.R

class PinMessageItem(
    @ColumnInfo(name = "id")
    val messageId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "content")
    val content: String?,
    @ColumnInfo(name = "full_name")
    val userFullName: String?,
    @ColumnInfo(name = "mentions")
    val mentions: String?,
    @ColumnInfo(name = "category")
    override val type: String,
): ICategory


fun PinMessageItem?.explain(context: Context): CharSequence {
    return when {
        this == null -> context.getText(R.string.a_message)
        isImage() -> context.getText(R.string.a_photo)
        isVideo() -> context.getText(R.string.a_video)
        isLive() -> context.getText(R.string.a_live)
        isData() -> context.getText(R.string.a_file)
        isAudio() -> context.getText(R.string.an_audio)
        isSticker() -> context.getText(R.string.a_sticker)
        isContact() -> context.getText(R.string.a_contact)
        isPost() -> context.getText(R.string.a_post)
        isLocation() -> context.getText(R.string.a_location)
        isTranscript() -> context.getText(R.string.a_transcript)
        isAppCard() -> context.getText(R.string.a_card)
        isAppButtonGroup() -> context.getText(R.string.a_message)
        else -> " \"${content}\""
    }
}

