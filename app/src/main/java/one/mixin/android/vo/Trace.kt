package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "traces")
@JsonClass(generateAdapter = true)
data class Trace(
    @PrimaryKey
    @Json(name ="trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String,
    @Json(name ="asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @Json(name ="amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @Json(name ="opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String?,
    @ColumnInfo(name = "destination")
    @Json(name ="destination")
    val destination: String?,
    @ColumnInfo(name = "tag")
    @Json(name ="tag")
    val tag: String?,
    @Json(name ="snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    var snapshotId: String?,
    @Json(name ="created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String
) : Parcelable
