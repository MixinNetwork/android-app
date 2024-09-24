package one.mixin.android.ui.wallet.alert.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.extension.priceFormat
import java.math.BigDecimal
import java.math.RoundingMode

@Entity("market_alerts")
@TypeConverters(AlertFrequencyConverter::class, AlertTypeConverter::class, AlertStatusConverter::class)
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
    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: AlertStatus,
    @SerializedName("value")
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String
) {
    val displayValue: String
        get() {
            return when (type) {
                in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_INCREASED, AlertType.PRICE_DECREASED) -> {
                    "${BigDecimal(value).priceFormat()} USD"
                }

                AlertType.PERCENTAGE_INCREASED -> {
                    "+${(value.toFloat() * 100f).toBigDecimal().setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString()}%"
                }

                else -> {
                    "-${(value.toFloat() * 100f).toBigDecimal().setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString()}%"
                }
            }
        }

    val rawValue: String
        get() {
            return when (type) {
                in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_INCREASED, AlertType.PRICE_DECREASED) -> {
                    value
                }

                else -> {
                    (value.toFloat() * 100f).toString()
                }
            }
        }
}