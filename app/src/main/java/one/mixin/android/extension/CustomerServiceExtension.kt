package one.mixin.android.extension

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.ui.common.WebBottomSheetDialogFragment
import one.mixin.android.util.analytics.AnalyticsTracker

fun Context.openWebBottomSheet(
    url: String,
    title: String,
    subtitle: String? = null,
): Boolean {
    val activity = findFragmentActivityOrNull() ?: return false
    return activity.showWebBottomSheet(url, title, subtitle)
}

fun Fragment.openWebBottomSheet(
    url: String,
    title: String,
    subtitle: String? = null,
) {
    if (!WebBottomSheetDialogFragment.show(parentFragmentManager, url, title, subtitle)) {
        val context = context ?: return
        if (!context.openWebBottomSheet(url, title, subtitle)) {
            context.openExternalUrl(url)
        }
    }
}

fun isCustomerServiceUrl(url: String): Boolean {
    if (url.startsWith(Constants.HelpLink.CUSTOMER_SERVICE)) {
        return true
    }
    val currentUri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val targetUri = Uri.parse(Constants.HelpLink.CUSTOMER_SERVICE)
    val currentPath = currentUri.path.orEmpty().trimEnd('/')
    val targetPath = targetUri.path.orEmpty().trimEnd('/')
    val currentWebsiteId = currentUri.getQueryParameter("website_id")
    val targetWebsiteId = targetUri.getQueryParameter("website_id")
    return currentUri.scheme.equals(targetUri.scheme, ignoreCase = true) &&
        currentUri.host.equals(targetUri.host, ignoreCase = true) &&
        currentPath == targetPath &&
        (currentWebsiteId.isNullOrBlank() || currentWebsiteId == targetWebsiteId)
}

fun Context.openCustomerServiceIfMatched(
    url: String,
    source: String? = null,
    wallet: String = AnalyticsTracker.TradeWallet.MAIN,
): Boolean {
    if (!isCustomerServiceUrl(url)) {
        return false
    }
    openCustomerService(source = source, wallet = wallet)
    return true
}

fun Context.openCustomerService(
    source: String? = null,
    wallet: String = AnalyticsTracker.TradeWallet.MAIN,
) {
    if (
        openWebBottomSheet(
            Constants.HelpLink.CUSTOMER_SERVICE,
            getString(R.string.mixin_support),
            getString(R.string.ask_me_anything),
        )
    ) {
        source?.let { AnalyticsTracker.trackCustomerServiceDialog(source = it, wallet = wallet) }
        return
    }
    openExternalUrl(Constants.HelpLink.CUSTOMER_SERVICE)
}

fun Fragment.openCustomerService(
    source: String? = null,
    wallet: String = AnalyticsTracker.TradeWallet.MAIN,
) {
    context?.openCustomerService(source = source, wallet = wallet)
}

private fun FragmentActivity.showWebBottomSheet(
    url: String,
    title: String,
    subtitle: String? = null,
): Boolean {
    if (isFinishing || isDestroyed) {
        return false
    }
    return WebBottomSheetDialogFragment.show(supportFragmentManager, url, title, subtitle)
}
