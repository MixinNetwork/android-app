package one.mixin.android.ui.wallet

import one.mixin.android.util.assetListComparator
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

fun sortQueryAsset(
    query: String,
    localAssets: List<TokenItem>?,
    remoteAssets: List<TokenItem>,
): List<TokenItem> =
    (remoteAssets + localAssets.orEmpty().filterNot { local -> remoteAssets.any { it.assetId == local.assetId } })
        .filter { it.primitiveAssetId.isNullOrEmpty() }
        .sortedWith(
            assetListComparator(
                query = query,
                symbol = { it.symbol },
                balance = { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO },
                price = { it.priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO },
                level = { it.level },
                name = { it.name },
                chainName = { it.chainName.orEmpty() },
            ),
        )
