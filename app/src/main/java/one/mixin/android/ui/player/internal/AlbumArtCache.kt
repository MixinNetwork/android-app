package one.mixin.android.ui.player.internal

import android.graphics.BitmapFactory
import one.mixin.android.MixinApplication
import one.mixin.android.extension.toBytes
import timber.log.Timber
import java.io.File

object AlbumArtCache {
    const val DEFAULT_ALBUM_ART = "android.resource://one.mixin.messenger/drawable/ic_music_place_holder"

    fun getAlbumArtUri(id: String, url: String?, artData: ByteArray?): String {
        val cachedFile = getCachedFile(id)
        return if (cachedFile != null) {
            Timber.d("Cache hit for $url at ${cachedFile.absolutePath}")
            cachedFile.absolutePath
        } else {
            if (artData != null) {
                val file = File(parentDir(), filenameForId(id))
                if (file.exists()) {
                    file.delete()
                }
                val bitmap = BitmapFactory.decodeByteArray(artData, 0, artData.size)
                file.writeBytes(bitmap.toBytes())
                file.absolutePath
            } else {
                DEFAULT_ALBUM_ART
            }
        }
    }

    private fun getCachedFile(id: String): File? {
        val file = File(parentDir(), filenameForId(id))
        return if (file.exists()) {
            file
        } else null
    }

    private fun parentDir(): File {
        val file = File(MixinApplication.appContext.cacheDir, "album_art_cache")
        if (file.isFile) {
            file.delete()
        }
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    private fun filenameForId(messageId: String) =
        "album_art_cache_" + messageId.replace("\\W+".toRegex(), "") + ".jpg"
}
