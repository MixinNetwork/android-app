package one.mixin.android.api.response.perps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "market_categories",
    primaryKeys = ["market_id", "category"],
    indices = [Index(value = ["category"])],
)
data class PerpsMarketCategoryRelation(
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @ColumnInfo(name = "category")
    val category: Int,
)
