package one.mixin.android.ui.wallet

import com.google.gson.Gson
import one.mixin.android.db.property.PropertyHelper

internal const val PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED = "pref_wallet_home_add_wallet_banner_closed"
internal const val PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED = "pref_wallet_home_dynamic_banner_closed"
internal const val PREF_WALLET_HOME_REFERRAL_CLOSED = "pref_wallet_home_referral_closed"

private val walletHomePreferencesGson = Gson()

internal fun walletHomeDynamicBannerClosedKey() = PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED

internal suspend fun findWalletHomeDynamicBannerClosedIds(): Set<String> {
    val value = PropertyHelper.findValueByKey(walletHomeDynamicBannerClosedKey(), "")
    if (value.isBlank()) return emptySet()
    return runCatching {
        walletHomePreferencesGson.fromJson(value, Array<String>::class.java)
            .orEmpty()
            .filter(String::isNotBlank)
            .toSet()
    }.getOrDefault(emptySet())
}

internal suspend fun updateWalletHomeDynamicBannerClosedIds(closedIds: Set<String>) {
    if (closedIds.isEmpty()) {
        PropertyHelper.deleteKeyValue(walletHomeDynamicBannerClosedKey())
    } else {
        PropertyHelper.updateKeyValue(
            walletHomeDynamicBannerClosedKey(),
            walletHomePreferencesGson.toJson(closedIds.sorted()),
        )
    }
}
