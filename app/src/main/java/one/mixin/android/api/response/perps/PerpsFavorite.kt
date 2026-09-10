package one.mixin.android.api.response.perps

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "favorites")
data class PerpsFavorite(
    @PrimaryKey
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @ColumnInfo(name = "is_favored")
    val isFavored: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
