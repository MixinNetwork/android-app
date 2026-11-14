package one.mixin.android.ui.web

import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.JavaScriptExecutionException
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewOutcomeReceiver
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BotSigningBridgeTest {
    private val pageUrl = "https://bot.example/earn"
    private val arguments = listOf("app-id", "false", "POST", "/transactions", "{}", "callbacks.signed")
    private val webView = WebView(ApplicationProvider.getApplicationContext()).apply { loadUrl(pageUrl) }
    private val bridge = BotSigningBridge()
    private val responses = mutableListOf<String>()
    private val reply = object : JavaScriptReplyProxy() {
        override fun postMessage(message: String) = error("Callbacks must execute in the originating frame")

        override fun postMessage(arrayBuffer: ByteArray) = error("Unexpected binary reply")

        override fun executeJavaScript(script: String, receiver: WebViewOutcomeReceiver<String, JavaScriptExecutionException>?) {
            responses += script
        }
    }
    private val requests = mutableListOf<BotSigningBridge.Request>()

    private fun post(origin: String = "https://bot.example", mainFrame: Boolean = true, data: String = JSONArray(arguments).toString()) {
        bridge.onPostMessage(webView, WebMessageCompat(data), Uri.parse(origin), mainFrame, reply)
    }

    @Test
    fun mainFrameRequestPreservesArgumentsAndCallbackShape() {
        bridge.onRequest = requests::add
        post()
        val request = requests.single()
        assertEquals(pageUrl, request.pageUrl)
        assertEquals(arguments, request.arguments)
        assertTrue(request.isCurrent())
        request.respond("123", "signature")
        assertEquals("callbacks.signed(\"123\",\"signature\");", responses.single())
    }

    @Test
    fun nativeFrameAndOriginChecksRejectIframeAndOtherOrigins() {
        bridge.onRequest = requests::add
        post(mainFrame = false)
        post(origin = "https://evil.example")
        post(origin = "https://bot.example:8443")
        post(origin = "http://bot.example")
        post(origin = "null")
        post(origin = "file:///")
        assertTrue(requests.isEmpty())
        post(origin = "https://bot.example:443")
        assertEquals(1, requests.size)
    }

    @Test
    fun navigationDuringAsyncWorkDiscardsRequestAndReply() {
        bridge.onRequest = requests::add
        post()
        val request = requests.single()
        webView.loadUrl("https://bot.example/other")
        assertFalse(request.isCurrent())
        request.respond("123", "signature")
        assertTrue(responses.isEmpty())
    }

    @Test
    fun reloadOrNavigateAwayAndBackInvalidatesSameUrlRequest() {
        bridge.onRequest = requests::add
        post()
        val request = requests.single()
        bridge.invalidate()
        assertFalse(request.isCurrent())
        request.respond("123", "signature")
        assertTrue(responses.isEmpty())
    }

    @Test
    fun detachInvalidatesPendingWorkAndRestoreUsesNewHandler() {
        bridge.onRequest = requests::add
        post()
        val oldRequest = requests.single()
        bridge.detach()
        post()
        assertEquals(1, requests.size)
        bridge.onRequest = requests::add
        assertFalse(oldRequest.isCurrent())
        oldRequest.respond("old", "signature")
        assertTrue(responses.isEmpty())
        post()
        assertTrue(requests.last().isCurrent())
        requests.last().respond("[]")
        assertEquals("callbacks.signed(\"[]\");", responses.single())
    }

    @Test
    fun malformedArgumentsAndCallbackInjectionNeverDispatch() {
        bridge.onRequest = requests::add
        post(data = "not json")
        post(data = "[]")
        post(data = "{}")
        post(data = JSONArray(arguments).put(1, 1).toString())
        post(data = JSONArray(arguments).put(4, true).toString())
        post(data = JSONArray(arguments).put(5, "callback);alert(1)//").toString())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun acceptsBooleanReloadFlagAndIgnoresTrailingArguments() {
        bridge.onRequest = requests::add
        post(data = JSONArray(arguments + "extra").put(1, true).toString())
        post(data = JSONArray(arguments).put(1, false).toString())
        assertEquals(listOf("true", "false"), requests.map { it.arguments[1] })
        assertTrue(requests.all { it.arguments.size == 6 })
    }

    @Test
    fun callbackArgumentsAreQuotedRatherThanInterpretedAsCode() {
        bridge.onRequest = requests::add
        post()
        requests.single().respond("123", "\");alert(1)//\n")
        assertEquals("callbacks.signed(\"123\",\"\\\");alert(1)//\\n\");", responses.single())
    }

    @Test
    fun legacyNativeInterfaceCannotBypassOriginChecks() {
        assertFalse(
            WebFragment.WebAppInterface::class.java.methods.any {
                it.name == "signBotSignature" && it.isAnnotationPresent(JavascriptInterface::class.java)
            },
        )
    }

    @Test
    fun originRulesUseExactHttpOriginsOnly() {
        assertEquals("https://bot.example", botSigningOrigin("https://bot.example:443/earn/?a=1#hash"))
        assertEquals("https://bot.example:8443", botSigningOrigin("https://bot.example:8443/"))
        assertNull(botSigningOrigin("https://*.example/"))
        assertNull(botSigningOrigin("file:///earn"))
        assertNull(botSigningOrigin("null"))
    }

    @Test
    fun signingResourceRequiresExactPathOrExplicitDirectoryRule() {
        val exact = listOf("https://bot.example/earn")
        val directory = listOf("https://bot.example/earn/")
        assertTrue("https://bot.example/earn?q=1#section".matchesBotSigningResource(exact))
        assertFalse("https://bot.example/earn-evil".matchesBotSigningResource(exact))
        assertFalse("https://bot.example/earn/child".matchesBotSigningResource(exact))
        assertFalse("https://bot.example/earn/".matchesBotSigningResource(exact))
        assertFalse("https://bot.example/earn".matchesBotSigningResource(directory))
        assertTrue("https://bot.example/earn/".matchesBotSigningResource(directory))
        assertTrue("https://bot.example/earn/child".matchesBotSigningResource(directory))
        assertFalse("https://bot.example/earn%2Fchild".matchesBotSigningResource(directory))
        assertFalse("https://bot.example/earn/../other".matchesBotSigningResource(directory))
        assertFalse("https://bot.example/earn/%2e%2e/other".matchesBotSigningResource(directory))
    }

    @Test
    fun signingResourceRequiresExactOriginAndNormalizesDefaultPorts() {
        val rules = listOf("https://bot.example/earn")
        assertTrue("https://bot.example:443/earn".matchesBotSigningResource(rules))
        assertTrue("HTTPS://BOT.EXAMPLE/earn".matchesBotSigningResource(rules))
        assertFalse("https://bot.example:8443/earn".matchesBotSigningResource(rules))
        assertFalse("http://bot.example/earn".matchesBotSigningResource(rules))
        assertFalse("https://bot.example@evil.example/earn".matchesBotSigningResource(rules))
        assertFalse("https://sub.bot.example/earn".matchesBotSigningResource(rules))
        assertFalse("https://sub.bot.example/earn".matchesBotSigningResource(listOf("https://*.bot.example/")))
    }

    @Test
    fun signingResourceKeepsBareHostRootRulesAndRejectsMissingRules() {
        assertTrue("https://bot.example/path".matchesBotSigningResource(listOf("https://bot.example")))
        assertTrue("https://bot.example/path".matchesBotSigningResource(listOf("https://bot.example/")))
        assertTrue("https://bot.example".matchesBotSigningResource(listOf("https://bot.example/")))
        assertFalse(pageUrl.matchesBotSigningResource(null))
        assertFalse(pageUrl.matchesBotSigningResource(emptyList()))
        assertFalse(pageUrl.matchesBotSigningResource(listOf("not a url")))
        assertFalse("file:///earn".matchesBotSigningResource(listOf("file:///")))
    }
}
