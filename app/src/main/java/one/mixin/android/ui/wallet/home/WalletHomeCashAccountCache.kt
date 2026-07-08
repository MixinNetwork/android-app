package one.mixin.android.ui.wallet.home

import android.content.SharedPreferences
import com.google.gson.JsonParser
import one.mixin.android.extension.putString
import one.mixin.android.util.GsonHelper
import java.math.BigDecimal

private const val PREF_WALLET_HOME_CASH_ACCOUNT_CACHE_PREFIX = "pref_wallet_home_cash_account_cache"

fun SharedPreferences.getWalletHomeCashAccountCache(
    key: String,
): WalletHomeCashAccount? =
    runCatching {
        getString(walletHomeCashAccountCacheKey(key), null)
            ?.let { GsonHelper.customGson.fromJson(it, WalletHomeCashAccount::class.java) }
    }.getOrNull()

fun SharedPreferences.putWalletHomeCashAccountCache(
    key: String,
    cashAccount: WalletHomeCashAccount,
) {
    putString(walletHomeCashAccountCacheKey(key), GsonHelper.customGson.toJson(cashAccount))
}

fun SharedPreferences.getLegacyWalletHomeCashAccountCache(
    walletHomeCacheKey: String,
): WalletHomeCashAccount? =
    getString(walletHomeCacheKey, null).legacyWalletHomeCashAccountCache()

internal fun String?.legacyWalletHomeCashAccountCache(): WalletHomeCashAccount? =
    runCatching {
        val root = JsonParser.parseString(this).asJsonObject
        val cashAccount = root.getAsJsonObject("cashAccount") ?: return@runCatching null
        val balance = cashAccount.get("balanceUsd")
            ?: cashAccount.get("balance")
            ?: return@runCatching null
        val balanceValue = balance.asString.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val rewardApy = cashAccount.get("rewardApy")
            ?.takeUnless { it.isJsonNull }
            ?.asString
        WalletHomeCashAccount(
            balanceUsd = balanceValue,
            rewardApy = rewardApy,
        )
    }.getOrNull()

private fun walletHomeCashAccountCacheKey(key: String) = "$PREF_WALLET_HOME_CASH_ACCOUNT_CACHE_PREFIX:$key"
