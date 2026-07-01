package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CashAccountStringResourceTest {
    @Test
    fun cashAccountApyUsesRewardApyPlaceholder() {
        val expectedResources = mapOf(
            "app/src/main/res/values/strings.xml" to """<string name="cash_account_apy">%1${'$'}s APY</string>""",
            "app/src/main/res/values-zh-rCN/strings.xml" to """<string name="cash_account_apy">年化 %1${'$'}s</string>""",
            "app/src/main/res/values-zh-rTW/strings.xml" to """<string name="cash_account_apy">年化 %1${'$'}s</string>""",
        )

        expectedResources.forEach { (path, expectedString) ->
            val stringsFile = listOf(
                File(path),
                File(path.removePrefix("app/")),
            ).firstOrNull { it.exists() }

            assertTrue("$path should exist", stringsFile != null)
            assertTrue(
                "$path should format cash_account_apy from CashAccount.rewardApy",
                stringsFile!!.readText().contains(expectedString),
            )
        }
    }
}
