package one.mixin.android.util

import one.mixin.android.Constants
import java.math.BigDecimal

fun <T> assetListComparator(
    query: String?,
    symbol: (T) -> String,
    balance: (T) -> BigDecimal,
    price: (T) -> BigDecimal,
    level: (T) -> Int?,
    name: (T) -> String,
    chainName: (T) -> String,
): Comparator<T> =
    compareByDescending<T> { !query.isNullOrBlank() && symbol(it).equals(query, ignoreCase = true) }
        .thenByDescending { balance(it) * price(it) }
        .thenByDescending { level(it) ?: Constants.AssetLevel.UNKNOWN }
        .thenByDescending(balance)
        .thenByDescending(price)
        .thenBy(name)
        .thenBy(chainName)
