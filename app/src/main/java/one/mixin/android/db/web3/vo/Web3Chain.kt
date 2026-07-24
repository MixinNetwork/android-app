package one.mixin.android.db.web3.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "chains")
data class Web3Chain(
    @PrimaryKey
    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "threshold")
    val threshold: Int,
)