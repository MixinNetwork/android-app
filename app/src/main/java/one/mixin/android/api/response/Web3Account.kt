package one.mixin.android.api.response

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "web_accounts")
class Web3Account(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,
    @SerializedName("change_absolute_1d")
    @ColumnInfo(name = "change_absolute_1d")
    val changeAbsolute1d: String,
    val tokens: List<Web3Token>,
    @ColumnInfo(name = "change_percent_1d")
    @SerializedName("change_percent_1d")
    val changePercent1d: String,
    @ColumnInfo(name = "balance")
    @SerializedName("balance")
    val balance: String,
)