package one.mixin.android.ui.home.web3.trade.perps

import android.os.Parcelable
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.isUUID
import java.math.BigDecimal
import java.util.Base64

@Parcelize
data class PerpsSharePayload(
    val marketId: String,
    val side: String,
    val leverage: Int,
    val entryPrice: String,
    val markPrice: String,
) : Parcelable {
    companion object {
        const val CATEGORY = "perps_position"
        private val decimal = Regex("-?[0-9]{1,40}(\\.[0-9]{1,40})?")

        fun decode(data: String?): PerpsSharePayload? {
            if (data.isNullOrEmpty() || data.length > 8192) return null
            val root = try {
                JsonParser.parseString(Base64.getDecoder().decode(data).toString(Charsets.UTF_8))
            } catch (_: IllegalArgumentException) {
                return null
            } catch (_: JsonParseException) {
                return null
            }
            val json = root.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            fun field(name: String) = json[name]?.takeIf { it.isJsonPrimitive }?.asString
            fun number(name: String) = field(name)?.takeIf(decimal::matches)

            return PerpsSharePayload(
                marketId = field("market_id")?.takeIf { it.length == 36 && it.isUUID() } ?: return null,
                side = field("side")?.takeIf { it == "long" || it == "short" } ?: return null,
                leverage = json["leverage"]?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                    ?.asString?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO && it <= Int.MAX_VALUE.toBigDecimal() && it.stripTrailingZeros().scale() <= 0 }
                    ?.toInt() ?: return null,
                entryPrice = number("entry_price")?.takeIf { it.toBigDecimal().signum() > 0 } ?: return null,
                markPrice = number("mark_price")?.takeIf { it.toBigDecimal().signum() > 0 } ?: return null,
            )
        }
    }
}
