package one.mixin.android.ui.web

private val javaScriptCallbackRegex = Regex("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$")

internal fun isValidJavaScriptCallback(callbackFunction: String): Boolean = javaScriptCallbackRegex.matches(callbackFunction)
