package one.mixin.android.job

import android.os.Build
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT_LAST
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT_OFFSET
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT
import one.mixin.android.MixinApplication
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.createEmptyTemp
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.createWebpTemp
import one.mixin.android.extension.getAncientMediaPath
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.reportException
import one.mixin.android.vo.Property
import one.mixin.android.vo.getFile
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.IOException
import java.nio.file.Files

class AttachmentMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "attachment_migration"
        private const val EACH = 10
    }

    override fun onRun() =
        runBlocking {
            val startTime = System.currentTimeMillis()
            var migrationLast = propertyDao().findValueByKey(PREF_MIGRATION_ATTACHMENT_LAST)?.toLongOrNull() ?: -1

            if (migrationLast == -1L) {
                migrationLast = messageDao().getLastMessageRowid()
                propertyDao().insertSuspend(Property(PREF_MIGRATION_ATTACHMENT_LAST, migrationLast.toString(), nowInUtc()))
            }

            if (!hasWritePermission()) return@runBlocking
            val offset = propertyDao().findValueByKey(PREF_MIGRATION_ATTACHMENT_OFFSET)?.toLongOrNull() ?: 0
            val list = messageDao().findAttachmentMigration(migrationLast, EACH, offset)
            list.forEach { attachment ->
                val fromFile = attachment.getFile(MixinApplication.appContext) ?: return@forEach
                if (!fromFile.exists()) {
                    Timber.d("Attachment migration no exists ${fromFile.absoluteFile}")
                    return@forEach
                }
                val toFile =
                    when {
                        attachment.isImage() -> {
                            when {
                                attachment.mediaMimeType?.isImageSupport() == false -> {
                                    MixinApplication.get().getImagePath()
                                        .createEmptyTemp(attachment.conversationId, attachment.messageId)
                                }
                                attachment.mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
                                    MixinApplication.get().getImagePath().createImageTemp(
                                        attachment.conversationId,
                                        attachment.messageId,
                                        ".png",
                                    )
                                }
                                attachment.mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
                                    MixinApplication.get().getImagePath()
                                        .createGifTemp(attachment.conversationId, attachment.messageId)
                                }
                                attachment.mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
                                    MixinApplication.get().getImagePath()
                                        .createWebpTemp(attachment.conversationId, attachment.messageId)
                                }
                                else -> {
                                    MixinApplication.get().getImagePath().createImageTemp(
                                        attachment.conversationId,
                                        attachment.messageId,
                                        ".jpg",
                                    )
                                }
                            }
                        }
                        attachment.isData() -> {
                            val extensionName = attachment.name?.getExtensionName()
                            MixinApplication.get().getDocumentPath()
                                .createDocumentTemp(
                                    attachment.conversationId,
                                    attachment.messageId,
                                    extensionName,
                                )
                        }
                        attachment.isVideo() -> {
                            val extensionName =
                                attachment.name?.getExtensionName().let {
                                    it ?: "mp4"
                                }
                            MixinApplication.get().getVideoPath()
                                .createVideoTemp(
                                    attachment.conversationId,
                                    attachment.messageId,
                                    extensionName,
                                )
                        }
                        attachment.isAudio() -> {
                            MixinApplication.get().getAudioPath()
                                .createAudioTemp(attachment.conversationId, attachment.messageId, "ogg")
                        }
                        else -> null
                    }
                toFile ?: return@forEach
                try {
                    if (fromFile.absolutePath != toFile.absolutePath) {
                        if (toFile.parentFile?.exists() != true) {
                            toFile.parentFile?.mkdirs()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Files.move(fromFile.toPath(), toFile.toPath())
                        } else {
                            fromFile.renameTo(toFile)
                        }
                    }
                } catch (e: IOException) {
                    Timber.e("Attachment migration ${e.message}")
                    reportException(e)
                }
                Timber.d("Attachment migration ${fromFile.absolutePath} ${toFile.absolutePath}")
                if (attachment.mediaUrl != toFile.name) {
                    messageDao().updateMediaMessageUrl(toFile.name, attachment.messageId)
                    MessageFlow.update(attachment.conversationId, attachment.messageId)
                }
            }
            propertyDao().insertSuspend(Property(PREF_MIGRATION_ATTACHMENT_OFFSET, (offset + list.size).toString(), nowInUtc()))
            Timber.d("Attachment migration handle ${offset + list.size} file cost: ${System.currentTimeMillis() - startTime} ms")
            if (list.size < EACH) {
                Timber.d("Attachment start delete ancient media path")
                MixinApplication.appContext.getAncientMediaPath()?.deleteRecursively()
                Timber.d("Attachment delete ancient media path completed!!!")
                if (propertyDao().findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT)?.toBoolean() != true) {
                    Timber.d("Attachment start delete media path")
                    MixinApplication.appContext.getMediaPath(true)?.deleteRecursively()
                    Timber.d("Attachment delete media path completed!!!")
                }
                propertyDao().updateValueByKey(PREF_MIGRATION_ATTACHMENT, false.toString())
                Timber.d("Attachment migration completed!!!")
            } else {
                jobManager.addJobInBackground(AttachmentMigrationJob())
            }
        }
}
