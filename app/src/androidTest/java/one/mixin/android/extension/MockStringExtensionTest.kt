package one.mixin.android.extension

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
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
