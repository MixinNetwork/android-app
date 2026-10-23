package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters

@ColumnTypeConverters(ArrayConverters::class)
data class ForwardUser(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "app_id")
    val appId: String?,
    @ColumnInfo(name = "capabilities")
    val capabilities: ArrayList<String>?,
)

fun ForwardUser.encryptedCategory(): EncryptCategory =
    if (appId != null && capabilities?.contains(AppCap.ENCRYPTED.name) == true) {
        EncryptCategory.ENCRYPTED
    } else if (appId != null) {
        EncryptCategory.PLAIN
    } else {
        EncryptCategory.SIGNAL
    }
