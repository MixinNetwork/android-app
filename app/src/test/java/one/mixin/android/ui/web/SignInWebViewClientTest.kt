package one.mixin.android.ui.web

import android.app.Application
import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [35])
class SignInWebViewClientTest {
    private var currentUrl = "https://example.com/"
    private val view = object : WebView(RuntimeEnvironment.getApplication()) {
        override fun getUrl(): String = currentUrl
    }

    @Test
    fun permitsSameOriginHistoryChanges() {
        val client = loadedClient()
        for (url in listOf("https://example.com/login", "https://example.com/?login=true#account", "https://example.com:443/other")) {
            currentUrl = url
            assertEquals("https://example.com/", client.getSignInPageUrl(view))
        }
    }

    @Test
    fun rejectsDifferentOriginsAndUnknownPages() {
        assertNull(SignInWebViewClient().getSignInPageUrl(view))
        val client = loadedClient()
        for (url in listOf("https://other.example/", "http://example.com/", "https://example.com:8443/", "about:blank")) {
            currentUrl = url
            assertNull(client.getSignInPageUrl(view))
        }
    }

    @Test
    fun preservesValidatedPageAcrossFloatingAndRestoredClients() {
        val floating = SignInWebViewClient(loadedClient())
        currentUrl = "https://example.com/login"
        assertEquals("https://example.com/", SignInWebViewClient(floating).getSignInPageUrl(view))
    }

    @Test
    fun invalidatesNavigationWhileFloatingUntilNewPageFinishes() {
        val floating = SignInWebViewClient(loadedClient())
        currentUrl = "https://example.com/next"
        floating.onPageStarted(view, currentUrl, null)
        val restored = SignInWebViewClient(floating)
        assertNull(restored.getSignInPageUrl(view))
        restored.onPageFinished(view, "https://example.com/")
        assertNull(restored.getSignInPageUrl(view))
        restored.onPageFinished(view, currentUrl)
        assertEquals(currentUrl, restored.getSignInPageUrl(view))
    }

    @Test
    fun preservesLoadingFailureAcrossClientReplacement() {
        val floating = SignInWebViewClient(loadedClient())
        floating.onReceivedError(view, -2, "Failed", currentUrl)
        val restored = SignInWebViewClient(floating)
        restored.onPageFinished(view, currentUrl)
        assertNull(restored.getSignInPageUrl(view))
        restored.onPageStarted(view, currentUrl, null)
        restored.onPageFinished(view, currentUrl)
        assertEquals(currentUrl, restored.getSignInPageUrl(view))
    }

    @Test
    fun sslFailureCannotBeRevalidatedByPageFinishedOrRestoration() {
        val client = loadedClient()
        client.onReceivedSslError(view, null, null)
        assertNull(client.getSignInPageUrl(view))
        val restored = SignInWebViewClient(client)
        restored.onPageFinished(view, currentUrl)
        assertNull(restored.getSignInPageUrl(view))
        restored.onPageStarted(view, currentUrl, null)
        restored.onPageFinished(view, currentUrl)
        assertEquals(currentUrl, restored.getSignInPageUrl(view))
    }

    private fun loadedClient() = SignInWebViewClient().apply {
        onPageStarted(view, currentUrl, null)
        onPageFinished(view, currentUrl)
    }
}
