package one.mixin.android.extension

import android.net.Uri
import androidx.core.net.toUri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MockStringExtensionTest {

    @Test
    fun testAppendQueryParamsFromOtherUri() {
        val t1 = "http://example.com"
        val t2 = "http://example.com?ab=cd"

        val u1 = "http://example.com?a=b&c=d&e=f"
        val u2 = "http://example.com?action=open&a=b&c=d&e=f"

        assertEquals("http://example.com?a=b&c=d&e=f", Uri.decode(t1.appendQueryParamsFromOtherUri(u1.toUri())))
        assertEquals("http://example.com?a=b&c=d&e=f", Uri.decode(t1.appendQueryParamsFromOtherUri(u2.toUri())))
        assertEquals("http://example.com?ab=cd&a=b&c=d&e=f", Uri.decode(t2.appendQueryParamsFromOtherUri(u1.toUri())))
        assertEquals("http://example.com?ab=cd&a=b&c=d&e=f", Uri.decode(t2.appendQueryParamsFromOtherUri(u2.toUri())))
    }
}
