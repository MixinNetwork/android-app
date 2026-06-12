package one.mixin.android.tip.wc.internal

import com.reown.walletkit.client.Wallet
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalletConnectNamespaceTest {
    @Test
    fun supportedNamespacesIncludeEveryAvailableWalletAddress() {
        val evmAddress = "0x1111111111111111111111111111111111111111"
        val solanaAddress = "So11111111111111111111111111111111111111112"
        val bitcoinAddress = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"

        val namespaces = getSupportedNamespaces(
            WalletConnectAddresses(
                evm = evmAddress,
                solana = solanaAddress,
                bitcoin = bitcoinAddress,
            ),
        )

        assertEquals(setOf("eip155", "solana", "bip122"), namespaces.keys)
        assertEquals(evmChainList.map { it.chainId }, namespaces.getValue("eip155").chains)
        assertTrue(namespaces.getValue("eip155").accounts.contains("${Chain.Ethereum.chainId}:$evmAddress"))
        assertFalse(namespaces.getValue("eip155").methods.contains(Method.SolanaSignMessage.name))
        assertEquals(listOf(Chain.Solana.chainId), namespaces.getValue("solana").chains)
        assertEquals(listOf("${Chain.Solana.chainId}:$solanaAddress"), namespaces.getValue("solana").accounts)
        assertEquals(listOf(Chain.Bitcoin.chainId), namespaces.getValue("bip122").chains)
        assertEquals(listOf("${Chain.Bitcoin.chainId}:$bitcoinAddress"), namespaces.getValue("bip122").accounts)
        assertTrue(namespaces.getValue("bip122").methods.contains(Method.BtcGetAccountAddresses.name))
    }

    @Test
    fun supportedNamespacesSkipBlankAddresses() {
        val namespaces = getSupportedNamespaces(
            WalletConnectAddresses(
                evm = "0x1111111111111111111111111111111111111111",
                solana = "",
                bitcoin = "",
            ),
        )

        assertEquals(setOf("eip155"), namespaces.keys)
    }

    @Test
    fun walletConnectAddressesSelectAccountByChainId() {
        val addresses = WalletConnectAddresses(
            evm = "0x1111111111111111111111111111111111111111",
            solana = "So11111111111111111111111111111111111111112",
            bitcoin = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu",
        )

        assertEquals(addresses.evm, addresses.accountForChainId(Chain.Base.chainId))
        assertEquals(addresses.solana, addresses.accountForChainId(Chain.Solana.chainId))
        assertEquals(addresses.bitcoin, addresses.accountForChainId(Chain.Bitcoin.chainId))
    }

    @Test
    fun walletConnectMethodsAreScopedToTheirChainNamespace() {
        assertTrue(isSupportedMethodForChain(Method.BtcSignMessage.name, Chain.Bitcoin.chainId))
        assertFalse(isSupportedMethodForChain(Method.BtcSignMessage.name, Chain.Ethereum.chainId))
        assertFalse(isSupportedMethodForChain(Method.BtcSignMessage.name, Chain.Solana.chainId))
        assertTrue(isSupportedMethodForChain(Method.ETHSendTransaction.name, Chain.Base.chainId))
        assertFalse(isSupportedMethodForChain(Method.ETHSendTransaction.name, Chain.Bitcoin.chainId))
    }

    @Test
    fun proposalAccountTextDoesNotFallBackToEvmWhenProposalHasNoSupportedAccount() {
        val addresses = WalletConnectAddresses(
            evm = "0x1111111111111111111111111111111111111111",
            solana = "",
            bitcoin = "",
        )

        assertEquals("", formatProposalAccountText(setOf(Chain.Solana.chainId), addresses))
    }

    @Test
    fun sessionNamespaceUpdateReturnsNullWhenWalletNoLongerHasAConnectedChainAddress() {
        val namespaces =
            mapOf(
                "eip155" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Ethereum.chainId),
                        accounts = listOf("${Chain.Ethereum.chainId}:0x1111111111111111111111111111111111111111"),
                        methods = evmSupportedMethods,
                        events = emptyList(),
                    ),
                "bip122" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Bitcoin.chainId),
                        accounts = listOf("${Chain.Bitcoin.chainId}:bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"),
                        methods = bitcoinSupportedMethods,
                        events = emptyList(),
                    ),
            )

        val updated =
            buildUpdatedNamespaces(
                namespaces,
                WalletConnectAddresses(
                    evm = "0x2222222222222222222222222222222222222222",
                    solana = "So11111111111111111111111111111111111111112",
                    bitcoin = "",
                ),
            )

        assertNull(updated)
    }

    @Test
    fun sessionNamespaceUpdateReplacesEveryConnectedChainAccount() {
        val evmAddress = "0x2222222222222222222222222222222222222222"
        val solanaAddress = "So11111111111111111111111111111111111111112"
        val bitcoinAddress = "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu"
        val namespaces =
            mapOf(
                "eip155" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Ethereum.chainId, Chain.Base.chainId),
                        accounts = listOf(
                            "${Chain.Ethereum.chainId}:0x1111111111111111111111111111111111111111",
                            "${Chain.Base.chainId}:0x1111111111111111111111111111111111111111",
                        ),
                        methods = evmSupportedMethods,
                        events = emptyList(),
                    ),
                "solana" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Solana.chainId),
                        accounts = listOf("${Chain.Solana.chainId}:OldSolanaAddress"),
                        methods = solanaSupportedMethods,
                        events = emptyList(),
                    ),
                "bip122" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Bitcoin.chainId),
                        accounts = listOf("${Chain.Bitcoin.chainId}:OldBitcoinAddress"),
                        methods = bitcoinSupportedMethods,
                        events = emptyList(),
                    ),
            )

        val updated =
            buildUpdatedNamespaces(
                namespaces,
                WalletConnectAddresses(
                    evm = evmAddress,
                    solana = solanaAddress,
                    bitcoin = bitcoinAddress,
                ),
            )

        assertEquals(
            listOf("${Chain.Ethereum.chainId}:$evmAddress", "${Chain.Base.chainId}:$evmAddress"),
            updated?.getValue("eip155")?.accounts,
        )
        assertEquals(listOf("${Chain.Solana.chainId}:$solanaAddress"), updated?.getValue("solana")?.accounts)
        assertEquals(listOf("${Chain.Bitcoin.chainId}:$bitcoinAddress"), updated?.getValue("bip122")?.accounts)
    }
}
