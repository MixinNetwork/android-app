package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId
import one.mixin.android.session.Session
import java.math.BigDecimal

internal fun generateDepositUri(
    assetId: String?,
    chainId: String?,
    assetKey: String?,
    address: String?,
    amount: String,
    precision: Int? = null,
): String? {
    if (assetId.isNullOrEmpty() || amount == "0") return null
    if (address == null) {
        return "${Constants.Scheme.HTTPS_PAY}/${Session.getAccountId()}?asset=${assetId}&amount=$amount"
    }

    val cleanAmount = amount.replace(Regex("[^0-9.]"), "")
    if (cleanAmount.isEmpty() || cleanAmount.toDoubleOrNull() == 0.0) return null

    return when (chainId) {
        ChainId.BITCOIN_CHAIN_ID -> {
            "bitcoin:$address?amount=$cleanAmount"
        }

        ChainId.PEARL_CHAIN_ID -> {
            "pearl:$address?amount=$cleanAmount"
        }

        ChainId.ETHEREUM_CHAIN_ID -> {
            if (assetId == ChainId.ETHEREUM_CHAIN_ID) {
                "ethereum:$address?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@1/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Arbitrum -> {
            if (assetId == ChainId.Arbitrum) {
                "ethereum:$address@42161?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@42161/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Optimism -> {
            if (assetId == ChainId.Optimism) {
                "ethereum:$address@10?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@10/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Base -> {
            if (assetId == ChainId.Base) {
                "ethereum:$address@8453?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@8453/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Polygon -> {
            if (assetId == ChainId.Polygon) {
                "ethereum:$address@137?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@137/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.BinanceSmartChain -> {
            if (assetId == ChainId.BinanceSmartChain) {
                "ethereum:$address@56?amount=$cleanAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@56/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Avalanche -> {
            if (assetId == ChainId.Avalanche) {
                val weiAmount = nativeEvmWeiAmount(cleanAmount)
                "ethereum:$address@43114?value=$weiAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@43114/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.HyperEVM -> {
            if (assetId == ChainId.HyperEVM) {
                val weiAmount = nativeEvmWeiAmount(cleanAmount)
                "ethereum:$address@999?value=$weiAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@999/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.XLayer -> {
            if (assetId == ChainId.XLayer) {
                val weiAmount = nativeEvmWeiAmount(cleanAmount)
                "ethereum:$address@196?value=$weiAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@196/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Robinhood -> {
            if (assetId == ChainId.Robinhood) {
                val weiAmount = nativeEvmWeiAmount(cleanAmount)
                "ethereum:$address@4663?value=$weiAmount"
            } else {
                val uint256Amount = erc20Amount(cleanAmount, precision)
                "ethereum:${assetKey}@4663/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Litecoin -> {
            "litecoin:$address?amount=$cleanAmount"
        }

        ChainId.Dogecoin -> {
            "dogecoin:$address?amount=$cleanAmount"
        }

        ChainId.Dash -> {
            "dash:$address?amount=$cleanAmount&IS=1"
        }

        ChainId.Monero -> {
            "monero:$address?tx_amount=$cleanAmount"
        }

        ChainId.Solana -> {
            if (assetKey.isNullOrBlank()) {
                "solana:$address?amount=$cleanAmount"
            } else {
                "solana:$address?amount=$cleanAmount&spl-token=${assetKey}&token=${assetKey}"
            }
        }

        ChainId.TON_CHAIN_ID -> {
            if (assetId == ChainId.TON_CHAIN_ID) {
                val tonAmount = try {
                    val nanoAmount = BigDecimal(cleanAmount)
                    val divisor = BigDecimal("1000000000")
                    nanoAmount.multiply(divisor).stripTrailingZeros().toPlainString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ton://transfer/$address?amount=$tonAmount"
            } else {
                val jettonAmount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: when (assetId) {
                        Constants.AssetId.USDT_ASSET_TON_ID -> 6
                        else -> 9
                    }
                    tokenAmount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ton://transfer/$address?jetton=$assetKey&amount=$jettonAmount"
            }
        }

        else -> null
    }
}

private fun erc20Amount(cleanAmount: String, precision: Int?): String {
    return try {
        val tokenAmount = BigDecimal(cleanAmount)
        val decimals = precision ?: 18
        tokenAmount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger().toString()
    } catch (_: Exception) {
        cleanAmount
    }
}

private fun nativeEvmWeiAmount(cleanAmount: String): String {
    return try {
        BigDecimal(cleanAmount).multiply(BigDecimal.TEN.pow(18)).toBigInteger().toString()
    } catch (_: Exception) {
        cleanAmount
    }
}
