package one.mixin.android.ui.web

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class BotSigningBridge : WebViewCompat.WebMessageListener {
    var onRequest: ((Request) -> Unit)? = null
    private var generation = 0L

    fun install(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD)
        ) {
            // Transport availability is not authorization: botSign checks the current bot rules.
            WebViewCompat.addWebMessageListener(webView, BRIDGE_NAME, setOf("*"), this)
            WebViewCompat.addDocumentStartJavaScript(webView, SCRIPT, setOf("*"))
        }
    }

    override fun onPostMessage(view: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy) {
        val handler = onRequest ?: return
        val pageUrl = view.url ?: return
        if (!isTrustedBotSigningSource(pageUrl, sourceOrigin, isMainFrame)) return
        if (message.type != WebMessageCompat.TYPE_STRING) return
        val arguments = parseBotSigningArguments(message.data ?: return) ?: return
        val requestGeneration = generation
        handler(
            Request(pageUrl, arguments, replyProxy) {
                onRequest != null && generation == requestGeneration && view.url == pageUrl
            },
        )
    }

    fun invalidate() {
        generation++
    }

    fun detach() {
        invalidate()
        onRequest = null
    }

    class Request(
        val pageUrl: String,
        val arguments: List<String>,
        private val reply: JavaScriptReplyProxy,
        val isCurrent: () -> Boolean,
    ) {
        fun respond(vararg values: String) {
            if (!isCurrent()) return
            reply.executeJavaScript(
                "${arguments[5]}(${values.joinToString(",") { JSONObject.quote(it) }});",
                null,
            )
        }
    }

    companion object {
        private const val BRIDGE_NAME = "_MixinBotSigning"

        internal val SCRIPT = """
            (function() {
                if (window !== window.top || !window.MixinContext || !window._MixinBotSigning) return;
                const bridge = window._MixinBotSigning;
                window.MixinContext.signBotSignature = function(args) {
                    bridge.postMessage(JSON.stringify(args));
                };
            })();
        """.trimIndent()
    }
}

internal fun botSigningOrigin(url: String): String? = url.toHttpUrlOrNull()
    ?.takeIf { '*' !in it.host }
    ?.newBuilder()
    ?.username("")
    ?.password("")
    ?.encodedPath("/")
    ?.query(null)
    ?.fragment(null)
    ?.build()
    ?.toString()
    ?.removeSuffix("/")

internal fun isTrustedBotSigningSource(pageUrl: String, sourceOrigin: Uri, isMainFrame: Boolean): Boolean {
    if (!isMainFrame) return false
    val origin = botSigningOrigin(sourceOrigin.toString()) ?: return false
    return origin == botSigningOrigin(pageUrl)
}

internal fun parseBotSigningArguments(message: String): List<String>? {
    val arguments = try {
        JSONArray(message)
    } catch (_: JSONException) {
        return null
    }
    if (arguments.length() < 6) return null
    val values = (0 until 6).map { index ->
        when (val value = arguments.opt(index)) {
            is String -> value
            is Boolean -> if (index == 1) value.toString() else return null
            else -> return null
        }
    }
    return values.takeIf { isValidJavaScriptCallback(it[5]) }
}

internal fun String.matchesBotSigningResource(resourcePatterns: Collection<String>?): Boolean {
    val url = toHttpUrlOrNull() ?: return false
    return resourcePatterns?.any { pattern ->
        val rule = pattern.toHttpUrlOrNull() ?: return@any false
        url.scheme == rule.scheme && url.host == rule.host && url.port == rule.port &&
            (url.encodedPath == rule.encodedPath ||
                (rule.encodedPath.endsWith('/') && url.encodedPath.startsWith(rule.encodedPath)))
    } == true
}
