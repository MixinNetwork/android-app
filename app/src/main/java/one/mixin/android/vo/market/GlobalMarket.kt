package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity("global_market")
class GlobalMarket(
    @PrimaryKey
    @SerializedName("market_cap")
    @ColumnInfo(name = "market_cap")
    val marketCap: String,
    @SerializedName("market_cap_change_percentage")
    @ColumnInfo(name = "market_cap_change_percentage")
    val marketCapChangePercentage: String,
    @SerializedName("volume")
    @ColumnInfo(name = "volume")
    val volume: String,
    @ColumnInfo(name = "volume_change_percentage")
    @SerializedName("volume_change_percentage")
    val volumeChangePercentage: String,
    @SerializedName("dominance")
    @ColumnInfo(name = "dominance")
    val dominance: String,
    @SerializedName("dominance_percentage")
    @ColumnInfo(name = "dominance_percentage")
    val dominancePercentage: String,
)