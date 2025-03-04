package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.WithdrawalMemoPossibility

@Entity(tableName = "web3_chains")
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
    @ColumnInfo(name = "withdrawal_memo_possibility")
    @SerializedName("withdrawal_memo_possibility")
    val withdrawalMemoPossibility: String = WithdrawalMemoPossibility.POSSIBLE.name,
)
