package one.mixin.android.util

import junit.framework.TestCase
import org.junit.Test
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class URLDecoderTest : TestCase() {

    @Test
    @Throws(Exception::class)
    fun test_decodeLjava_lang_String() {
        val URL = "http://mixin.one"
        val URL2 = "telnet://mixin.one:400"
        val URL3 = "file://myServer.org/a file with spaces.jpg"
        assertTrue(
            "1. Incorrect encoding/decoding",
            URLDecoder.decode(
                URLEncoder.encode(URL, UTF_8.name()),
            ) == URL,
        )
        assertTrue(
            "2. Incorrect encoding/decoding",
            URLDecoder.decode(
                URLEncoder.encode(URL2, UTF_8.name()),
            ) == URL2,
        )
        assertTrue(
            "3. Incorrect encoding/decoding",
            URLDecoder.decode(
                URLEncoder.encode(URL3, UTF_8.name()),
            ) == URL3,
        )
    }

    @Test
    fun test_decodeLjava_lang_String_Ljava_lang_String() {
        val enc = UTF_8.name()
        val urls = arrayOf(
            "http://mixin.one/test?hl=en&q=te+st",
            "file://a+b/c/d.e-f*g_+l",
            "jar:file://a.jar+!/b.c/",
            "ftp://test:pwd@localhost:2121/%D0%9C",
            "%D0%A2%D0%B5%D1%81%D1%82+URL+for+test",
        )
        val expected = arrayOf(
            "http://mixin.one/test?hl=en&q=te st",
            "file://a b/c/d.e-f*g_ l",
            "jar:file://a.jar !/b.c/",
        )
        for (i in 0 until urls.size - 2) {
            try {
                assertEquals(expected[i], URLDecoder.decode(urls[i], enc))
            } catch (e: UnsupportedEncodingException) {
                fail("UnsupportedEncodingException: " + e.message)
            }
        }
        try {
            URLDecoder.decode(urls[urls.size - 2], enc)
            URLDecoder.decode(urls[urls.size - 1], enc)
        } catch (e: UnsupportedEncodingException) {
            fail("UnsupportedEncodingException: " + e.message)
        }
    }
}
