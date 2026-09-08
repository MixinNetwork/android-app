package one.mixin.android.vo.safe

import java.math.BigDecimal

data class TokenGroup(val tokens: List<TokenItem>) {
    val representative: TokenItem get() = tokens.first()
    val balance: BigDecimal get() = tokens.sumOf { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val fiat: BigDecimal get() = tokens.sumOf { it.fiat() }
}

val TokenItem.groupId: String
    get() = coinId?.takeIf { it.isNotBlank() && collectionHash.isNullOrEmpty() }
        ?.let { "coin:$it" } ?: "asset:$assetId"

fun List<TokenItem>.groupTokens(): List<TokenGroup> =
    distinctBy { it.assetId }.groupBy { it.groupId }.values.map { TokenGroup(it) }
