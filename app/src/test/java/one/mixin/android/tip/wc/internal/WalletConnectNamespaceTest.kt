package one.mixin.android.tip.wc.internal

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
