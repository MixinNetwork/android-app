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
private const val dynamicShortcutCount = 4
private const val staticShortcutCount = 2 // wallet and scan

val shareCategories = setOf(
    categoryTextShareTarget, categoryImageShareTarget,
    categoryVideoShareTarget, categoryAudioShareTarget, categoryApplicationShareTarget
)

val maxDynamicShortcutCount by lazy {
    val systemMaxCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(MixinApplication.appContext)
    min(dynamicShortcutCount, systemMaxCount - staticShortcutCount)
}

fun addPinShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent) {
    val shortcut = ShortcutInfoCompat.Builder(context, conversationId)
        .setShortLabel(name)
        .setIcon(IconCompat.createWithBitmap(icon))
        .setIntent(launcher)
        .build()
    val successCallback = PendingIntent.getBroadcast(context, 0, ShortcutManagerCompat.createShortcutResultIntent(context, shortcut), PendingIntent.FLAG_IMMUTABLE)
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
}

fun generateDynamicShortcut(context: Context, shortcutInfo: ShortcutInfo): ShortcutInfoCompat {
    var shortcutName = shortcutInfo.name
    if (shortcutName.isEmpty()) {
        shortcutName = "Mixin-${shortcutInfo.conversationId}"
    }
    return ShortcutInfoCompat.Builder(context, shortcutInfo.conversationId)
        .setShortLabel(shortcutName)
        .setIntent(shortcutInfo.intent)
        .setIcon(IconCompat.createWithBitmap(shortcutInfo.icon))
        .setCategories(shareCategories)
        .build()
}

fun updateShortcuts(shortcuts: MutableList<ShortcutInfoCompat>) {
    val exists = ShortcutManagerCompat.getDynamicShortcuts(MixinApplication.appContext)
    val keepSize = maxDynamicShortcutCount - shortcuts.size
    if (keepSize >= 0 && exists.size > keepSize) {
        val removeIds = mutableListOf<String>()
        exists.take(exists.size - keepSize)
            .mapTo(removeIds) { it.id }
        ShortcutManagerCompat.removeDynamicShortcuts(MixinApplication.appContext, removeIds)
    }
    ShortcutManagerCompat.addDynamicShortcuts(MixinApplication.appContext, shortcuts)
}

data class ShortcutInfo(
    var conversationId: String,
    var name: String,
    var icon: Bitmap,
    var intent: Intent
)
