package one.mixin.android.vo

import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "traces")
data class Trace(
    @PrimaryKey
    @SerializedName("trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String?,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String?,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,
    @SerializedName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    var snapshotId: String?,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
) : Parcelable
