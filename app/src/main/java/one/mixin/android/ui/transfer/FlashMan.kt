package one.mixin.android.ui.transfer

import android.app.Application
import com.google.gson.Gson
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TransferSyncJob
import java.io.File
import java.util.UUID
import javax.inject.Inject

class FlashMan @Inject internal constructor(
    val gson: Gson,
    val context: Application,
    val assetDao: AssetDao,
    val conversationDao: ConversationDao,
    val expiredMessageDao: ExpiredMessageDao,
    val messageDao: MessageDao,
    val participantDao: ParticipantDao,
    val pinMessageDao: PinMessageDao,
    val snapshotDao: SnapshotDao,
    val stickerDao: StickerDao,
    val transcriptMessageDao: TranscriptMessageDao,
    val userDao: UserDao,
    val appDao: AppDao,
    val messageMentionDao: MessageMentionDao,
    val ftsDatabase: FtsDatabase,
    val jobManager: MixinJobManager,
) {
    private val currentId: UUID = UUID.randomUUID()

    private val cachePath by lazy {
        File("${(context.externalCacheDir ?: context.cacheDir).absolutePath}${File.separator}${currentId}")
    }


    private var index: Int = 0

    private fun getCacheFile(): File {
        cachePath.mkdirs()
        return File(cachePath, "${++index}.cache")
    }

    private var currentFile = getCacheFile()
    private var currentOutputStream = currentFile.outputStream()
    fun writeBytes(bytes: ByteArray) {
        if (currentFile.length() + bytes.size > 10485760L) { // 10M
            jobManager.addJob(TransferSyncJob(currentFile.absolutePath))
            currentFile = getCacheFile()
            currentOutputStream.close()
            currentOutputStream = currentFile.outputStream()
        } else {
            currentOutputStream.write(bytes)
        }
    }

    fun finish() {
        currentOutputStream.close()
        jobManager.addJob(TransferSyncJob(currentFile.absolutePath))
    }

}