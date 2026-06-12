package one.mixin.android.ui.conversation

import one.mixin.android.Constants
import one.mixin.android.extension.isUUID
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class PerpsTradeAction(
    val marketId: String,
)

internal data class SpotTradeAction(
    val input: String?,
    val output: String?,
    val amount: String?,
    val referral: String?,
    val openLimit: Boolean,
)

internal fun String.toPerpsTradeAction(): PerpsTradeAction? {
    if (!startsWith(Constants.Scheme.HTTPS_TRADE, true) && !startsWith(Constants.Scheme.MIXIN_TRADE, true)) {
        return null
    }

    val query = runCatching { URI(this).rawQuery }.getOrNull() ?: return null
    val type = query.queryParameter("type") ?: return null
    if (!type.equals("perps", true) && !type.equals("perpetual", true)) {
        return null
    }

    val marketId = query.queryParameter("market")?.takeIf { it.isUUID() } ?: return null
    return PerpsTradeAction(marketId)
}

internal fun String.toSpotTradeAction(): SpotTradeAction? {
    val isSwap = startsWith(Constants.Scheme.HTTPS_SWAP, true) || startsWith(Constants.Scheme.MIXIN_SWAP, true)
    val isTrade = startsWith(Constants.Scheme.HTTPS_TRADE, true) || startsWith(Constants.Scheme.MIXIN_TRADE, true)
    if (!isSwap && !isTrade) {
        return null
    }

    val query = runCatching { URI(this).rawQuery }.getOrElse { return null }
    val type = query?.queryParameter("type")
    if (type.equals("perps", true) || type.equals("perpetual", true)) {
        return null
    }

    val input = query?.queryParameter("input")?.takeIf(String::isNotBlank)
    val output = query?.queryParameter("output")?.takeIf(String::isNotBlank)
    if (input != null && !input.isUUID()) return null
    if (output != null && !output.isUUID()) return null

    return SpotTradeAction(
        input = input,
        output = output,
        amount = query?.queryParameter("amount"),
        referral = query?.queryParameter("referral"),
        openLimit = type.equals("limit", true),
    )
}

private fun String.queryParameter(name: String): String? =
    split("&")
        .asSequence()
        .mapNotNull { parameter ->
            val parts = parameter.split("=", limit = 2)
            val key = parts.getOrNull(0)?.urlDecode()
            val value = parts.getOrNull(1)?.urlDecode()
            if (key == name) value else null
        }
        .firstOrNull()

private fun String.urlDecode(): String? =
    runCatching { URLDecoder.decode(this, StandardCharsets.UTF_8.name()) }.getOrNull()
