package one.mixin.android.ui.home.web3.components

import androidx.room.ColumnInfo
import java.math.BigDecimal
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats

class InscriptionState(
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "sequence")
    val sequence: Long?,
    @ColumnInfo(name = "amount")
    val amount: String?,
    @ColumnInfo(name = "symbol")
    val symbol: String?,
    @ColumnInfo(name = "price_usd")
    val priceUsd: String?,
    @ColumnInfo(name = "state")
    val state: String?,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "content_url")
    val contentURL: String?,
    @ColumnInfo(name = "content_type")
    val contentType: String?,
    @ColumnInfo(name = "traits")
    val traits: String?
) {
    val isText: Boolean
        get() = contentType?.startsWith("text", true) == true

    val id: String
        get() {
            return if (sequence != null) {
                "#$sequence"
            } else {
                ""
            }
        }

    val valueAs: String
        get() {
            val value =
                try {
                    if (priceUsd == null || priceUsd == "0") {
                        BigDecimal.ZERO
                    } else {
                        BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
                    }
                } catch (e: ArithmeticException) {
                    BigDecimal.ZERO
                } catch (e: NumberFormatException) {
                    BigDecimal.ZERO
                }
            return "${value.numberFormat2()} ${Fiats.getAccountCurrencyAppearance()}"
        }

    val tokenTotal: String
        get() {
            return "$amount ${symbol ?: ""}"
        }
}
