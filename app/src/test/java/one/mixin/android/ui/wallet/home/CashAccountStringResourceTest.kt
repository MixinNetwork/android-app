package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CashAccountStringResourceTest {
    @Test
    fun cashAccountApyIsNotHardcodedInStringResources() {
        val stringsFile = listOf(
            File("app/src/main/res/values/strings.xml"),
            File("src/main/res/values/strings.xml"),
        ).firstOrNull { it.exists() }

        assertTrue("strings.xml should exist", stringsFile != null)
        assertFalse(
            "cash_account_apy should come from CashAccount.rewardApy",
            stringsFile!!.readText().contains("name=\"cash_account_apy\""),
        )
    }
}
