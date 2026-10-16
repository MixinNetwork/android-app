package one.mixin.android.extension

import android.app.Application
import android.net.Uri
import one.mixin.android.Constants
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppSchemeTest {
    @Test
    fun appPageLinksOpenHomeAndForwardPage() {
        for (scheme in listOf(Constants.Scheme.APPS, Constants.Scheme.HTTPS_APPS)) {
            val base = "$scheme/${Constants.MIXIN_CASH_USER_ID}"
            val uri = Uri.parse("$base?page=add-cash-ban")
            assertTrue(uri.shouldOpenAppHome())
            assertEquals(
                "https://example.com?existing=1&page=add-cash-ban",
                "https://example.com?existing=1".appendQueryParamsFromOtherUri(uri),
            )
            assertTrue(Uri.parse("$base?action=open").shouldOpenAppHome())
            for (query in listOf("", "?page=", "?page=%20", "?action=other")) {
                assertFalse(Uri.parse("$base$query").shouldOpenAppHome())
            }
        }
        for (scheme in listOf(Constants.Scheme.USERS, Constants.Scheme.HTTPS_USERS)) {
            assertFalse(Uri.parse("$scheme/${Constants.MIXIN_CASH_USER_ID}?page=add-cash-ban").shouldOpenAppHome())
        }
    }
}
