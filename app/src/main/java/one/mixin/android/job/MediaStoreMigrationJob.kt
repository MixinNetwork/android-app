package one.mixin.android.job

import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.MediaStore.PREF_MEDIA_STORE
import one.mixin.android.Constants.Account.MediaStore.PREF_MEDIA_STORE_LAST
import one.mixin.android.Constants.Account.MediaStore.PREF_MEDIA_STORE_OFFSET
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getLegacyMediaPath
import one.mixin.android.extension.getOldMediaPath
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.util.copyInputStreamToUri
import one.mixin.android.util.getAttachmentAudioUri
import one.mixin.android.util.getAttachmentFilesUri
import one.mixin.android.util.getAttachmentImagesUri
import one.mixin.android.util.getAttachmentVideoUri
import one.mixin.android.util.hasRWMediaStorePermission
import one.mixin.android.vo.MessageCategory
import timber.log.Timber

class MediaStoreMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "media_store_migration"
        private const val EACH = 10
    }

    override fun onRun() {
        val ctx = MixinApplication.appContext
        val startTime = System.currentTimeMillis()
        val preferences = ctx.defaultSharedPreferences
        var migrationLast = preferences.getLong(PREF_MEDIA_STORE_LAST, -1)
        val offset = preferences.getLong(PREF_MEDIA_STORE_OFFSET, 0)
        if (migrationLast == -1L) {
            migrationLast = messageDao.getLastMessageRowid()
            preferences.putLong(PREF_MEDIA_STORE_LAST, migrationLast)
        }
        if (!hasRWMediaStorePermission()) return
        val list = messageDao.findAttachmentMigration(migrationLast, EACH, offset)
        list.forEach { attachment ->
            val attachUri = attachment.mediaUrl?.toUri() ?: return@forEach
            val inputStream = ctx.contentResolver.openInputStream(attachUri) ?: return@forEach
            val toUri = when (attachment.category) {
                MessageCategory.PLAIN_IMAGE.name, MessageCategory.SIGNAL_IMAGE.name -> {
                    getAttachmentImagesUri(attachment.mediaMimeType, attachment.conversationId, attachment.messageId, attachment.name)
                }
                MessageCategory.PLAIN_DATA.name, MessageCategory.SIGNAL_DATA.name -> {
                    getAttachmentFilesUri(attachment.conversationId, attachment.messageId, attachment.name)
                }
                MessageCategory.PLAIN_VIDEO.name, MessageCategory.SIGNAL_VIDEO.name -> {
                    getAttachmentVideoUri(attachment.conversationId, attachment.messageId, attachment.name)
                }
                MessageCategory.PLAIN_AUDIO.name, MessageCategory.SIGNAL_AUDIO.name -> {
                    getAttachmentAudioUri(attachment.conversationId, attachment.messageId, attachment.name)
                }
                else -> null
            } ?: return@forEach

            copyInputStreamToUri(toUri, inputStream)

            messageDao.updateMediaMessageUrl(toUri.toString(), attachment.messageId)
        }
        preferences.putLong(PREF_MEDIA_STORE_OFFSET, offset + list.size)
        Timber.d("MediaStore migration handle ${offset + list.size} file cost: ${System.currentTimeMillis() - startTime} ms")
        if (list.size < EACH) {
            val hadAttachmentMigration = preferences.getBoolean(Constants.Account.PREF_ATTACHMENT, false)
            if (hadAttachmentMigration) {
                ctx.getLegacyMediaPath()
            } else {
                ctx.getOldMediaPath()
            }?.deleteRecursively()
            preferences.putBoolean(PREF_MEDIA_STORE, true)
        } else {
            jobManager.addJobInBackground(MediaStoreMigrationJob())
        }
    }
}
