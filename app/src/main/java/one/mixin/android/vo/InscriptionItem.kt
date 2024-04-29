package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "inscription_item")
data class InscriptionItem(
    @PrimaryKey
    @ColumnInfo(name = "inscription_hash")
    @SerializedName("inscription_hash")
    val inscriptionHash: String,
    @ColumnInfo(name = "collection_hash")
    @SerializedName("collection_hash")
    val collectionHash: String,
    @ColumnInfo(name = "sequence")
    @SerializedName("sequence")
    val sequence: Long,
    @ColumnInfo(name = "content_type")
    @SerializedName("content_type")
    val contentType: String,
    @ColumnInfo(name = "content_url")
    @SerializedName("content_url")
    val contentURL: String,
    @ColumnInfo(name = "occupied_by")
    @SerializedName("occupied_by")
    val occupiedBy: String?,
    @ColumnInfo(name = "occupied_at")
    @SerializedName("occupied_at")
    val occupiedAt: String?,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String
)
