package one.mixin.android.util.analytics

import java.math.BigDecimal

internal object AnalyticsRules {
    private const val NON_ORGANIC = "Non-Organic"

    private val directAppsFlyerEvents = setOf(
        "sign_up_start",
        "sign_up_account_created",
        "login_start",
        "buy_start",
        "asset_receive_start",
        "asset_receive_end",
        "asset_receive_success",
        "trade_spot_start",
        "trade_spot_end",
        "trade_perps_open_position_start",
        "trade_perps_open_position_end",
        "asset_send_start",
        "asset_send_end",
    )

    fun appsFlyerEventName(eventName: String): String? =
        when (eventName) {
            "login_end" -> "af_login"
            "sign_up_end" -> "af_complete_registration"
            in directAppsFlyerEvents -> eventName
            else -> null
        }

    fun conversionUserProperties(conversionData: Map<String, Any?>): Map<String, String> {
        val status = conversionData["af_status"]?.toString()?.takeIf { it.isNotBlank() } ?: return emptyMap()
        val properties = linkedMapOf("af_source" to status)
        if (status == NON_ORGANIC) {
            conversionData["media_source"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                properties["af_media_source"] = it
            }
            conversionData["campaign"]?.toString()?.takeIf { it.isNotBlank() }?.let {
                properties["af_campaign"] = it
            }
        }
        return properties
    }

    fun receiveAssetLevel(amountUsd: BigDecimal): String =
        when {
            amountUsd >= BigDecimal("1000000") -> "v1,000,000"
            amountUsd >= BigDecimal("100000") -> "v100,000"
            amountUsd >= BigDecimal("10000") -> "v10,000"
            amountUsd >= BigDecimal("1000") -> "v1,000"
            amountUsd >= BigDecimal("100") -> "v100"
            else -> "v1"
        }
}
