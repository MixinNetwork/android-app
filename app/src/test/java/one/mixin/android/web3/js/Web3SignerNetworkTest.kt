package one.mixin.android.web3.js

import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class Web3SignerNetworkTest {
    @Test
    fun pearlUsesStableNetworkName() {
        assertEquals("pearl", Web3Signer.JsSignerNetwork.Pearl.name)
    }

    @Test
    fun xLayerHexReferenceResolvesForPersistedChainState() {
        assertEquals(Chain.XLayer, findChainByHexReference("0xc4"))
    }

    @Test
    fun evmRequestAfterSolanaReturnsToLastEvmChain() {
        // A dapp on Arbitrum makes a Solana request, then signs EVM typed data
        // for Arbitrum again: it must not land on Ethereum.
        assertEquals(Chain.Arbitrum, evmChainAfter(Chain.Solana, Chain.Arbitrum))
        // An EVM chain stays as it is.
        assertEquals(Chain.Base, evmChainAfter(Chain.Base, Chain.Arbitrum))
        // Without an EVM chain to return to, Ethereum is the default.
        assertEquals(Chain.Ethereum, evmChainAfter(Chain.Solana, null))
        assertEquals(Chain.Ethereum, evmChainAfter(Chain.Solana, Chain.Solana))
    }
}
