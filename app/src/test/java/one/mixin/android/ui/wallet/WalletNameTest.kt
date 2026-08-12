package one.mixin.android.ui.wallet

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletNameTest {
    @Test
    fun `first wallet name starts at one`() {
        assertEquals("Common Wallet 1", nextWalletName(emptyList(), "Common Wallet"))
    }

    @Test
    fun `next wallet name follows highest matching suffix`() {
        assertEquals(
            "Common Wallet 8",
            nextWalletName(
                listOf(null, "Common Wallet 2", "Other Wallet 20", "Common Wallet 7", "Common Wallet invalid"),
                "Common Wallet",
            ),
        )
    }

    @Test
    fun `wallet name treats prefix as plain text`() {
        assertEquals(
            "Wallet (Main) 4",
            nextWalletName(listOf("Wallet (Main) 3"), "Wallet (Main)"),
        )
    }
}
