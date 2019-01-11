package one.mixin.android.util.backup.drive

import java.util.Date

data class Metadata(
    val description: String,
    val fileSize: Long,
    val mimeType: String,
    val createdDate: Date
)