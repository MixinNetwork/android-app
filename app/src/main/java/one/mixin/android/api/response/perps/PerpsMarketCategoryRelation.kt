package one.mixin.android.api.response.perps

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

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
