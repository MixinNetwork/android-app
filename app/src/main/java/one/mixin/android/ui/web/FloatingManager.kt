package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.gson.reflect.TypeToken
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.App
import one.mixin.android.widget.MixinWebView

var screenshot: Bitmap? = null
fun expand(context: Context) {
    MixinApplication.get().currentActivity?.let { activity ->
        val rootView: View = activity.window.decorView.findViewById(android.R.id.content)
        rootView.isDrawingCacheEnabled = true
        val screenBitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false
        val resultBitmap = Bitmap.createBitmap(screenBitmap.width, screenBitmap.height, Bitmap.Config.ARGB_8888)
        val cv = Canvas(resultBitmap)
        cv.drawBitmap(screenBitmap, 0f, 0f, Paint())
        cv.drawRect(
            0f,
            0f,
            screenBitmap.width.toFloat(),
            screenBitmap.height.toFloat(),
            Paint().apply {
                color = Color.parseColor("#CC000000")
            })
        screenshot = resultBitmap
    }
    WebActivity.show(context)
    FloatingWebClip.getInstance().hide()
}

fun collapse() {
    if (clips.size > 0) {
        FloatingWebClip.getInstance().show()
    }
}

var clips = mutableListOf<WebClip>()

data class WebClip(
    val url: String,
    val thumb: Bitmap?,
    val app: App?,
    val titleColor: Int,
    val name: String?,
    @Transient val webView: MixinWebView?
)

fun updateClip(activity: Activity, index: Int, webClip: WebClip) {
    clips.removeAt(index)
    clips.add(index, webClip)
    saveClips(activity)
}

fun holdClip(activity: Activity, webClip: WebClip) {
    if (!clips.contains(webClip)) {
        if (clips.size >= 6) {
            activity.toast(R.string.web_full)
        } else {
            clips.add(webClip)
            FloatingWebClip.getInstance().show(activity)
            saveClips(activity)
        }
    }
}

private fun initClips(activity: Activity) {
    val content = activity.defaultSharedPreferences.getString("floating", null) ?: return
    val type = object : TypeToken<List<WebClip>>() {}.type
    val list = GsonHelper.customGson.fromJson<List<WebClip>>(content, type)
    clips.clear()
    if (list.isEmpty()) return
    clips.addAll(list)
    FloatingWebClip.getInstance().show(activity)
}

fun refresh(activity: Activity) {
    if (clips.isEmpty()) {
        initClips(activity)
    } else {
        FloatingWebClip.getInstance().show(activity, false)
    }
}

fun releaseClip(index: Int) {
    if (index < clips.size && index >= 0) {
        clips.removeAt(index)
        if (clips.isEmpty()) {
            FloatingWebClip.getInstance().hide()
        }
        saveClips(MixinApplication.appContext)
    }
}

private fun saveClips(context: Context) {
    context.defaultSharedPreferences.putString("floating", GsonHelper.customGson.toJson(clips))
}

fun releaseAll() {
    clips.clear()
    saveClips(MixinApplication.appContext)
    FloatingWebClip.getInstance().hide()
}
