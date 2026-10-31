package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build

internal fun Intent.prepareForExternalWebLaunch(context: Context): Boolean =
    prepareForExternalWebLaunch(
        ownPackage = context.packageName,
        requireNonBrowser = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        newTask = context !is Activity,
    ) {
        resolveActivity(context.packageManager)?.packageName
    }

internal fun Intent.prepareForExternalWebLaunch(
    ownPackage: String,
    requireNonBrowser: Boolean,
    newTask: Boolean,
    resolvePackage: () -> String?,
): Boolean {
    component = null
    selector = null
    action = Intent.ACTION_VIEW
    addCategory(Intent.CATEGORY_BROWSABLE)
    flags = 0
    // https://developer.android.com/training/package-visibility/use-cases#let-non-browser-apps-handle-urls
    if (requireNonBrowser) {
        addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    }
    if (newTask) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return resolvePackage() != ownPackage
}
