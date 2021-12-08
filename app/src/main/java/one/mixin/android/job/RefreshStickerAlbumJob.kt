package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.insertUpdate
import one.mixin.android.event.AlbumEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

class RefreshStickerAlbumJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork()
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerAlbumJob"
        const val PREF_REFRESH_STICKER_ALBUM = "pref_refresh_sticker_album"
        const val PREF_NEW_ALBUM = "pref_new_album"
    }

    override fun onRun() = runBlocking {
        val response = accountService.getStickerAlbums()
        if (response.isSuccess && response.data != null) {
            val albums = response.data as List<StickerAlbum>

            val localLatestCreatedAt = stickerAlbumDao.findLatestCreatedAt()?.let { createdAt ->
                parseCreatedAt(createdAt)
            }
            var hasNewAlbum = false
            if (localLatestCreatedAt == null && albums.isNotEmpty()) {
                hasNewAlbum = true
            }

            for (a in albums) {
                val localAlbum = stickerAlbumDao.findAlbumById(a.albumId)
                if (localAlbum == null) {
                    a.added = !a.banner.isNullOrBlank()
                    a.orderedAt = Instant.now().toString()
                    stickerAlbumDao.insertSuspend(a)
                } else {
                    // old version users have all albums but lack the added field,
                    // we should keep these albums be showing by set added to true.
                    a.added = localAlbum.added ?: true
                    a.orderedAt = localAlbum.orderedAt
                    stickerAlbumDao.update(a)
                }

                if (!hasNewAlbum && localLatestCreatedAt != null) {
                    val remoteCreatedAt = parseCreatedAt(a.createdAt)
                    if (remoteCreatedAt != null) {
                        if (remoteCreatedAt.isAfter(localLatestCreatedAt)) {
                            hasNewAlbum = true
                        }
                    }
                }

                val r = accountService.getStickersByAlbumIdSuspend(a.albumId)
                if (r.isSuccess && r.data != null) {
                    val stickers = r.data as List<Sticker>
                    val relationships = arrayListOf<StickerRelationship>()
                    for (s in stickers) {
                        stickerDao.insertUpdate(s)
                        relationships.add(StickerRelationship(a.albumId, s.stickerId))
                    }
                    stickerRelationshipDao.insertList(relationships)
                }
            }

            if (hasNewAlbum) {
                MixinApplication.appContext.defaultSharedPreferences.putBoolean(PREF_NEW_ALBUM, true)
                RxBus.publish(AlbumEvent(true))
            }

            val sp = applicationContext.defaultSharedPreferences
            if (!sp.getBoolean("UpgradeMessageSticker", false)) {
                stickerRelationshipDao.updateMessageStickerId()
                sp.putBoolean("UpgradeMessageSticker", true)
            }
        }
    }

    private fun parseCreatedAt(createdAt: String): Instant? =
        try {
            ZonedDateTime.parse(createdAt).toOffsetDateTime()
        } catch (e: Exception) {
            null
        }?.toInstant()
}
