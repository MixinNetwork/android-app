package one.mixin.android.web3

import android.net.Uri
import one.mixin.android.Constants
import timber.log.Timber

fun convertWcLink(url: String): Uri? {
    try {
        val wcUrl =
            if (url.startsWith(Constants.Scheme.HTTPS_MIXIN_WC)) {
                Uri.parse(url).getQueryParameter("uri")
            } else if (url.startsWith(Constants.Scheme.MIXIN_WC)) {
                Uri.parse(url).getQueryParameter("uri")
            } else {
                url
            }
        if (wcUrl == null) return null
        val wcUri =
            wcUrl.run {
                when {
                    startsWith("wc://") -> this
                    startsWith("wc:/") -> replace("wc:/", "wc://")
                    else -> replace("wc:", "wc://")
                }
            }
        val uri = Uri.parse(wcUri)
        val sumKey = uri.getQueryParameter("symKey")
        if (sumKey != null) {
            return Uri.parse(wcUri)
        }
        return null
    } catch (e: Exception) {
        Timber.e(e)
        return null
    }
}
