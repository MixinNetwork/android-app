package one.mixin.android.widget

import android.content.Context
import android.view.KeyEvent
import android.webkit.WebView
import one.mixin.android.ui.web.WebViewBridgeBinding

class MixinWebView(context: Context) : WebView(context) {
    private val bridgeBindingDelegate = lazy(LazyThreadSafetyMode.NONE) {
        WebViewBridgeBinding(this)
    }
    val bridgeBinding: WebViewBridgeBinding by bridgeBindingDelegate

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun destroy() {
        if (bridgeBindingDelegate.isInitialized()) {
            bridgeBinding.dispose()
        }
        super.destroy()
    }
}
