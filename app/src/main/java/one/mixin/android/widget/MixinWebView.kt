package one.mixin.android.widget

import android.content.Context
import android.view.KeyEvent
import android.webkit.WebView
import one.mixin.android.ui.web.BotSigningBridge

class MixinWebView(context: Context) : WebView(context) {
    internal var botSigningBridge: BotSigningBridge? = null

    override fun destroy() {
        botSigningBridge?.detach()
        super.destroy()
    }

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
}
