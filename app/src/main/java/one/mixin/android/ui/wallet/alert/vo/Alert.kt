package one.mixin.android.ui.wallet.alert.vo

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.vo.AlertType.PERCENTAGE_DECREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PERCENTAGE_INCREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_DECREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_INCREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_REACHED
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

    @Composable
    fun getTintColor(quoteColorPref: Boolean): Color {
        return if (status == AlertStatus.RUNNING) {
            return when (type) {
                PRICE_REACHED -> Color.Unspecified
                PRICE_INCREASED, PERCENTAGE_INCREASED -> {
                    if (quoteColorPref) {
                        MixinAppTheme.colors.walletRed
                    } else {
                        MixinAppTheme.colors.walletGreen
                    }
                }

                PRICE_DECREASED, PERCENTAGE_DECREASED -> {
                    if (quoteColorPref) {
                        MixinAppTheme.colors.walletGreen
                    } else {
                        MixinAppTheme.colors.walletRed
                    }
                }
            }
        } else {
            MixinAppTheme.colors.textAssist
        }
    }
}