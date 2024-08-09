package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "history_prices", primaryKeys = ["asset_id", "type"])
data class HistoryPrice(
    @ColumnInfo(name = "asset_id")
    @SerializedName("key")
    val assetId: String,
    @ColumnInfo(name = "type")
    @SerializedName("type")
    val type: String,
    @ColumnInfo(name = "data")
    @SerializedName("data")
    val data: List<Price>
)