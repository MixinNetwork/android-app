package one.mixin.android.tip.wc.internal

internal fun formatProposalAccountText(
    chainIds: Set<String>,
    addresses: WalletConnectAddresses,
): String =
    buildList {
        if (chainIds.any { it.startsWith("eip155:") } && addresses.evm.isNotBlank()) {
            add("EVM: ${addresses.evm}")
        }
        if (chainIds.any { Chain.Solana.supportsWalletConnectChainId(it) } && addresses.solana.isNotBlank()) {
            add("${Chain.Solana.name}: ${addresses.solana}")
        }
        if (Chain.Bitcoin.chainId in chainIds && addresses.bitcoin.isNotBlank()) {
            add("${Chain.Bitcoin.name}: ${addresses.bitcoin}")
        }
    }.joinToString("\n")
