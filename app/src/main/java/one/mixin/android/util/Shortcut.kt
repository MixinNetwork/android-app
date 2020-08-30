package one.mixin.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

fun addShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent) {
    ShortcutManagerCompat.createShortcutResultIntent(context, ShortcutInfoCompat.Builder(context, conversationId)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setLongLabel(name)
            .setIntent(launcher)
            .build()
    )
}
