package one.mixin.android.extension

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.ui.common.WebBottomSheetDialogFragment

fun Context.openWebBottomSheet(
    url: String,
    title: String,
): Boolean {
    val activity = findFragmentActivityOrNull() ?: return false
    return activity.showWebBottomSheet(url, title)
}

fun Fragment.openWebBottomSheet(
    url: String,
    title: String,
) {
    if (!WebBottomSheetDialogFragment.show(parentFragmentManager, url, title)) {
        val context = context ?: return
        if (!context.openWebBottomSheet(url, title)) {
            context.openExternalUrl(url)
        }
    }
}

fun Context.openCustomerService() {
    if (openWebBottomSheet(Constants.HelpLink.CUSTOMER_SERVICE, getString(R.string.Customer_Service))) {
        return
    }
    openExternalUrl(Constants.HelpLink.CUSTOMER_SERVICE)
}

fun Fragment.openCustomerService() {
    openWebBottomSheet(Constants.HelpLink.CUSTOMER_SERVICE, getString(R.string.Customer_Service))
}

private fun FragmentActivity.showWebBottomSheet(
    url: String,
    title: String,
): Boolean {
    if (isFinishing || isDestroyed) {
        return false
    }
    return WebBottomSheetDialogFragment.show(supportFragmentManager, url, title)
}
