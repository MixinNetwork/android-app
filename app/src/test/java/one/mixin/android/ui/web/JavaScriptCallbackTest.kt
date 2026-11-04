package one.mixin.android.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaScriptCallbackTest {
    @Test
    fun acceptsIdentifiersAndMemberPaths() {
        listOf("callback", "_callback0", "\$", "\$callback", "window.mixin.callbacks.done_1", "_bridge.\$done").forEach {
            assertTrue(it, isValidJavaScriptCallback(it))
        }
    }

    @Test
    fun rejectsExpressionsAndMalformedMemberPaths() {
        listOf(
            "",
            "0callback",
            ".callback",
            "callback.",
            "window..callback",
            "window.0callback",
            " callback",
            "callback ",
            "callback\n",
            "callback\r\n",
            "window.\ncallback",
            "callback\u2028",
            "callback\u0000",
            "回调",
            "window['callback']",
            "callback()",
            "callback;alert(1)//",
            "callback/*comment*/",
            "callback//comment",
            "callback = alert",
            "callback,alert",
            "callback'",
            "callback\"",
            "callback`",
            "callback\\u0061",
        ).forEach {
            assertFalse(it, isValidJavaScriptCallback(it))
        }
    }
}
