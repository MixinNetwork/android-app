package one.mixin.android.ui.tip

import one.mixin.android.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassicUtxoBackfillPlanTest {
    @Test
    fun additionalClassicWalletUsesItsOwnIndexAndRequiresPearl() {
        val plan = requireNotNull(
            resolveClassicUtxoBackfillPlan(
                chainIds = setOf(Constants.ChainId.ETHEREUM_CHAIN_ID, Constants.ChainId.SOLANA_CHAIN_ID),
                paths = listOf("m/44'/60'/0'/0/1", "m/84'/0'/0'/0/1"),
            )
        )

        assertEquals(1, plan.derivationIndex)
        assertEquals(
            setOf(Constants.ChainId.BITCOIN_CHAIN_ID, Constants.ChainId.PEARL_CHAIN_ID),
            plan.missingChainIds,
        )
    }

    @Test
    fun defaultClassicWalletRequiresMissingPearl() {
        val plan = requireNotNull(
            resolveClassicUtxoBackfillPlan(
                chainIds = setOf(Constants.ChainId.BITCOIN_CHAIN_ID),
                paths = listOf("m/44'/60'/0'/0/0", "m/84'/0'/0'/0/0"),
            )
        )

        assertEquals(0, plan.derivationIndex)
        assertEquals(setOf(Constants.ChainId.PEARL_CHAIN_ID), plan.missingChainIds)
    }

    @Test
    fun inconsistentClassicAddressIndexesDoNotProduceBackfillPlan() {
        assertNull(
            resolveClassicUtxoBackfillPlan(
                chainIds = emptySet(),
                paths = listOf("m/44'/60'/0'/0/0", "m/84'/0'/0'/0/1"),
            )
        )
    }
}
