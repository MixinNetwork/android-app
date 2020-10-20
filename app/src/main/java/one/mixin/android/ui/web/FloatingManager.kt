package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.util.GsonHelper
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

var clips = mutableListOf<WebClip>()
var holdWebViews = mutableListOf<MixinWebView>()

data class WebClip(
    val url: String,
    @Expose(serialize = false, deserialize = false) val thumb: Bitmap?,
    val app: App?,
    val name: String?
)

fun holdClip(activity: Activity, webView: MixinWebView, webClip: WebClip) {
    if (!clips.contains(webClip)) {
        if (clips.size >= 6) {
            // Todo
        } else {
            clips.add(webClip)
            holdWebViews.add(webView)
            FloatingWebClip.getInstance().show(activity)
            activity.defaultSharedPreferences.putString(
                "floating", GsonHelper.customGson.toJson(
                    clips
                )
            )
        }
    } else {
        // Todo
    }
}

fun initClips(activity: Activity) {
    val content = activity.defaultSharedPreferences.getString("floating", null) ?: return
    val type = object : TypeToken<List<WebClip>>() {}.type
    val list = GsonHelper.customGson.fromJson<List<WebClip>>(content, type)
    clips.clear()
    clips.addAll(list)
    FloatingWebClip.getInstance().show(activity)
}

fun releaseClip(index: Int) {
    if (index < clips.size) {
        clips.removeAt(index)
        holdWebViews.removeAt(index)
    }
}

fun releaseAll() {
    clips.clear()
    holdWebViews.clear()
    FloatingWebClip.getInstance().hide()
}