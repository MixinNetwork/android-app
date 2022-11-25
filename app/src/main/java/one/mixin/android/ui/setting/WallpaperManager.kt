package one.mixin.android.ui.setting

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getWallpaperFile
import one.mixin.android.extension.putInt

object WallpaperManager {

    const val INDEX_KEY = "WALLPAPER_INDEX"

    fun getWallpaper(context: Context): Drawable? {
        return getWallpaper(context, getIndex(context))
    }

    fun getWallpaperByPosition(context: Context, position: Int): Drawable? {
        val index = if (wallpaperExists(context)) {
            position - 2
        } else {
            position - 1
        }
        return getWallpaper(context, index)
    }

    private fun getWallpaper(context: Context, index: Int): Drawable? {
        val isNight = context.booleanFromAttribute(R.attr.flag_night)
        return when (index) {
            -1 -> context.getWallpaperFile().run {
                if (exists()) {
                    Drawable.createFromPath(context.getWallpaperFile().path)
                } else {
                    null
                }
            }
            0 -> ContextCompat.getDrawable(
                context,
                if (isNight) R.drawable.bg_chat_symbol_night else R.drawable.bg_chat_symbol
            )
            1 -> ContextCompat.getDrawable(
                context,
                if (isNight) R.drawable.bg_chat_star_night else R.drawable.bg_chat_star
            )
            2 -> ContextCompat.getDrawable(
                context,
                if (isNight) R.drawable.bg_chat_animal_night else R.drawable.bg_chat_animal
            )
            3 -> ContextCompat.getDrawable(
                context,
                if (isNight) R.drawable.bg_chat_plant_night else R.drawable.bg_chat_plant
            )
            else -> null
        }
    }

    fun save(context: Context, position: Int) {
        if (position == 0) {
            return
        }
        val wallpaperExists = wallpaperExists(context)
        if (wallpaperExists && position == 1) {
            context.defaultSharedPreferences.putInt(INDEX_KEY, -1)
        } else {
            context.defaultSharedPreferences.putInt(
                INDEX_KEY,
                position - if (wallpaperExists) 2 else 1
            )
            clear(context)
        }
    }

    private fun getIndex(context: Context) =
        context.defaultSharedPreferences.getInt(INDEX_KEY, 0)

    fun getCurrentSelected(context: Context) =
        getIndex(context) + if (wallpaperExists(context)) 2 else 1

    fun getWallpaperCount(context: Context) = 4 + if (wallpaperExists(context)) 2 else 1

    fun wallpaperExists(context: Context) = context.getWallpaperFile().exists()

    fun wallpaperFile(context: Context) = context.getWallpaperFile()

    private fun clear(context: Context) {
        context.getWallpaperFile().apply {
            if (exists()) {
                delete()
            }
        }
    }
}
