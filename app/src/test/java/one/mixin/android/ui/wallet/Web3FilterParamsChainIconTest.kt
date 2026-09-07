package one.mixin.android.ui.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Web3FilterParamsChainIconTest {
    @Test
    fun transactionQueryUsesChainMetadataForNetworkBadge() {
        val sql = Web3FilterParams(walletId = "wallet").buildQuery().sql

        assertTrue(sql.contains("c.icon_url as chain_icon_url"))
        assertTrue(sql.contains("LEFT JOIN chains c ON c.chain_id = w.chain_id"))
        assertFalse(sql.contains("LEFT JOIN tokens c ON c.asset_id = w.chain_id"))
    }
}
