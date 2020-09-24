package one.mixin.android.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import one.mixin.android.MixinApplication
import kotlin.math.min

private const val categoryTextShareTarget = "one.mixin.android.directshare.category.TEXT_SHARE_TARGET"
private const val categoryImageShareTarget = "one.mixin.android.directshare.category.IMAGE_SHARE_TARGET"
private const val categoryVideoShareTarget = "one.mixin.android.directshare.category.VIDEO_SHARE_TARGET"
private const val categoryAudioShareTarget = "one.mixin.android.directshare.category.AUDIO_SHARE_TARGET"
private const val categoryApplicationShareTarget = "one.mixin.android.directshare.category.APPLICATION_SHARE_TARGET"
private const val dynamicShortcutCount = 2

val shareCategories = setOf(
    categoryTextShareTarget, categoryImageShareTarget,
    categoryVideoShareTarget, categoryAudioShareTarget, categoryApplicationShareTarget
)

val maxDynamicShortcutCount by lazy {
    val systemMaxCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(MixinApplication.appContext)
    min(dynamicShortcutCount, systemMaxCount)
}

fun addPinShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent) {
    val shortcut = ShortcutInfoCompat.Builder(context, conversationId)
        .setShortLabel(name)
        .setIcon(IconCompat.createWithBitmap(icon))
        .setIntent(launcher)
        .build()
    val successCallback = PendingIntent.getBroadcast(context, 0, ShortcutManagerCompat.createShortcutResultIntent(context, shortcut), 0)
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
}

fun addDynamicShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent): Boolean {
    val shortcut = ShortcutInfoCompat.Builder(context, conversationId)
        .setShortLabel(name)
        .setIcon(IconCompat.createWithBitmap(icon))
        .setIntent(launcher)
        .setCategories(shareCategories)
        .build()
    return ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcut))
}
