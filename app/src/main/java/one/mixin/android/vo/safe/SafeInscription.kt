package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

class SafeInscription(
    @ColumnInfo(name = "collection_hash") @SerializedName("collection_hash") val collectionHash: String,
    @ColumnInfo(name = "inscription_hash") @SerializedName("inscription_hash") val inscriptionHash: String,
    @ColumnInfo(name = "sequence") @SerializedName("sequence") val sequence: Long,
    @ColumnInfo(name = "name") @SerializedName("name") val name: String,
    @ColumnInfo(name = "content_type") @SerializedName("content_type") val contentType: String,
    @ColumnInfo(name = "content_url") @SerializedName("content_url") val contentURL: String,
    @ColumnInfo(name = "icon_url") @SerializedName("icon_url") val iconURL: String,
)