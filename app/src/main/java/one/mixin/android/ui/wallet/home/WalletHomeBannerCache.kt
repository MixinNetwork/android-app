package one.mixin.android.ui.wallet.home

import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.extension.putString
import one.mixin.android.util.GsonHelper
import org.threeten.bp.Instant

private const val PREF_WALLET_HOME_BANNER_CACHE_PREFIX = "pref_wallet_home_banner_cache"
private val walletHomeBannerListType = object : TypeToken<List<WalletHomeBanner>>() {}.type

fun SharedPreferences.getWalletHomeBannerCache(
    key: String,
    now: Instant = Instant.now(),
): List<WalletHomeBanner>? =
    runCatching {
        getString(walletHomeBannerCacheKey(key), null)
            ?.let { GsonHelper.customGson.fromJson<List<WalletHomeBanner>>(it, walletHomeBannerListType) }
            ?.filterNot { it.isExpired(now) }
    }.getOrNull()

fun SharedPreferences.putWalletHomeBannerCache(
    key: String,
    banners: List<WalletHomeBanner>,
) {
    putString(walletHomeBannerCacheKey(key), GsonHelper.customGson.toJson(banners))
}

private fun walletHomeBannerCacheKey(key: String) = "$PREF_WALLET_HOME_BANNER_CACHE_PREFIX:$key"
