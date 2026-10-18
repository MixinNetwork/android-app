package one.mixin.android.ui.wallet

import com.google.gson.Gson
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import one.mixin.android.db.property.PropertyHelper

internal const val PREF_WALLET_HOME_ADD_WALLET_BANNER_CLOSED = "pref_wallet_home_add_wallet_banner_closed"
internal const val PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED = "pref_wallet_home_dynamic_banner_closed"
internal const val PREF_WALLET_HOME_REFERRAL_CLOSED = "pref_wallet_home_referral_closed"

private val closedBannerMutex = Mutex()

private val walletHomePreferencesGson = Gson()

internal fun walletHomeDynamicBannerClosedKey() = PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED

internal suspend fun findWalletHomeDynamicBannerClosedIds(): Set<String> = withContext(Dispatchers.IO) {
    parseWalletHomeDynamicBannerClosedIds(PropertyHelper.findValueByKey(walletHomeDynamicBannerClosedKey(), ""))
}

internal fun parseWalletHomeDynamicBannerClosedIds(value: String): Set<String> {
    if (value.isBlank()) return emptySet()
    return requireNotNull(walletHomePreferencesGson.fromJson(value, Array<String>::class.java)) {
        "Invalid closed banner ids"
    }.filter(String::isNotBlank).toSet()
}

internal suspend fun updateWalletHomeDynamicBannerClosedIds(
    closedIds: Set<String>,
    read: suspend () -> Set<String> = ::findWalletHomeDynamicBannerClosedIds,
    write: suspend (Set<String>) -> Unit = { ids ->
        PropertyHelper.updateKeyValue(walletHomeDynamicBannerClosedKey(), walletHomePreferencesGson.toJson(ids.sorted()))
    },
) {
    withContext(NonCancellable) {
        closedBannerMutex.withLock {
            write(read() + closedIds)
        }
    }
}
