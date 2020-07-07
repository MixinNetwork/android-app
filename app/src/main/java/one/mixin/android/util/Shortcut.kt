package one.mixin.android.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService

fun addShortcut(context: Context, conversationId: String, name: String, icon: Bitmap, launcher: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val scm: ShortcutManager? = context.getSystemService()
        val si = ShortcutInfo.Builder(context, conversationId)
            .setIcon(Icon.createWithBitmap(icon))
            .setShortLabel(name)
            .setIntent(launcher)
            .build()
        scm?.requestPinShortcut(si, null)
    } else {
        val addShortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        addShortcutIntent.putExtra("duplicate", false)
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon)
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcher)
        context.sendBroadcast(addShortcutIntent)
    }
}
