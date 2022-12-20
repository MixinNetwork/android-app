package one.mixin.android.ui.wallet

import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.vo.AssetItem
import java.math.BigDecimal

private const val defaultIconUrl = "https://images.mixin.one/yH_I5b0GiV2zDmvrXRyr3bK5xusjfy5q7FX3lw3mM2Ryx4Dfuj6Xcw8SHNRnDKm7ZVE3_LvpKlLdcLrlFQUBhds=s128"

fun sortQueryAsset(query: String, localAssets: List<AssetItem>?, remoteAssets: List<AssetItem>): List<AssetItem>? {
    return localAssets?.plus(
        remoteAssets.filterNot { r ->
            localAssets.any { l ->
                l.assetId == r.assetId
            }
        },
    )?.sortedWith(
        Comparator { o1, o2 ->
            if (o1 == null && o2 == null) return@Comparator 0
            if (o1 == null) return@Comparator 1
            if (o2 == null) return@Comparator -1

            val equal2Keyword1 = o1.symbol.equalsIgnoreCase(query)
            val equal2Keyword2 = o2.symbol.equalsIgnoreCase(query)
            if (equal2Keyword1 && !equal2Keyword2) {
                return@Comparator -1
            } else if (!equal2Keyword1 && equal2Keyword2) {
                return@Comparator 1
            }

            val capitalization1 = o1.priceFiat() * BigDecimal(o1.balance)
            val capitalization2 = o2.priceFiat() * BigDecimal(o2.balance)
            if (capitalization1 != capitalization2) {
                if (capitalization2 > capitalization1) {
                    return@Comparator 1
                } else if (capitalization2 < capitalization1) {
                    return@Comparator -1
                }
            }

            val hasIcon1 = o1.iconUrl != defaultIconUrl
            val hasIcon2 = o2.iconUrl != defaultIconUrl
            if (hasIcon1 && !hasIcon2) {
                return@Comparator -1
            } else if (!hasIcon1 && hasIcon2) {
                return@Comparator 1
            }

            return@Comparator o1.name.compareTo(o2.name)
        },
    )
}
