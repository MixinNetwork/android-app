package one.mixin.android.api.response.perps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ranks")
data class PerpsRank(
    @PrimaryKey
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @ColumnInfo(name = "rank")
    val rank: Int,
)
