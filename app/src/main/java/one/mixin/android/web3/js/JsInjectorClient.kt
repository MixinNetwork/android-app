package one.mixin.android.web3.js

import android.content.Context
import androidx.annotation.RawRes
import one.mixin.android.R
import one.mixin.android.tip.wc.internal.Chain
import timber.log.Timber
import java.io.IOException

class JsInjectorClient {
    fun initJs(context: Context, chain: Chain, address: String): String {
        val initSrc = loadFile(context, rawRes = R.raw.init)
        return String.format(initSrc, chain.chainReference, chain.rpcUrl, address)
    }

    fun loadProviderJs(context: Context): String {
        return loadFile(context, R.raw.mixin_min)
    }

    companion object {
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
                return String(buffer)
            } catch (e: Exception) {
                Timber.tag("READ_JS_TAG").d(e, "Ex")
            }
            return ""
        }
    }
}
