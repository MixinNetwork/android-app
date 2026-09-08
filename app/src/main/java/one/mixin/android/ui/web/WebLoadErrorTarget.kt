package one.mixin.android.ui.web

import one.mixin.android.vo.App

internal fun webLoadErrorTarget(
    app: App?,
    failingUrl: String?,
): String? = app?.let { "${it.name} (${it.appNumber})" } ?: failingUrl
