package one.mixin.android.ui.transfer

import android.app.Application
import android.database.SQLException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import one.mixin.android.MixinApplication
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.PinMessageDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.UserDao
import one.mixin.android.extension.copy
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.moveTo
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.transfer.vo.TransferDataType
import one.mixin.android.ui.transfer.vo.TransferMessageMention
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.SINGLE_TRANSFER_FILE_THREAD
import one.mixin.android.util.SINGLE_TRANSFER_THREAD
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.Participant
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class FlashMan(
    val deviceId: String,
    val context: Application,
    val assetDao: AssetDao,
    val conversationDao: ConversationDao,
    val conversationExtDao: ConversationExtDao,
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
    private val serializationJson: Json,
) {

    companion object {
        private const val MAX_PROCESS_BYTES = 2097152L // 2M
        private const val MAX_FILE_SIZE = 10485760 // 10M
    }

    private val runtime by lazy {
        Runtime.getRuntime()
    }

    private val cachePath by lazy {
        File("${(context.externalCacheDir ?: context.cacheDir).absolutePath}${File.separator}$deviceId").apply {
            this.mkdirs()
        }
    }

    fun getAttachmentPath(): File {
        return File("${cachePath.absolutePath}${File.separator}attachment").also {
            it.mkdirs()
        }
    }

    private var index: Int = 0

    private fun getCacheFile(index: Int): File {
        cachePath.mkdirs()
        return File(cachePath, "$index.cache")
    }

    private var currentFile: File? = null
    private var currentOutputStream: OutputStream? = null
    private var lastName = ""
    suspend fun writeBytes(bytes: ByteArray) = withContext(SINGLE_TRANSFER_FILE_THREAD) {
        val file = currentFile ?: getCacheFile(++index).also {
            currentOutputStream?.close()
            currentFile = it
            currentOutputStream = it.outputStream()
        }
        if (file.length() + bytes.size > MAX_FILE_SIZE) {
            MixinApplication.get().applicationScope.launch(SINGLE_TRANSFER_THREAD) {
                processDataFile(file)
            }
            currentFile = null
            currentOutputStream?.close()
            currentOutputStream = null
        } else {
            if (lastName != file.name) {
                Timber.e("write bytes: ${file.name}")
                lastName = file.name
            }
            currentOutputStream?.write(bytes)
        }
    }

    suspend fun finish(status: TransferStatusLiveData) = MixinApplication.get().applicationScope.launch(SINGLE_TRANSFER_THREAD) {
        currentOutputStream?.close()
        currentFile?.let { file ->
            processDataFile(file)
            processFile(getAttachmentPath(), status)
        }
    }

    private suspend fun processDataFile(file: File) = withContext(SINGLE_TRANSFER_THREAD) {
        Timber.e("Process data file: ${file.name}")
        try {
            if (file.exists() && file.length() > 0) {
                file.inputStream().use { input ->
                    while (input.available() > 0) {
                        val sizeData = ByteArray(4)
                        input.read(sizeData)
                        val data = ByteArray(byteArrayToInt(sizeData))
                        input.read(data)
                        processJson(data)
                    }
                }
            }
            if (messageList.isNotEmpty()) {
                insertMessages(messageList)
            }
            file.delete()
            Timber.e("delete ${file.absolutePath}")
        } catch (e: Exception) {
            Timber.e("skip ${file.absolutePath} ${e.message}")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun processJson(data: ByteArray) {
        try {
            if (runtime.freeMemory() < 5242880) {
                runtime.gc()
            }
            val transferData = serializationJson.decodeFromStream<TransferSendData<JsonElement>>(ByteArrayInputStream(data))
            when (transferData.type) {
                TransferDataType.CONVERSATION.value -> {
                    val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                    conversationDao.insertIgnore(conversation)
                }

                TransferDataType.PARTICIPANT.value -> {
                    val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                    participantDao.insertIgnore(participant)
                }

                TransferDataType.USER.value -> {
                    val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                    userDao.insertIgnore(user)
                }

                TransferDataType.APP.value -> {
                    val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                    appDao.insertIgnore(app)
                }

                TransferDataType.ASSET.value -> {
                    val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                    assetDao.insertIgnore(asset)
                }

                TransferDataType.SNAPSHOT.value -> {
                    val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                    snapshotDao.insertIgnore(snapshot)
                }

                TransferDataType.STICKER.value -> {
                    val sticker = serializationJson.decodeFromJsonElement<Sticker>(transferData.data)
                    sticker.lastUseAt?.let {
                        try {
                            sticker.lastUseAt = it.createAtToLong().toString()
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    stickerDao.insertIgnore(sticker)
                }

                TransferDataType.PIN_MESSAGE.value -> {
                    val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                    pinMessageDao.insertIgnore(pinMessage)
                }

                TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                    val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                    transcriptMessageDao.insertIgnore(transcriptMessage)
                }

                TransferDataType.MESSAGE.value -> {
                    val messageId = transferData.data.jsonObject["message_id"]?.jsonPrimitive?.contentOrNull ?: return
                    if (messageDao.findMessageIdById(messageId) == null) {
                        val message = serializationJson.decodeFromJsonElement<Message>(transferData.data)
                        processMessage(message)
                    }
                }

                TransferDataType.MESSAGE_MENTION.value -> {
                    val messageMention =
                        serializationJson.decodeFromJsonElement<TransferMessageMention>(transferData.data).let {
                            val messageContent =
                                messageDao.findMessageContentById(it.conversationId, it.messageId)
                                    ?: return
                            val mentionData = parseMentionData(messageContent, userDao) ?: return
                            MessageMention(it.messageId, it.conversationId, mentionData, it.hasRead)
                        }
                    messageMentionDao.insertIgnoreReturn(messageMention)
                }

                TransferDataType.EXPIRED_MESSAGE.value -> {
                    val expiredMessage =
                        serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                    expiredMessageDao.insertIgnore(expiredMessage)
                }

                else -> {
                    Timber.e("No support ${transferData.type}")
                }
            }
        } catch (e: Exception) {
            Timber.e("${e.message} ${StandardCharsets.UTF_8}")
        }
    }

    private val messageList = mutableListOf<Message>()

    private fun processMessage(message: Message) {
        messageList.add(message)
        ftsDatabase.insertOrReplaceMessageFts4(message)
        if (messageList.size == 100) {
            insertMessages(messageList)
            messageList.clear()
        }
    }

    private fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        for (i in byteArray.indices) {
            result = result shl 8
            result = result or (byteArray[i].toInt() and 0xff)
        }
        return result
    }

    private suspend fun processFile(folder: File, status: TransferStatusLiveData) = withContext(SINGLE_TRANSFER_THREAD) {
        // Final db work
        conversationDao.getAllConversationId().forEach { conversationId ->
            conversationDao.refreshLastMessageId(conversationId)
        }
        conversationExtDao.getAllConversationId().forEach { conversationId ->
            conversationExtDao.refreshCountByConversationId(conversationId)
        }
        val context = MixinApplication.appContext
        folder.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0) {
                val messageId = f.name
                if (messageId.isUUID()) {
                    val transferMessage = transcriptMessageDao.findAttachmentMessage(messageId)
                    if (transferMessage?.mediaUrl != null) {
                        val dir = context.getTranscriptDirPath()
                        if (!dir.exists()) {
                            dir.mkdirs()
                        }
                        f.copy(File(dir, transferMessage.mediaUrl!!))
                    }
                    val message = messageDao.findAttachmentMessage(messageId)
                    if (message?.mediaUrl != null) {
                        val extensionName = message.mediaUrl.getExtensionName()
                        val outFile =
                            if (message.isImage()) {
                                context.getImagePath().createImageTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName?.run { ".$extensionName" } ?: ".jpg",
                                )
                            } else if (message.isAudio()) {
                                context.getAudioPath().createAudioTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "ogg",
                                )
                            } else if (message.isVideo()) {
                                context.getVideoPath().createVideoTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "mp4",
                                )
                            } else {
                                context.getDocumentPath().createDocumentTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "",
                                )
                            }
                        f.moveTo(outFile)
                    } else {
                        // Unable to Mapping to data, delete file
                        f.delete()
                    }
                }
            }
        }
        folder.deleteRecursively()
        status.value = TransferStatus.FINISHED
    }

    private fun insertMessages(messages: List<Message>) {
        val writableDatabase = MixinDatabase.getWritableDatabase() ?: return

        val sql =
            "INSERT OR IGNORE INTO messages (id,conversation_id,user_id,category,content,media_url,media_mime_type,media_size,media_duration,media_width,media_height,media_hash,thumb_image,thumb_url,media_key,media_digest,media_status,status,created_at,action,participant_id,snapshot_id,hyperlink,name,album_id,sticker_id,shared_user_id,media_waveform,media_mine_type,quote_message_id,quote_content,caption) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

        val statement = writableDatabase.compileStatement(sql)

        writableDatabase.beginTransaction()
        try {
            for (message in messages) {
                statement.bindString(1, message.messageId)
                statement.bindString(2, message.conversationId)
                statement.bindString(3, message.userId)
                statement.bindString(4, message.category)

                val content = message.content
                if (content != null) {
                    statement.bindString(5, content)
                } else {
                    statement.bindNull(5)
                }

                val mediaUrl = message.mediaUrl
                if (mediaUrl != null) {
                    statement.bindString(6, mediaUrl)
                } else {
                    statement.bindNull(6)
                }

                val mediaMimeType = message.mediaMimeType
                if (mediaMimeType != null) {
                    statement.bindString(7, mediaMimeType)
                } else {
                    statement.bindNull(7)
                }

                val mediaSize = message.mediaSize
                if (mediaSize != null) {
                    statement.bindLong(8, mediaSize)
                } else {
                    statement.bindNull(8)
                }

                val mediaDuration = message.mediaDuration
                if (mediaDuration != null) {
                    statement.bindString(9, mediaDuration)
                } else {
                    statement.bindNull(9)
                }

                val mediaWidth = message.mediaWidth
                if (mediaWidth != null) {
                    statement.bindLong(10, mediaWidth.toLong())
                } else {
                    statement.bindNull(10)
                }

                val mediaHeight = message.mediaHeight
                if (mediaHeight != null) {
                    statement.bindLong(10, mediaHeight.toLong())
                } else {
                    statement.bindNull(10)
                }

                val mediaHash = message.mediaHash
                if (mediaHash != null) {
                    statement.bindString(12, mediaHash)
                } else {
                    statement.bindNull(12)
                }

                val thumbImage = message.thumbImage
                if (thumbImage != null) {
                    statement.bindString(13, thumbImage)
                } else {
                    statement.bindNull(13)
                }

                val thumbUrl = message.thumbUrl
                if (thumbUrl != null) {
                    statement.bindString(14, thumbUrl)
                } else {
                    statement.bindNull(14)
                }

                val mediaKey = message.mediaKey
                if (mediaKey != null) {
                    statement.bindBlob(15, mediaKey)
                } else {
                    statement.bindNull(15)
                }

                val mediaDigest = message.mediaDigest
                if (mediaDigest != null) {
                    statement.bindBlob(16, mediaDigest)
                } else {
                    statement.bindNull(16)
                }

                val mediaStatus = message.mediaStatus
                if (mediaStatus == null) {
                    statement.bindNull(17)
                } else {
                    statement.bindString(17, mediaStatus)
                }
                statement.bindString(18, message.status)
                statement.bindString(19, message.createdAt)
                val action = message.action
                if (action == null) {
                    statement.bindNull(20)
                } else {
                    statement.bindString(20, action)
                }

                val participantId = message.participantId
                if (participantId != null) {
                    statement.bindString(21, participantId)
                } else {
                    statement.bindNull(21)
                }

                val snapshotId = message.snapshotId
                if (snapshotId != null) {
                    statement.bindString(22, snapshotId)
                } else {
                    statement.bindNull(22)
                }

                val hyperlink = message.hyperlink
                if (hyperlink != null) {
                    statement.bindString(23, hyperlink)
                } else {
                    statement.bindNull(23)
                }

                val name = message.name
                if (name != null) {
                    statement.bindString(24, name)
                } else {
                    statement.bindNull(24)
                }

                val albumId = message.albumId
                if (albumId != null) {
                    statement.bindString(25, albumId)
                } else {
                    statement.bindNull(25)
                }

                val stickerId = message.stickerId
                if (stickerId != null) {
                    statement.bindString(26, stickerId)
                } else {
                    statement.bindNull(26)
                }

                val sharedUserId = message.sharedUserId
                if (sharedUserId != null) {
                    statement.bindString(27, sharedUserId)
                } else {
                    statement.bindNull(27)
                }

                val mediaWaveform = message.mediaWaveform
                if (mediaWaveform != null) {
                    statement.bindBlob(28, mediaWaveform)
                } else {
                    statement.bindNull(28)
                }

                statement.bindNull(29)

                val quoteMessageId = message.quoteMessageId
                if (quoteMessageId != null) {
                    statement.bindString(30, quoteMessageId)
                } else {
                    statement.bindNull(30)
                }

                val quoteContent = message.quoteContent
                if (quoteContent != null) {
                    statement.bindString(31, quoteContent)
                } else {
                    statement.bindNull(31)
                }

                val caption = message.caption
                if (caption != null) {
                    statement.bindString(32, caption)
                } else {
                    statement.bindNull(32)
                }

                statement.executeInsert()
            }
            writableDatabase.setTransactionSuccessful()
        } catch (e: SQLException) {
            Timber.e(e)
        } finally {
            writableDatabase.endTransaction()
            statement.close()
        }
    }
}
