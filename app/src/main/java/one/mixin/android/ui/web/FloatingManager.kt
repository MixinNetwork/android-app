package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.collection.arrayMapOf
import one.mixin.android.vo.App

fun expand(context: Context) {
    WebActivity.show(context)
    FloatingWebClip.getInstance().hide()
}

fun collapse() {
    FloatingWebClip.getInstance().show()
}

var clips = arrayMapOf<String, WebClip>()

class WebClip(val url: String, val thumb: Bitmap, val app: App?, val name: String?)

fun holdClip(activity: Activity, webClip: WebClip) {
    if (!clips.contains(webClip.url)) {
        if (clips.size >= 6) {
            // Todo
        } else {
            clips[webClip.url] = webClip
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