package one.mixin.android.db.web3.vo

import java.math.BigDecimal

data class Web3TokenGroup(val tokens: List<Web3TokenItem>) {
    val representative: Web3TokenItem get() = tokens.first()
    val balance: BigDecimal get() = tokens.sumOf { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val fiat: BigDecimal get() = tokens.sumOf { it.fiat() }
}

val Web3TokenItem.groupId: Pair<String, String>
    get() = walletId to (coinId?.takeIf { it.isNotBlank() }?.let { "coin:$it" } ?: "asset:$assetId")

fun List<Web3TokenItem>.groupWeb3Tokens(): List<Web3TokenGroup> =
    distinctBy { it.walletId to it.assetId }.groupBy { it.groupId }.values.map { Web3TokenGroup(it) }
