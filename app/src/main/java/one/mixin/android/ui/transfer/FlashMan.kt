package one.mixin.android.ui.transfer

import android.app.Application
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import one.mixin.android.MixinApplication
import one.mixin.android.db.AppDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
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
import one.mixin.android.ui.transfer.vo.TransferMessage
import one.mixin.android.ui.transfer.vo.TransferMessageMention
import one.mixin.android.ui.transfer.vo.TransferSendData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.toMessage
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
import java.io.File
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
    private val serializationJson: Json
) {

    private val cachePath by lazy {
        File("${(context.externalCacheDir ?: context.cacheDir).absolutePath}${File.separator}$deviceId")
    }

    fun getAttachmentPath(): File {
        return File("${cachePath.absolutePath}${File.separator}attachment").also {
            it.mkdirs()
        }
    }

    private var index: Int = 0

    private fun getCacheFile(): File {
        cachePath.mkdirs()
        return File(cachePath, "${++index}.cache")
    }

    private var currentFile = getCacheFile()
    private var currentOutputStream = currentFile.outputStream()
    suspend fun writeBytes(bytes: ByteArray) {
        if (currentFile.length() + bytes.size > 5242880L) { // 5M
            processData(currentFile)
            currentFile = getCacheFile()
            currentOutputStream.close()
            currentOutputStream = currentFile.outputStream()
        } else {
            currentOutputStream.write(bytes)
        }
    }
    suspend fun finish(status: TransferStatusLiveData) {
        currentOutputStream.close()
        processData(currentFile)
        processFile(getAttachmentPath(), status)
    }

    private suspend fun processData(file: File)= withContext(SINGLE_TRANSFER_THREAD) {
        try {
            val messageList = mutableListOf<Message>()
            if (file.exists() && file.length() > 0) {
                file.inputStream().use { input ->
                    while (input.available() > 0) {
                        val sizeData = ByteArray(4)
                        input.read(sizeData)
                        val data = ByteArray(byteArrayToInt(sizeData))
                        input.read(data)
                        val content = String(data, StandardCharsets.UTF_8)
                        processJson(content, messageList)
                    }
                }
            }
            if (messageList.isNotEmpty()) {
                messageDao.insertList(messageList)
            }
            file.delete()
            Timber.e("delete ${file.absolutePath}")
        } catch (e: Exception) {
            Timber.e("skip ${file.absolutePath} ${e.message}")
        }
    }

    private fun processJson(content: String, messageList: MutableList<Message>) {
        val transferData = serializationJson.decodeFromString<TransferSendData<JsonElement>>(content)
        when (transferData.type) {
            TransferDataType.CONVERSATION.value -> {
                val conversation = serializationJson.decodeFromJsonElement<Conversation>(transferData.data)
                conversationDao.insertIgnore(conversation)
                Timber.e("Conversation ID: ${conversation.conversationId}")
            }

            TransferDataType.PARTICIPANT.value -> {
                val participant = serializationJson.decodeFromJsonElement<Participant>(transferData.data)
                participantDao.insertIgnore(participant)
                Timber.e("Participant ID: ${participant.conversationId} ${participant.userId}")
            }

            TransferDataType.USER.value -> {
                val user = serializationJson.decodeFromJsonElement<User>(transferData.data)
                userDao.insertIgnore(user)
                Timber.e("User ID: ${user.userId}")
            }

            TransferDataType.APP.value -> {
                Timber.e("$content ${transferData.data}")
                val app = serializationJson.decodeFromJsonElement<App>(transferData.data)
                appDao.insertIgnore(app)
                Timber.e("App ID: ${app.appId}")
            }

            TransferDataType.ASSET.value -> {
                val asset = serializationJson.decodeFromJsonElement<Asset>(transferData.data)
                assetDao.insertIgnore(asset)
                Timber.e("Asset ID: ${asset.assetId}")
            }

            TransferDataType.SNAPSHOT.value -> {
                val snapshot = serializationJson.decodeFromJsonElement<Snapshot>(transferData.data)
                snapshotDao.insertIgnore(snapshot)
                Timber.e("Snapshot ID: ${snapshot.snapshotId}")
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
                Timber.e("Sticker ID: ${sticker.stickerId}")
            }

            TransferDataType.PIN_MESSAGE.value -> {
                val pinMessage = serializationJson.decodeFromJsonElement<PinMessage>(transferData.data)
                pinMessageDao.insertIgnore(pinMessage)
                Timber.e("PinMessage ID: ${pinMessage.messageId}")
            }

            TransferDataType.TRANSCRIPT_MESSAGE.value -> {
                val transcriptMessage = serializationJson.decodeFromJsonElement<TranscriptMessage>(transferData.data)
                transcriptMessageDao.insertIgnore(transcriptMessage)
                Timber.e("Transcript ID: ${transcriptMessage.messageId}")
            }

            TransferDataType.MESSAGE.value -> {
                val message = serializationJson.decodeFromJsonElement<TransferMessage>(transferData.data)
                if (messageDao.findMessageIdById(message.messageId) == null) {
                    processMessage(message.toMessage(), messageList)
                }
                Timber.e("Message ID: ${message.messageId}")
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
                val rowId = messageMentionDao.insertIgnoreReturn(messageMention)
                Timber.e("MessageMention ID: $rowId ${messageMention.messageId}")
            }

            TransferDataType.EXPIRED_MESSAGE.value -> {
                val expiredMessage =
                    serializationJson.decodeFromJsonElement<ExpiredMessage>(transferData.data)
                expiredMessageDao.insertIgnore(expiredMessage)
                Timber.e("ExpiredMessage ID: ${expiredMessage.messageId}")
            }

            else -> {
                Timber.e("No support $content")
            }
        }
    }

    private fun processMessage(message: Message, list: MutableList<Message>) {
        list.add(message)
        ftsDatabase.insertOrReplaceMessageFts4(message)
        if (list.size == 100) {
            messageDao.insertList(list)
            list.clear()
        }
        Timber.e("Message ID: ${message.messageId}")
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
        // Final work
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
                        if (!dir.exists()){
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
                                    extensionName?.run { ".$extensionName" } ?: ".jpg"
                                )
                            } else if (message.isAudio()) {
                                context.getAudioPath().createAudioTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName?: "ogg"
                                )
                            } else if (message.isVideo()) {
                                context.getVideoPath().createVideoTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName?: "mp4"
                                )
                            } else {
                                context.getDocumentPath().createDocumentTemp(
                                    message.conversationId,
                                    message.messageId,
                                    extensionName ?: "",
                                )
                            }
                        f.moveTo(outFile)
                    }
                }
            }
        }
        folder.deleteRecursively()
        status.value = TransferStatus.FINISHED
    }
}
