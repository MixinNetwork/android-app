package one.mixin.android.ui.web

import android.webkit.JavascriptInterface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import one.mixin.android.widget.MixinWebView
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class BotSigningBridgeWebViewTest {
    @Test
    fun documentStartShimPreservesNativeContextAndSigningCallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD))
        val responses = LinkedBlockingQueue<String>()
        lateinit var webView: MixinWebView
        instrumentation.runOnMainSync {
            webView = MixinWebView(instrumentation.targetContext)
        }
        try {
            instrumentation.runOnMainSync {
                webView.settings.javaScriptEnabled = true
                webView.addJavascriptInterface(TestContext(responses), "MixinContext")
                webView.botSigningBridge = BotSigningBridge().also { it.install(webView) }
                webView.botSigningBridge?.onRequest = { it.respond("123", "test-signature") }
                webView.loadDataWithBaseURL(
                    "https://bot.example/earn",
                    """
                        <html><head><meta http-equiv="Content-Security-Policy" content="script-src 'nonce-test'"></head>
                        <body><script nonce="test">
                        const signed = function(timestamp, signature) {
                            MixinContext.report(JSON.stringify([timestamp, signature, MixinContext.getContext()]));
                        };
                        MixinContext.signBotSignature(['app-id', false, 'POST', '/transactions', '{}', 'signed']);
                        </script></body></html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
            val response = responses.poll(10, TimeUnit.SECONDS)
            assertEquals(
                "The bridge must be ready before page scripts without changing the existing callback contract",
                JSONArray(listOf("123", "test-signature", "native-context")).toString(),
                response,
            )
        } finally {
            instrumentation.runOnMainSync { webView.destroy() }
        }
    }

    class TestContext(private val responses: LinkedBlockingQueue<String>) {
        @JavascriptInterface
        fun getContext() = "native-context"

        @JavascriptInterface
        fun report(value: String) {
            responses.offer(value)
        }
    }
}
