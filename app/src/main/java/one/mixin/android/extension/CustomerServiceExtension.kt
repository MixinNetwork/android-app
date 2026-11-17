package one.mixin.android.extension

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.SAFE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.session.Session
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
    val currentUri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val targetUri = Uri.parse(Constants.HelpLink.CUSTOMER_SERVICE)
    val currentPath = currentUri.path.orEmpty().trimEnd('/')
    val targetPath = targetUri.path.orEmpty().trimEnd('/')
    return currentUri.scheme.equals(targetUri.scheme, ignoreCase = true) &&
        currentUri.host.equals(targetUri.host, ignoreCase = true) &&
        currentPath == targetPath
}

private const val SUPPORT_PAGE = "support"

private const val SUPPORT_BOT_URL = "${Constants.Scheme.APPS}/$SAFE_BOT_USER_ID?action=open&page=$SUPPORT_PAGE"

fun isSupportBotPage(
    appId: String?,
    url: String,
): Boolean {
    if (appId != SAFE_BOT_USER_ID) return false
    val page = runCatching { Uri.parse(url).getQueryParameter("page") }.getOrNull()
    return page == SUPPORT_PAGE
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
    val activity = findFragmentActivityOrNull()
    if (Session.getAccount() == null || activity == null || activity.isFinishing || activity.isDestroyed) {
        openLegacyCustomerService(source, wallet)
        return
    }
    SUPPORT_BOT_URL.checkUserOrApp(activity, activity.supportFragmentManager, activity.lifecycleScope)
    source?.let { AnalyticsTracker.trackCustomerServiceDialog(source = it, wallet = wallet) }
}

private fun Context.openLegacyCustomerService(
    source: String?,
    wallet: String,
) {
    if (
        openWebBottomSheet(
            Constants.HelpLink.CUSTOMER_SERVICE,
            getString(R.string.mixin_support),
            getString(R.string.real_people_real_support),
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
