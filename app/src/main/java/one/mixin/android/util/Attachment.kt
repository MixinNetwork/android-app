package one.mixin.android.util

import android.net.Uri
import one.mixin.android.websocket.DataMessagePayload

data class Attachment(val uri: Uri, val filename: String, val mimeType: String, val fileSize: Long) {
    fun toDataMessagePayload() = DataMessagePayload(uri.toString(), filename, mimeType, fileSize)
}
