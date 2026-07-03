package one.mixin.android.vo.market

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "history_prices", primaryKeys = ["coin_id", "type"])
data class HistoryPrice(
    @ColumnInfo(name = "coin_id")
    @SerializedName("coin_id")
    val coinId: String,
    @ColumnInfo(name = "type")
    @SerializedName("type")
    val type: String,
    @ColumnInfo(name = "data")
    @SerializedName("data")
    val data: List<Price>,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String
)