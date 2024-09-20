package one.mixin.android.ui.wallet.alert.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@Entity("alerts")
@TypeConverters(AlertFrequencyConverter::class, AlertTypeConverter::class)
class Alert(
    @PrimaryKey
    @ColumnInfo(name = "alert_id")
    @SerializedName("alert_id")
    val alertId: String,
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "type")
    @SerializedName("type")
    val type: AlertType,
    @ColumnInfo(name = "frequency")
    @SerializedName("frequency")
    val frequency: AlertFrequency,
    @SerializedName("value")
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String
)
