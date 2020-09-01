package one.mixin.android.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

fun addShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent) {
    val shortcut = ShortcutInfoCompat.Builder(context, conversationId)
        .setShortLabel(name)
        .setIcon(IconCompat.createWithBitmap(icon))
        .setIntent(launcher)
        .build()
    val successCallback = PendingIntent.getBroadcast(context,0, ShortcutManagerCompat.createShortcutResultIntent(context, shortcut),0)
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, successCallback.intentSender)
}
