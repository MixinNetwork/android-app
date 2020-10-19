package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.collection.arrayMapOf
import one.mixin.android.vo.App
import one.mixin.android.widget.MixinWebView

fun expand(context: Context) {
    WebActivity.show(context)
    FloatingWebClip.getInstance().hide()
}

fun collapse() {
    if (clips.size > 0) {
        FloatingWebClip.getInstance().show()
    }
}

var clips = arrayMapOf<String, WebClip>()
var holdWebViews = mutableListOf<MixinWebView>()

class WebClip(val url: String, val thumb: Bitmap, val app: App?, val name: String?)

fun holdClip(activity: Activity, webView: MixinWebView, webClip: WebClip) {
    if (!clips.contains(webClip.url)) {
        if (clips.size >= 6) {
            // Todo
        } else {
            clips[webClip.url] = webClip
            holdWebViews.add(webView)
            FloatingWebClip.getInstance().show(activity)
        }
    } else {
        // Todo
    }
}

fun releaseClip(webClip: WebClip) {
    if (clips.contains(webClip.url)) {
        clips[webClip.url] = null
        //
    } else {
        // Todo
    }
}

fun releaseClip(index: Int) {
    if (index < clips.size) {
        clips.removeAt(index)
        // Todo
    }
}

fun releaseAll(){
    clips.clear()
    holdWebViews.clear()
    FloatingWebClip.getInstance().hide()
}