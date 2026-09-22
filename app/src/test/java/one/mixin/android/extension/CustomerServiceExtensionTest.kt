package one.mixin.android.extension

import android.app.Application
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CustomerServiceExtensionTest {
    @Test
    fun matchesSupportPageWithoutInterceptingOtherUrls() {
        listOf(
            "https://mixin.one/support",
            "https://mixin.one/support/",
            "https://mixin.one/support?source=wallet#contact",
            "HTTPS://MIXIN.ONE/support",
        ).forEach { assertTrue(isCustomerServiceUrl(it), it) }

        listOf(
            "https://mixin.one/support-other",
            "https://mixin.one/support/article",
            "https://mixin.one/pay",
            "https://support.mixin.one/en/",
            "https://mixin.one.example.com/support",
            "http://mixin.one/support",
            "mailto:support@example.com",
            "not a url",
        ).forEach { assertFalse(isCustomerServiceUrl(it), it) }
    }
}
