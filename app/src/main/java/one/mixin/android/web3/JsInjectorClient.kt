package one.mixin.android.web3

import android.content.Context
import android.text.TextUtils
import androidx.annotation.RawRes
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import one.mixin.android.R
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.vo.Address
import timber.log.Timber
import java.io.IOException
import java.math.BigInteger
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.min

class JsInjectorClient(context: Context?) {
    private var chainId: Int = 0
    var walletAddress: Address? = null

    private var rpcUrl: String? = null

    fun getChainId(): Int {
        return chainId
    }

    fun setChainId(chainId: Int) {
        this.chainId = chainId
        this.rpcUrl = when(chainId){
            Chain.Ethereum.chainReference -> Chain.Ethereum.rpcUrl
            Chain.Polygon.chainReference -> Chain.Polygon.rpcUrl
            Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain.rpcUrl
            else -> ""
        }
    }

    // Set ChainId for TokenScript inject
    fun setTSChainId(chainId: Int) {
        this.chainId = chainId
        this.rpcUrl = when(chainId){
            Chain.Ethereum.chainReference -> Chain.Ethereum.rpcUrl
            Chain.Polygon.chainReference -> Chain.Polygon.rpcUrl
            Chain.BinanceSmartChain.chainReference -> Chain.BinanceSmartChain.rpcUrl
            else -> ""
        }
    }

    fun initJs(context: Context, address: String, chain: Chain): String {
        return loadInitJs(context, address, chain)
    }

    fun providerJs(context: Context): String {
        return loadFile(context, R.raw.wallet_min)
    }

    fun injectWeb3TokenInit(ctx: Context, view: String, tokenContent: String?, tokenId: BigInteger): String {
        var initSrc = loadFile(ctx, R.raw.init_token)
        //put the view in here
        val tokenIdWrapperName = "token-card-" + tokenId.toString(10)
        initSrc = String.format(initSrc, tokenContent, walletAddress, rpcUrl, chainId, tokenIdWrapperName)
        //now insert this source into the view
        // note that the <div> is not closed because it is closed in injectStyleAndWrap().
        val wrapper = "<div id=\"token-card-" + tokenId.toString(10) + "\" class=\"token-card\">"
        initSrc = "<script>\n$initSrc</script>\n$wrapper"
        return injectJS(view, initSrc)
    }

    fun injectJSAtEnd(view: String, newCode: String): String {
        val position = getEndInjectionPosition(view)
        if (position >= 0) {
            val beforeTag = view.substring(0, position)
            val afterTab = view.substring(position)
            return beforeTag + newCode + afterTab
        }
        return view
    }

    fun injectJS(html: String, js: String): String {
        if (TextUtils.isEmpty(html)) {
            return html
        }
        val position = getInjectionPosition(html)
        if (position >= 0) {
            val beforeTag = html.substring(0, position)
            val afterTab = html.substring(position)
            return beforeTag + js + afterTab
        }
        return html
    }

    private fun getInjectionPosition(body: String): Int {
        var body = body
        body = body.lowercase(Locale.getDefault())
        val ieDetectTagIndex = body.indexOf("<!--[if")
        val scriptTagIndex = body.indexOf("<script")

        var index: Int
        index = if (ieDetectTagIndex < 0) {
            scriptTagIndex
        } else {
            min(scriptTagIndex.toDouble(), ieDetectTagIndex.toDouble()).toInt()
        }
        if (index < 0) {
            index = body.indexOf("</head")
        }
        if (index < 0) {
            index = 0 //just wrap whole view
        }
        return index
    }

    private fun getEndInjectionPosition(body: String): Int {
        var body = body
        body = body.lowercase(Locale.getDefault())
        val firstIndex = body.indexOf("<script")
        val nextIndex = body.indexOf("web3", firstIndex)
        return body.indexOf("</script", nextIndex)
    }

    private fun buildRequest(url: String, headers: Map<String, String>): Request? {
        val httpUrl = url.toHttpUrlOrNull()?: return null
        val requestBuilder: Request.Builder = Request.Builder()
            .get()
            .url(httpUrl)
        val keys = headers.keys
        for (key in keys) {
            headers[key]?.let { requestBuilder.addHeader(key, it) }
        }
        return requestBuilder.build()
    }

    private fun loadInitJs(context: Context, address:String, chain:Chain): String {
        val initSrc = loadFile(context, R.raw.init)
        return String.format(initSrc, address, chain.rpcUrl, chain.chainReference)
    }

    fun injectStyleAndWrap(view: String, style: String?): String {
        var style = style
        if (style == null) style = ""
        //String injectHeader = "<head><meta name=\"viewport\" content=\"width=device-width, user-scalable=false\" /></head>";
        val injectHeader = "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, shrink-to-fit=no\" />" //iOS uses these header settings
        style = """
             <style type="text/css">
             $style.token-card {
             padding: 0pt;
             margin: 0pt;
             }</style></head><body>
             
             """.trimIndent()
        // the opening of the following </div> is in injectWeb3TokenInit();
        return "$injectHeader$style$view</div></body>"
    }

    private fun getMimeType(contentType: String): String {
        val regexResult = Pattern.compile("^.*(?=;)").matcher(contentType)
        if (regexResult.find()) {
            return regexResult.group()
        }
        return DEFAULT_MIME_TYPE
    }

    private fun getCharset(contentType: String): String {
        val regexResult = Pattern.compile("charset=([a-zA-Z0-9-]+)").matcher(contentType)
        if (regexResult.find()) {
            if (regexResult.groupCount() >= 2) {
                return regexResult.group(1)
            }
        }
        return DEFAULT_CHARSET
    }

    private fun getContentTypeHeader(response: Response): String? {
        val headers = response.headers
        var contentType: String?
        contentType = if (TextUtils.isEmpty(headers["Content-Type"])) {
            if (TextUtils.isEmpty(headers["content-Type"])) {
                "text/data; charset=utf-8"
            } else {
                headers["content-Type"]
            }
        } else {
            headers["Content-Type"]
        }
        if (contentType != null) {
            contentType = contentType.trim { it <= ' ' }
        }
        return contentType
    }

    companion object {
        private const val DEFAULT_CHARSET = "utf-8"
        private const val DEFAULT_MIME_TYPE = "text/html"
        private const val JS_TAG_TEMPLATE = "<script type=\"text/javascript\">%1\$s%2\$s</script>"

        fun loadFile(context: Context, @RawRes rawRes: Int): String {
            var buffer = ByteArray(0)
            try {
                val `in` = context.resources.openRawResource(rawRes)
                buffer = ByteArray(`in`.available())
                val len = `in`.read(buffer)
                if (len < 1) {
                    throw IOException("Nothing is read.")
                }
            } catch (ex: Exception) {
                Timber.tag("READ_JS_TAG").d(ex, "Ex")
            }

            try {
                Timber.tag("READ_JS_TAG").d("HeapSize:%s", Runtime.getRuntime().freeMemory())
                return String(buffer)
            } catch (e: Exception) {
                Timber.tag("READ_JS_TAG").d(e, "Ex")
            }
            return ""
        }
    }
}
